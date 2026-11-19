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

type Usuario = ReturnType<typeof userEvent.setup>;

let user: Usuario;
let ventas: VentaCaja[];
let cobros: CobroDeudaCaja[];
let solicitudes: SolicitudPagoDeudaCaja[];
let recibos: Map<string, CobroDeudaCaja>;
let claveDelPagoPendiente = '';
let clienteId = '';

describe('cuenta y cobros de fiado de clientes', () => {
  it('registra una entrega y la cuenta queda con el saldo restante', async () => {
    await dadaLaCuentaDeAnaConDosVentasPendientes();

    await cuandoSeGuardaUnPagoDe('1500', 'Transferencia');

    await entoncesLaCuentaMuestraElSaldo('7.500,00');
  });

  it('salda las ventas pendientes sin perder el historial', async () => {
    await dadaLaCuentaDeAnaConDosVentasPendientes();
    await cuandoSeGuardaUnPagoDe('1500', 'Transferencia');

    await cuandoSeSaldaLoQueQueda();

    await entoncesLaCuentaQuedaSaldadaConSuHistorial();
  });

  it('el pago final se aplica a cada venta por lo que debía', async () => {
    await dadaLaCuentaDeAnaConDosVentasPendientes();
    await cuandoSeGuardaUnPagoDe('1500', 'Transferencia');

    await cuandoSeSaldaLoQueQueda();

    await entoncesElUltimoPagoSeAplicoACadaVenta([4500, 3000]);
  });

  it('reparte un pago mixto entre las ventas más antiguas con una vista previa', async () => {
    await dadaLaCuentaDeAnaConDosVentasPendientes();

    await cuandoSePrepararUnPagoMixtoDe7000();

    entoncesLaVistaPreviaDiceQueQuedaPagada();
  });

  it('el pago mixto se reparte entre las ventas más antiguas', async () => {
    await dadaLaCuentaDeAnaConDosVentasPendientes();
    await cuandoSePrepararUnPagoMixtoDe7000();

    await cuandoSeConfirmaElPago();

    await entoncesElPagoMixtoSeRepartioEntreLasVentas();
  });

  it('verifica un resultado incierto con el mismo id', async () => {
    await dadaLaCuentaDeAnaConRespuestaPerdida();

    await cuandoSeGuardaElPagoYSeVerificaLaMismaOperacion();

    await entoncesSeReenvioLaMismaSolicitud();
  });

  it('verificar un resultado incierto no vuelve a aplicar el pago', async () => {
    await dadaLaCuentaDeAnaConRespuestaPerdida();

    await cuandoSeGuardaElPagoYSeVerificaLaMismaOperacion();

    await entoncesElPagoSeAplicoUnaSolaVez();
  });

  it('recupera una confirmación pendiente después de recargar aunque el saldo ya sea cero', async () => {
    await dadaLaCuentaDeAnaConUnPagoPendienteDeVerificar();

    await cuandoSeVerificaElPagoPendiente();

    await entoncesElPagoPendienteSeResolvioSinDuplicarse();
  });

  it('impide confirmar importes que superan el saldo de la cuenta', async () => {
    await dadaLaCuentaDeAnaConDosVentasPendientes();

    await cuandoSeEscribeUnImporteMayorAlSaldo('9001');

    entoncesNoSePuedeConfirmarNiSeEnvioNada();
  });
});

const cuenta = () => screen.getByRole('dialog', { name: 'Cuenta de Ana López' });

function ventaPendiente(cambios: Partial<VentaCaja>): VentaCaja {
  return {
    id: crypto.randomUUID(),
    lineas: [],
    medio: 'EFECTIVO',
    clienteId,
    clienteNombre: 'Ana López',
    ...cambios,
  } as VentaCaja;
}

const dosVentasDeAna = (): VentaCaja[] => [
  ventaPendiente({
    total: 10000,
    recibido: 4000,
    vuelto: 0,
    creadoEn: '2026-11-14T12:00:00Z',
    cobrado: 4000,
    saldo: 6000,
    estado: 'PARCIAL',
    pagos: [{ medio: 'EFECTIVO', importe: 4000 }],
    pagosIniciales: [{ medio: 'EFECTIVO', importe: 4000 }],
  }),
  ventaPendiente({
    total: 3000,
    recibido: 0,
    vuelto: 0,
    creadoEn: '2026-11-14T13:00:00Z',
    cobrado: 0,
    saldo: 3000,
    estado: 'PENDIENTE',
    pagos: [],
    pagosIniciales: [],
  }),
];

const sumaDePagos = (pagos: { importe: number }[]) =>
  pagos.reduce((suma, p) => suma + p.importe, 0);

function guardarCobro(solicitud: SolicitudPagoDeudaCaja): CobroDeudaCaja {
  const previo = recibos.get(solicitud.id);
  if (previo) return previo;
  for (const aplicacion of solicitud.aplicaciones) {
    const venta = ventas.find((v) => v.id === aplicacion.ventaId);
    if (!venta) throw new Error('Falta venta');
    const importe = sumaDePagos(aplicacion.pagos);
    venta.cobrado = (venta.cobrado ?? 0) + importe;
    venta.saldo = (venta.saldo ?? 0) - importe;
    venta.estado = venta.saldo === 0 ? 'PAGADA' : 'PARCIAL';
    venta.pagos.push(...aplicacion.pagos);
  }
  const cobro: CobroDeudaCaja = {
    id: solicitud.id,
    clienteId,
    clienteNombre: 'Ana López',
    creadoEn: '2026-11-15T12:00:00Z',
    aplicaciones: solicitud.aplicaciones.map((a) => ({ ...a, importe: sumaDePagos(a.pagos) })),
    importe: sumaDePagos(solicitud.aplicaciones.flatMap((a) => a.pagos)),
  };
  cobros.push(cobro);
  recibos.set(cobro.id, cobro);
  return cobro;
}

async function abrirLaCuentaDeAna({ respuestaPerdida = false, recuperar = false } = {}) {
  const usuario = setupUsuarioConSesion();
  const sistema = makeTemplate('shop', {}, false);
  const inventario = allBlocks(sistema).find((b) => b.type === 'inventory');
  const clientes = allBlocks(sistema).find((b) => b.type === 'clients');
  if (!inventario || !clientes) throw new Error('Falta la estructura de prueba');
  prepararCaja(sistema, inventario.id);
  clienteId = crypto.randomUUID();
  clientes.rows = [{ id: `e${clienteId}`, name: 'Ana López', phone: '1122334455' }];
  estado.sistemas.set(usuario.email, [sistema]);
  ventas = dosVentasDeAna();
  cobros = [];
  solicitudes = [];
  recibos = new Map();
  claveDelPagoPendiente = clavePagoDeudaCaja(usuario.id, sistema.id, inventario.id, clienteId);
  if (recuperar) dejarUnPagoPendiente(sistema.id, inventario.id);
  servirLaCaja(respuestaPerdida);
  renderApp('/caja');
  await screen.findByRole('heading', { name: 'Caja', level: 1 }, ESPERA);
  user = userEvent.setup();
  await user.click(screen.getByRole('button', { name: 'Clientes' }));
  await user.click(await screen.findByRole('button', { name: 'Ver cuenta de Ana López' }));
}

function dejarUnPagoPendiente(sistemaId: string, inventarioId: string) {
  const solicitud: SolicitudPagoDeudaCaja = {
    id: crypto.randomUUID(),
    proyectoId: idCaja(sistemaId),
    inventarioId: idCaja(inventarioId),
    aplicaciones: ventas.map((v) => ({
      ventaId: v.id,
      pagos: [{ medio: 'TRANSFERENCIA', importe: v.saldo ?? 0 }],
    })),
  };
  guardarCobro(solicitud);
  sessionStorage.setItem(claveDelPagoPendiente, JSON.stringify(solicitud));
}

function servirLaCaja(respuestaPerdida: boolean) {
  server.use(
    http.get('/api/caja/ventas', () =>
      HttpResponse.json({ ventas, cobros, cuentas: cuentasDeCaja(ventas, cobros) }),
    ),
    http.post('/api/caja/cobros', async ({ request }) => {
      const solicitud = (await request.json()) as SolicitudPagoDeudaCaja;
      solicitudes.push(solicitud);
      const cobro = guardarCobro(solicitud);
      if (respuestaPerdida && solicitudes.length === 1)
        return new HttpResponse(null, { status: 500 });
      return HttpResponse.json(cobro);
    }),
  );
}

// --- dado ---

async function dadaLaCuentaDeAnaConDosVentasPendientes() {
  await abrirLaCuentaDeAna();
}

async function dadaLaCuentaDeAnaConRespuestaPerdida() {
  await abrirLaCuentaDeAna({ respuestaPerdida: true });
}

async function dadaLaCuentaDeAnaConUnPagoPendienteDeVerificar() {
  await abrirLaCuentaDeAna({ recuperar: true });
}

// --- cuando ---

async function cuandoSeGuardaUnPagoDe(importe: string, medio: string) {
  await user.click(within(cuenta()).getByRole('button', { name: 'Guardar pago' }));
  const formulario = within(screen.getByRole('dialog', { name: 'Guardar pago del fiado' }));
  await user.clear(formulario.getByLabelText('Importe del pago ($)'));
  await user.type(formulario.getByLabelText('Importe del pago ($)'), importe);
  await user.click(formulario.getByRole('button', { name: medio }));
  await user.click(formulario.getByRole('button', { name: 'Confirmar pago' }));
  await screen.findByRole('dialog', { name: 'Pago guardado' }, ESPERA);
  await user.click(screen.getByRole('button', { name: 'Listo' }));
}

async function cuandoSeSaldaLoQueQueda() {
  await waitFor(
    () => expect(within(cuenta()).getByText('7.500,00', { exact: false })).toBeVisible(),
    ESPERA,
  );
  await user.click(within(cuenta()).getByRole('button', { name: 'Guardar pago' }));
  await user.click(screen.getByRole('button', { name: 'Confirmar pago' }));
  await screen.findByRole('dialog', { name: 'Pago guardado' }, ESPERA);
  await user.click(screen.getByRole('button', { name: 'Listo' }));
}

async function cuandoSePrepararUnPagoMixtoDe7000() {
  await user.click(within(cuenta()).getByRole('button', { name: 'Guardar pago' }));
  const formulario = within(screen.getByRole('dialog', { name: 'Guardar pago del fiado' }));
  await user.clear(formulario.getByLabelText('Importe del pago ($)'));
  await user.type(formulario.getByLabelText('Importe del pago ($)'), '7000');
  await user.click(formulario.getByRole('button', { name: 'Dividir pago' }));
  await user.type(formulario.getByLabelText('Importe en efectivo ($)'), '2000');
}

async function cuandoSeConfirmaElPago() {
  await user.click(screen.getByRole('button', { name: 'Confirmar pago' }));
  await screen.findByRole('dialog', { name: 'Pago guardado' }, ESPERA);
}

async function cuandoSeGuardaElPagoYSeVerificaLaMismaOperacion() {
  await user.click(within(cuenta()).getByRole('button', { name: 'Guardar pago' }));
  await user.click(screen.getByRole('button', { name: 'Confirmar pago' }));
  await user.click(
    await screen.findByRole('button', { name: 'Verificar misma operación' }, ESPERA),
  );
  await screen.findByRole('dialog', { name: 'Pago guardado' }, ESPERA);
}

async function cuandoSeVerificaElPagoPendiente() {
  await user.click(screen.getByRole('button', { name: 'Verificar pago pendiente' }));
  expect(screen.getByRole('region', { name: 'Pago pendiente de verificar' })).toBeVisible();
  await user.click(screen.getByRole('button', { name: 'Verificar misma operación' }));
  await screen.findByRole('dialog', { name: 'Pago guardado' }, ESPERA);
}

async function cuandoSeEscribeUnImporteMayorAlSaldo(importe: string) {
  await user.click(within(cuenta()).getByRole('button', { name: 'Guardar pago' }));
  await user.clear(screen.getByLabelText('Importe del pago ($)'));
  await user.type(screen.getByLabelText('Importe del pago ($)'), importe);
}

// --- entonces ---

async function entoncesLaCuentaMuestraElSaldo(saldo: string) {
  await waitFor(
    () => expect(within(cuenta()).getByText(saldo, { exact: false })).toBeVisible(),
    ESPERA,
  );
}

async function entoncesLaCuentaQuedaSaldadaConSuHistorial() {
  expect(await within(cuenta()).findByText('No debe nada')).toBeVisible();
  expect(solicitudes).toHaveLength(2);
  expect(cobros).toHaveLength(2);
  expect(within(cuenta()).getAllByText('Pagada')).toHaveLength(2);
}

async function entoncesElUltimoPagoSeAplicoACadaVenta(importes: number[]) {
  await within(cuenta()).findByText('No debe nada');
  expect(solicitudes[1]?.aplicaciones.map((a) => a.pagos[0]?.importe)).toEqual(importes);
}

function entoncesLaVistaPreviaDiceQueQuedaPagada() {
  const vista = screen.getByRole('region', { name: 'Cómo se aplica el pago' });
  expect(vista).toHaveTextContent('Queda pagada');
}

async function entoncesElPagoMixtoSeRepartioEntreLasVentas() {
  expect(solicitudes[0]?.aplicaciones.map((a) => a.pagos)).toEqual([
    [
      { medio: 'EFECTIVO', importe: 2000 },
      { medio: 'TRANSFERENCIA', importe: 4000 },
    ],
    [{ medio: 'TRANSFERENCIA', importe: 1000 }],
  ]);
}

async function entoncesSeReenvioLaMismaSolicitud() {
  expect(solicitudes).toHaveLength(2);
  expect(solicitudes[0]).toEqual(solicitudes[1]);
}

async function entoncesElPagoSeAplicoUnaSolaVez() {
  expect(cobros).toHaveLength(1);
}

async function entoncesElPagoPendienteSeResolvioSinDuplicarse() {
  expect(sessionStorage.getItem(claveDelPagoPendiente)).toBeNull();
  expect(cobros).toHaveLength(1);
}

function entoncesNoSePuedeConfirmarNiSeEnvioNada() {
  expect(screen.getByRole('button', { name: 'Confirmar pago' })).toBeDisabled();
  expect(solicitudes).toHaveLength(0);
}
