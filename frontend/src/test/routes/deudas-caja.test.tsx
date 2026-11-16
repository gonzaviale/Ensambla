import { cuentasDeCaja } from '@/mocks/db/cuentasCaja';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { allBlocks, makeTemplate } from '@/lib/sistema/core';
import { prepararCaja, idCaja } from '@/lib/caja';
import { clavePagoDeudaCaja } from '@/lib/cobrosDeudasCaja';
import { estado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion } from '@/test/testHelpers';
import type { CobroDeudaCaja, SolicitudPagoDeudaCaja, VentaCaja } from '@/types/caja';

async function abrir({ respuestaPerdida = false, recuperar = false } = {}) {
  const usuario = setupUsuarioConSesion();
  const sistema = makeTemplate('shop', {}, false);
  const inventario = allBlocks(sistema).find((b) => b.type === 'inventory');
  const clientes = allBlocks(sistema).find((b) => b.type === 'clients');
  if (!inventario || !clientes) throw new Error('Falta la estructura de prueba');
  prepararCaja(sistema, inventario.id);
  const clienteId = crypto.randomUUID();
  clientes.rows = [{ id: `e${clienteId}`, name: 'Ana López', phone: '1122334455' }];
  estado.sistemas.set(usuario.email, [sistema]);
  const ventas: VentaCaja[] = [
    {
      id: crypto.randomUUID(),
      lineas: [],
      medio: 'EFECTIVO',
      total: 10000,
      recibido: 4000,
      vuelto: 0,
      creadoEn: '2026-11-14T12:00:00Z',
      clienteId,
      clienteNombre: 'Ana López',
      cobrado: 4000,
      saldo: 6000,
      estado: 'PARCIAL',
      pagos: [{ medio: 'EFECTIVO', importe: 4000 }],
      pagosIniciales: [{ medio: 'EFECTIVO', importe: 4000 }],
    },
    {
      id: crypto.randomUUID(),
      lineas: [],
      medio: 'EFECTIVO',
      total: 3000,
      recibido: 0,
      vuelto: 0,
      creadoEn: '2026-11-14T13:00:00Z',
      clienteId,
      clienteNombre: 'Ana López',
      cobrado: 0,
      saldo: 3000,
      estado: 'PENDIENTE',
      pagos: [],
      pagosIniciales: [],
    },
  ];
  const cobros: CobroDeudaCaja[] = [];
  const solicitudes: SolicitudPagoDeudaCaja[] = [];
  const recibos = new Map<string, CobroDeudaCaja>();
  const guardar = (request: SolicitudPagoDeudaCaja) => {
    const previo = recibos.get(request.id);
    if (previo) return previo;
    for (const a of request.aplicaciones) {
      const venta = ventas.find((v) => v.id === a.ventaId);
      if (!venta) throw new Error('Falta venta');
      const importe = a.pagos.reduce((sum, p) => sum + p.importe, 0);
      venta.cobrado = (venta.cobrado ?? 0) + importe;
      venta.saldo = (venta.saldo ?? 0) - importe;
      venta.estado = venta.saldo === 0 ? 'PAGADA' : 'PARCIAL';
      venta.pagos.push(...a.pagos);
    }
    const cobro: CobroDeudaCaja = {
      id: request.id,
      clienteId,
      clienteNombre: 'Ana López',
      creadoEn: '2026-11-15T12:00:00Z',
      aplicaciones: request.aplicaciones.map((a) => ({
        ...a,
        importe: a.pagos.reduce((sum, p) => sum + p.importe, 0),
      })),
      importe: request.aplicaciones.flatMap((a) => a.pagos).reduce((sum, p) => sum + p.importe, 0),
    };
    cobros.push(cobro);
    recibos.set(cobro.id, cobro);
    return cobro;
  };
  const clave = clavePagoDeudaCaja(usuario.id, sistema.id, inventario.id, clienteId);
  if (recuperar) {
    const request: SolicitudPagoDeudaCaja = {
      id: crypto.randomUUID(),
      proyectoId: idCaja(sistema.id),
      inventarioId: idCaja(inventario.id),
      aplicaciones: ventas.map((v) => ({
        ventaId: v.id,
        pagos: [{ medio: 'TRANSFERENCIA', importe: v.saldo ?? 0 }],
      })),
    };
    guardar(request);
    sessionStorage.setItem(clave, JSON.stringify(request));
  }
  server.use(
    http.get('/api/caja/ventas', () =>
      HttpResponse.json({ ventas, cobros, cuentas: cuentasDeCaja(ventas, cobros) }),
    ),
    http.post('/api/caja/cobros', async ({ request }) => {
      const body = (await request.json()) as SolicitudPagoDeudaCaja;
      solicitudes.push(body);
      const cobro = guardar(body);
      if (respuestaPerdida && solicitudes.length === 1)
        return new HttpResponse(null, { status: 500 });
      return HttpResponse.json(cobro);
    }),
  );
  renderApp('/caja');
  await screen.findByRole('heading', { name: 'Caja', level: 1 }, ESPERA);
  const user = userEvent.setup();
  await user.click(screen.getByRole('button', { name: 'Clientes' }));
  await user.click(await screen.findByRole('button', { name: 'Ver cuenta de Ana López' }));
  return { user, ventas, cobros, solicitudes, clave };
}

describe('cuenta y cobros de deudas de clientes', () => {
  it('registra una entrega y después salda las ventas pendientes sin perder el historial', async () => {
    const { user, solicitudes, cobros } = await abrir();
    const cuenta = screen.getByRole('dialog', { name: 'Cuenta de Ana López' });
    await user.click(within(cuenta).getByRole('button', { name: 'Registrar pago' }));
    const formulario = within(screen.getByRole('dialog', { name: 'Registrar pago de deuda' }));
    await user.clear(formulario.getByLabelText('Importe del pago ($)'));
    await user.type(formulario.getByLabelText('Importe del pago ($)'), '1500');
    await user.click(formulario.getByRole('button', { name: 'Transferencia' }));
    await user.click(formulario.getByRole('button', { name: 'Confirmar pago' }));
    await screen.findByRole('dialog', { name: 'Pago registrado' }, ESPERA);
    await user.click(screen.getByRole('button', { name: 'Listo' }));
    await waitFor(
      () => expect(within(cuenta).getByText('7.500,00', { exact: false })).toBeVisible(),
      ESPERA,
    );
    await user.click(within(cuenta).getByRole('button', { name: 'Registrar pago' }));
    await user.click(screen.getByRole('button', { name: 'Confirmar pago' }));
    await screen.findByRole('dialog', { name: 'Pago registrado' }, ESPERA);
    await user.click(screen.getByRole('button', { name: 'Listo' }));

    expect(await within(cuenta).findByText('Sin deuda')).toBeVisible();
    expect(solicitudes).toHaveLength(2);
    expect(solicitudes[1]?.aplicaciones.map((a) => a.pagos[0]?.importe)).toEqual([4500, 3000]);
    expect(cobros).toHaveLength(2);
    expect(within(cuenta).getAllByText('Pagada')).toHaveLength(2);
  });

  it('reparte un pago mixto entre las ventas más antiguas con una vista previa', async () => {
    const { user, solicitudes } = await abrir();
    await user.click(
      within(screen.getByRole('dialog', { name: 'Cuenta de Ana López' })).getByRole('button', {
        name: 'Registrar pago',
      }),
    );
    const formulario = within(screen.getByRole('dialog', { name: 'Registrar pago de deuda' }));
    await user.clear(formulario.getByLabelText('Importe del pago ($)'));
    await user.type(formulario.getByLabelText('Importe del pago ($)'), '7000');
    await user.click(formulario.getByRole('button', { name: 'Dividir pago' }));
    await user.type(formulario.getByLabelText('Importe en efectivo ($)'), '2000');
    expect(formulario.getByRole('region', { name: 'Cómo se aplica el pago' })).toHaveTextContent(
      'Queda pagada',
    );
    await user.click(formulario.getByRole('button', { name: 'Confirmar pago' }));

    await screen.findByRole('dialog', { name: 'Pago registrado' }, ESPERA);
    expect(solicitudes[0]?.aplicaciones.map((a) => a.pagos)).toEqual([
      [
        { medio: 'EFECTIVO', importe: 2000 },
        { medio: 'TRANSFERENCIA', importe: 4000 },
      ],
      [{ medio: 'TRANSFERENCIA', importe: 1000 }],
    ]);
  });

  it('verifica un resultado incierto con el mismo id y sin volver a aplicar el pago', async () => {
    const { user, solicitudes, cobros } = await abrir({ respuestaPerdida: true });
    await user.click(
      within(screen.getByRole('dialog', { name: 'Cuenta de Ana López' })).getByRole('button', {
        name: 'Registrar pago',
      }),
    );
    await user.click(screen.getByRole('button', { name: 'Confirmar pago' }));
    await user.click(
      await screen.findByRole('button', { name: 'Verificar misma operación' }, ESPERA),
    );

    await screen.findByRole('dialog', { name: 'Pago registrado' }, ESPERA);
    expect(solicitudes).toHaveLength(2);
    expect(solicitudes[0]).toEqual(solicitudes[1]);
    expect(cobros).toHaveLength(1);
  });

  it('recupera una confirmación pendiente después de recargar aunque el saldo ya sea cero', async () => {
    const { user, clave, cobros } = await abrir({ recuperar: true });
    await user.click(screen.getByRole('button', { name: 'Verificar pago pendiente' }));
    expect(screen.getByRole('region', { name: 'Pago pendiente de verificar' })).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Verificar misma operación' }));

    await screen.findByRole('dialog', { name: 'Pago registrado' }, ESPERA);
    expect(sessionStorage.getItem(clave)).toBeNull();
    expect(cobros).toHaveLength(1);
  });

  it('impide confirmar importes que superan el saldo de la cuenta', async () => {
    const { user, solicitudes } = await abrir();
    await user.click(
      within(screen.getByRole('dialog', { name: 'Cuenta de Ana López' })).getByRole('button', {
        name: 'Registrar pago',
      }),
    );
    await user.clear(screen.getByLabelText('Importe del pago ($)'));
    await user.type(screen.getByLabelText('Importe del pago ($)'), '9001');

    expect(screen.getByRole('button', { name: 'Confirmar pago' })).toBeDisabled();
    expect(solicitudes).toHaveLength(0);
  });
});
