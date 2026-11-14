import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { allBlocks, makeTemplate } from '@/lib/sistema/core';
import { prepararCaja } from '@/lib/caja';
import { estado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion } from '@/test/testHelpers';
import type { ConsultaCaja, SolicitudCierreCaja, SolicitudCobro, VentaCaja } from '@/types/caja';

const venta: VentaCaja = {
  id: crypto.randomUUID(),
  lineas: [
    {
      productoId: 'pan',
      nombre: 'Pan francés',
      cantidad: 0.5,
      precio: 4000,
      importe: 2000,
      modo: 'PESO',
    },
  ],
  medio: 'MIXTO',
  pagos: [
    { medio: 'EFECTIVO', importe: 1000 },
    { medio: 'TRANSFERENCIA', importe: 1000 },
  ],
  total: 2000,
  recibido: 2500,
  vuelto: 500,
  creadoEn: '2026-11-12T23:00:00Z',
};
const resumen = {
  ventas: [venta],
  total: 2000,
  efectivo: 1000,
  transferencia: 1000,
  tarjeta: 0,
  desde: venta.creadoEn,
  revision: 'revision-1',
};
const vacio = {
  ventas: [],
  total: 0,
  efectivo: 0,
  transferencia: 0,
  tarjeta: 0,
  desde: null,
  revision: 'vacia',
};
const cierre = {
  id: crypto.randomUUID(),
  resumen,
  fondoInicial: 0,
  entradas: 0,
  retiros: 0,
  esperado: 1000,
  contado: null,
  diferencia: null,
  nota: '',
  cerradoEn: '2026-11-13T01:00:00Z',
};

async function abrir() {
  const usuario = setupUsuarioConSesion();
  const sistema = makeTemplate('shop', {}, false),
    inventario = allBlocks(sistema).find((b) => b.type === 'inventory');
  if (!inventario) throw new Error('Falta inventario');
  inventario.rows = [
    { id: crypto.randomUUID(), name: 'Coca-Cola', quantity: 10, price: 2700, favorite: true },
  ];
  prepararCaja(sistema, inventario.id);
  estado.sistemas.set(usuario.email, [sistema]);
  renderApp('/caja');
  await screen.findByRole('heading', { name: 'Caja', level: 1 }, ESPERA);
  const user = userEvent.setup();
  await user.click(screen.getByRole('tab', { name: 'Favoritos' }));
  return user;
}

describe('cierres y pagos mixtos de caja', () => {
  it('divide una venta y calcula vuelto solamente sobre la parte en efectivo', async () => {
    let solicitud: SolicitudCobro | undefined;
    server.use(
      http.post('/api/caja/ventas', async ({ request }) => {
        solicitud = (await request.json()) as SolicitudCobro;
        return HttpResponse.json({
          ...venta,
          id: solicitud.id,
          total: 2700,
          pagos: solicitud.pagos,
          vuelto: 500,
        });
      }),
    );
    const user = await abrir();
    await user.click(screen.getByRole('button', { name: /Coca-Cola.*2.700/ }));
    await user.click(screen.getByRole('button', { name: /Cobrar.*2.700/ }));
    await user.click(screen.getByRole('button', { name: 'Dividir pago' }));
    await user.type(screen.getByLabelText('Importe en efectivo ($)'), '1000');
    await user.type(screen.getByLabelText('Efectivo recibido ($)'), '1500');
    expect(screen.getByText(/Transferencia:.*1.700/)).toBeVisible();
    expect(screen.getByText(/Vuelto:.*500/)).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Confirmar cobro' }));
    await screen.findByRole('heading', { name: 'Venta registrada' }, ESPERA);
    expect(solicitud).toMatchObject({
      medio: 'MIXTO',
      recibido: 1500,
      pagos: [
        { medio: 'EFECTIVO', importe: 1000 },
        { medio: 'TRANSFERENCIA', importe: 1700 },
      ],
    });
  });

  it('un importe mixto que supera el total deshabilita la confirmación', async () => {
    const user = await abrir();
    await user.click(screen.getByRole('button', { name: /Coca-Cola.*2.700/ }));
    await user.click(screen.getByRole('button', { name: /Cobrar.*2.700/ }));
    await user.click(screen.getByRole('button', { name: 'Dividir pago' }));
    await user.type(screen.getByLabelText('Importe en efectivo ($)'), '3000');
    expect(screen.getByRole('button', { name: 'Confirmar cobro' })).toBeDisabled();
  });

  it('conserva el período abierto de ayer y guarda un cierre recuperable desde el historial', async () => {
    let datos: ConsultaCaja = {
      fecha: '2026-11-13',
      dia: vacio,
      abierta: resumen,
      cierres: [],
      pagina: 0,
      hayMas: false,
    };
    let request: SolicitudCierreCaja | undefined;
    server.use(
      http.get('/api/caja/resumen', () => HttpResponse.json(datos)),
      http.post('/api/caja/cierres', async ({ request: peticion }) => {
        request = (await peticion.json()) as SolicitudCierreCaja;
        datos = {
          ...datos,
          abierta: vacio,
          cierres: [
            {
              id: cierre.id,
              desde: venta.creadoEn,
              cerradoEn: cierre.cerradoEn,
              total: 2000,
              cantidadVentas: 1,
              diferencia: null,
            },
          ],
        };
        return HttpResponse.json(cierre);
      }),
      http.get('/api/caja/cierres/:id', () => HttpResponse.json(cierre)),
    );
    const user = await abrir();
    await user.click(screen.getByRole('button', { name: 'Actividad' }));
    await user.click(screen.getByRole('button', { name: 'Caja abierta' }));
    expect(await screen.findByText(/Abierta desde 12\/11\/26/)).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Cerrar caja' }));
    await user.click(screen.getByRole('button', { name: 'Guardar cierre' }));
    await screen.findByRole('heading', { name: 'Cierre guardado' }, ESPERA);
    expect(request).toMatchObject({
      revision: 'revision-1',
      fondoInicial: 0,
      entradas: 0,
      retiros: 0,
      contado: null,
    });
    await user.click(screen.getByRole('button', { name: 'Listo' }));
    await user.click(screen.getByRole('button', { name: 'Cierres guardados' }));
    await user.click(await screen.findByRole('button', { name: /1 ventas.*Desde/ }, ESPERA));
    expect(await screen.findByRole('heading', { name: 'Cierre guardado' })).toBeVisible();
    expect(within(screen.getByRole('dialog')).getByText('Sin recuento')).toBeVisible();
  });

  it('un cierre incierto se vuelve a consultar con el mismo identificador', async () => {
    const solicitudes: SolicitudCierreCaja[] = [];
    server.use(
      http.get('/api/caja/resumen', () =>
        HttpResponse.json({
          fecha: '2026-11-13',
          dia: resumen,
          abierta: resumen,
          cierres: [],
          pagina: 0,
          hayMas: false,
        }),
      ),
      http.post('/api/caja/cierres', async ({ request }) => {
        solicitudes.push((await request.json()) as SolicitudCierreCaja);
        return solicitudes.length === 1 ? HttpResponse.error() : HttpResponse.json(cierre);
      }),
    );
    const user = await abrir();
    await user.click(screen.getByRole('button', { name: 'Actividad' }));
    await user.click(screen.getByRole('button', { name: 'Caja abierta' }));
    await user.click(await screen.findByRole('button', { name: 'Cerrar caja' }));
    await user.click(screen.getByRole('button', { name: 'Guardar cierre' }));
    await user.click(await screen.findByRole('button', { name: 'Verificar mismo cierre' }, ESPERA));
    await screen.findByRole('heading', { name: 'Cierre guardado' }, ESPERA);
    expect(solicitudes).toHaveLength(2);
    expect(solicitudes[0]?.id).toBe(solicitudes[1]?.id);
  });

  it('una venta nueva exige revisar el resumen actualizado antes de volver a cerrar', async () => {
    let revision = 'revision-1';
    server.use(
      http.get('/api/caja/resumen', () =>
        HttpResponse.json({
          fecha: '2026-11-13',
          dia: resumen,
          abierta: { ...resumen, revision },
          cierres: [],
          pagina: 0,
          hayMas: false,
        }),
      ),
      http.post('/api/caja/cierres', () => {
        revision = 'revision-2';
        return HttpResponse.json(
          {
            status: 409,
            message: 'Entraron nuevas ventas. Revisá el resumen actualizado antes de cerrar',
          },
          { status: 409 },
        );
      }),
    );
    const user = await abrir();
    await user.click(screen.getByRole('button', { name: 'Actividad' }));
    await user.click(screen.getByRole('button', { name: 'Caja abierta' }));
    await user.click(await screen.findByRole('button', { name: 'Cerrar caja' }));
    await user.click(screen.getByRole('button', { name: 'Guardar cierre' }));
    expect(
      await screen.findByRole('button', { name: 'Revisar resumen actualizado' }, ESPERA),
    ).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Guardar cierre' })).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Revisar resumen actualizado' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });
});
