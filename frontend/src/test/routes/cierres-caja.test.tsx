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

type Usuario = ReturnType<typeof userEvent.setup>;

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

let user: Usuario;
let solicitudDeCobro: SolicitudCobro | undefined;
let solicitudDeCierre: SolicitudCierreCaja | undefined;
let solicitudesDeCierre: SolicitudCierreCaja[] = [];
let revisionActual = 'revision-1';

describe('cierres y pagos mixtos de caja', () => {
  it('divide una venta y calcula vuelto solamente sobre la parte en efectivo', async () => {
    dadoUnServidorQueGuardaLaVenta();
    await dadaLaCajaConUnaCoca();

    await cuandoSeCobraUnaCocaEnDosMedios();

    entoncesSeVeLaTransferenciaYElVueltoSoloDelEfectivo();
  });

  it('el cobro mixto se envía con el efectivo y la transferencia', async () => {
    dadoUnServidorQueGuardaLaVenta();
    await dadaLaCajaConUnaCoca();
    await cuandoSeCobraUnaCocaEnDosMedios();

    await cuandoSeConfirmaElCobro();

    entoncesSeEnvioElCobroMixto();
  });

  it('un importe mixto que supera el total deshabilita la confirmación', async () => {
    await dadaLaCajaConUnaCoca();

    await cuandoSePideDividirElPagoConUnImporteMayorAlTotal();

    expect(screen.getByRole('button', { name: 'Confirmar cobro' })).toBeDisabled();
  });

  it('conserva el período abierto de ayer', async () => {
    dadoUnServidorConElPeriodoAbiertoDeAyer();
    await dadaLaCajaConUnaCoca();

    await cuandoSeAbreLaCajaAbierta();

    expect(await screen.findByText(/Abierta desde 12\/11\/26/)).toBeVisible();
  });

  it('guarda el cierre con el recuento contado', async () => {
    dadoUnServidorConElPeriodoAbiertoDeAyer();
    await dadaLaCajaConUnaCoca();
    await cuandoSeAbreLaCajaAbierta();

    await cuandoSeCierraLaCajaContando('1000');

    entoncesSeEnvioElCierreConLaRevisionYElContado();
  });

  it('el cierre guardado se puede recuperar desde el historial', async () => {
    dadoUnServidorConElPeriodoAbiertoDeAyer();
    await dadaLaCajaConUnaCoca();
    await cuandoSeAbreLaCajaAbierta();
    await cuandoSeCierraLaCajaContando('1000');

    await cuandoSeAbreElCierreDesdeElHistorial();

    entoncesElCierreSeVeSinRecuento();
  });

  it('un cierre incierto se vuelve a consultar con el mismo identificador', async () => {
    dadoUnServidorQueFallaElPrimerCierre();
    await dadaLaCajaConUnaCoca();
    await cuandoSeAbreLaCajaAbierta();

    await cuandoSeCierraLaCajaYSeVerificaElMismoCierre();

    entoncesSeReenvioElMismoCierre();
  });

  it('una venta nueva exige revisar el resumen actualizado antes de volver a cerrar', async () => {
    dadoUnServidorQueRechazaElCierreCon409();
    await dadaLaCajaConUnaCoca();
    await cuandoSeAbreLaCajaAbierta();

    await cuandoSeIntentaCerrarLaCaja();

    await entoncesSePideRevisarElResumenActualizado();
  });

  it('después de revisar el resumen actualizado se cierra el diálogo', async () => {
    dadoUnServidorQueRechazaElCierreCon409();
    await dadaLaCajaConUnaCoca();
    await cuandoSeAbreLaCajaAbierta();
    await cuandoSeIntentaCerrarLaCaja();

    await cuandoSeRevisaElResumenActualizado();

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });
});

async function dadaLaCajaConUnaCoca() {
  const usuario = setupUsuarioConSesion();
  const sistema = makeTemplate('shop', {}, false);
  const inventario = allBlocks(sistema).find((b) => b.type === 'inventory');
  if (!inventario) throw new Error('Falta la sección de stock');
  inventario.rows = [
    { id: crypto.randomUUID(), name: 'Coca-Cola', quantity: 10, price: 2700, favorite: true },
  ];
  prepararCaja(sistema, inventario.id);
  estado.sistemas.set(usuario.email, [sistema]);
  renderApp('/caja');
  await screen.findByRole('heading', { name: 'Caja', level: 1 }, ESPERA);
  user = userEvent.setup();
  await user.click(screen.getByRole('tab', { name: 'Favoritos' }));
}

function dadoUnServidorQueGuardaLaVenta() {
  server.use(
    http.post('/api/caja/ventas', async ({ request }) => {
      solicitudDeCobro = (await request.json()) as SolicitudCobro;
      return HttpResponse.json({
        ...venta,
        id: solicitudDeCobro.id,
        total: 2700,
        pagos: solicitudDeCobro.pagos,
        vuelto: 500,
      });
    }),
  );
}

function dadoUnServidorConElPeriodoAbiertoDeAyer() {
  let datos: ConsultaCaja = {
    fecha: '2026-11-13',
    dia: vacio,
    abierta: resumen,
    cierres: [],
    pagina: 0,
    hayMas: false,
  };
  server.use(
    http.get('/api/caja/resumen', () => HttpResponse.json(datos)),
    http.post('/api/caja/cierres', async ({ request }) => {
      solicitudDeCierre = (await request.json()) as SolicitudCierreCaja;
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
}

function dadoUnServidorQueFallaElPrimerCierre() {
  solicitudesDeCierre = [];
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
      solicitudesDeCierre.push((await request.json()) as SolicitudCierreCaja);
      return solicitudesDeCierre.length === 1 ? HttpResponse.error() : HttpResponse.json(cierre);
    }),
  );
}

function dadoUnServidorQueRechazaElCierreCon409() {
  revisionActual = 'revision-1';
  server.use(
    http.get('/api/caja/resumen', () =>
      HttpResponse.json({
        fecha: '2026-11-13',
        dia: resumen,
        abierta: { ...resumen, revision: revisionActual },
        cierres: [],
        pagina: 0,
        hayMas: false,
      }),
    ),
    http.post('/api/caja/cierres', () => {
      revisionActual = 'revision-2';
      return HttpResponse.json(
        {
          status: 409,
          message: 'Entraron nuevas ventas. Revisá el resumen actualizado antes de cerrar',
        },
        { status: 409 },
      );
    }),
  );
}

async function cuandoSeCobraUnaCocaEnDosMedios() {
  await user.click(screen.getByRole('button', { name: /Coca-Cola.*2.700/ }));
  await user.click(screen.getByRole('button', { name: /Cobrar.*2.700/ }));
  await user.click(screen.getByRole('button', { name: 'Dividir pago' }));
  await user.type(screen.getByLabelText('Importe en efectivo ($)'), '1000');
  await user.type(screen.getByLabelText('Efectivo recibido ($)'), '1500');
}

async function cuandoSeConfirmaElCobro() {
  await user.click(screen.getByRole('button', { name: 'Confirmar cobro' }));
  await screen.findByRole('heading', { name: 'Venta guardada' }, ESPERA);
}

async function cuandoSePideDividirElPagoConUnImporteMayorAlTotal() {
  await user.click(screen.getByRole('button', { name: /Coca-Cola.*2.700/ }));
  await user.click(screen.getByRole('button', { name: /Cobrar.*2.700/ }));
  await user.click(screen.getByRole('button', { name: 'Dividir pago' }));
  await user.type(screen.getByLabelText('Importe en efectivo ($)'), '3000');
}

async function cuandoSeAbreLaCajaAbierta() {
  await user.click(screen.getByRole('button', { name: 'Actividad' }));
  await user.click(screen.getByRole('button', { name: 'Caja abierta' }));
}

async function cuandoSeIntentaCerrarLaCaja() {
  await user.click(await screen.findByRole('button', { name: 'Cerrar caja' }));
  await user.type(screen.getByLabelText('Efectivo contado al cerrar ($)'), '1000');
  await user.click(screen.getByRole('button', { name: 'Guardar cierre' }));
}

async function cuandoSeCierraLaCajaContando(contado: string) {
  await user.click(screen.getByRole('button', { name: 'Cerrar caja' }));
  await user.type(screen.getByLabelText('Efectivo contado al cerrar ($)'), contado);
  await user.click(screen.getByRole('button', { name: 'Guardar cierre' }));
  await screen.findByRole('heading', { name: 'Cierre guardado' }, ESPERA);
  await user.click(screen.getByRole('button', { name: 'Listo' }));
}

async function cuandoSeAbreElCierreDesdeElHistorial() {
  await user.click(screen.getByRole('button', { name: 'Cierres guardados' }));
  await user.click(await screen.findByRole('button', { name: /1 ventas.*Desde/ }, ESPERA));
}

async function cuandoSeCierraLaCajaYSeVerificaElMismoCierre() {
  await cuandoSeIntentaCerrarLaCaja();
  await user.click(await screen.findByRole('button', { name: 'Verificar mismo cierre' }, ESPERA));
  await screen.findByRole('heading', { name: 'Cierre guardado' }, ESPERA);
}

async function cuandoSeRevisaElResumenActualizado() {
  await user.click(screen.getByRole('button', { name: 'Revisar resumen actualizado' }));
}

function entoncesSeVeLaTransferenciaYElVueltoSoloDelEfectivo() {
  expect(screen.getByText(/Transferencia:.*1.700/)).toBeVisible();
  expect(screen.getByText(/Vuelto:.*500/)).toBeVisible();
}

function entoncesSeEnvioElCobroMixto() {
  expect(solicitudDeCobro).toMatchObject({
    medio: 'MIXTO',
    recibido: 1500,
    pagos: [
      { medio: 'EFECTIVO', importe: 1000 },
      { medio: 'TRANSFERENCIA', importe: 1700 },
    ],
  });
}

function entoncesSeEnvioElCierreConLaRevisionYElContado() {
  expect(solicitudDeCierre).toMatchObject({
    revision: 'revision-1',
    fondoInicial: 0,
    entradas: 0,
    retiros: 0,
    contado: 1000,
  });
}

async function entoncesElCierreSeVeSinRecuento() {
  expect(await screen.findByRole('heading', { name: 'Cierre guardado' })).toBeVisible();
  expect(within(screen.getByRole('dialog')).getByText('Sin recuento')).toBeVisible();
}

function entoncesSeReenvioElMismoCierre() {
  expect(solicitudesDeCierre).toHaveLength(2);
  expect(solicitudesDeCierre[0]?.id).toBe(solicitudesDeCierre[1]?.id);
}

async function entoncesSePideRevisarElResumenActualizado() {
  const revisar = await screen.findByRole(
    'button',
    { name: 'Revisar resumen actualizado' },
    ESPERA,
  );
  expect(revisar).toBeVisible();
  expect(screen.queryByRole('button', { name: 'Guardar cierre' })).not.toBeInTheDocument();
}
