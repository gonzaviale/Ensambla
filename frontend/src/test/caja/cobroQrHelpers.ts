import { screen, waitFor, within } from '@testing-library/react';
import type userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { expect } from 'vitest';
import { estado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { ESPERA } from '@/test/testHelpers';
import type { CobroQr, SolicitudCobroQr } from '@/types/caja';

type Usuario = ReturnType<typeof userEvent.setup>;

export const ALIAS_DEL_COMERCIO = 'mi.alias.mp';
export const TITULAR_DEL_COMERCIO = 'Ana Pérez';

let generados: SolicitudCobroQr[] = [];
let cancelados: string[] = [];
let estadoRemoto: CobroQr['estado'] = 'PENDIENTE';

const dialogoDelQr = () => screen.getByRole('dialog', { name: 'Cobrar con QR' });

const cobroQr = (
  id: string,
  importe: number,
  estadoDelCobro: CobroQr['estado'],
  venceEn = '2099-11-19T18:45:00Z',
): CobroQr => ({
  id,
  importe,
  alias: ALIAS_DEL_COMERCIO,
  titular: TITULAR_DEL_COMERCIO,
  estado: estadoDelCobro,
  venceEn,
  qrDinamico: '000201-qr-dinamico-mercado-pago',
  qrEstatico: '000201-qr-estatico-mercado-pago',
  proveedor: 'MERCADO_PAGO',
});

/* dado */

export function dadoUnNegocioQueCobraEnSuAlias(email: string) {
  estado.negocios.set(email, {
    nombre: 'Mi negocio',
    rubro: 'OTRO',
    ubicacion: null,
    comparteDatosZona: true,
    zona: null,
    cobroAlias: ALIAS_DEL_COMERCIO,
    cobroTitular: TITULAR_DEL_COMERCIO,
  });
}

export function dadoUnServidorQueGeneraYCancelaCobrosQr() {
  generados = [];
  cancelados = [];
  estadoRemoto = 'PENDIENTE';
  server.use(
    http.post('/api/caja/cobros-qr', async ({ request }) => {
      const solicitud = (await request.json()) as SolicitudCobroQr;
      generados.push(solicitud);
      return HttpResponse.json(cobroQr(solicitud.id, solicitud.importe, 'PENDIENTE'));
    }),
    http.get('/api/caja/cobros-qr/:id', ({ params }) => {
      const generado = generados.find((item) => item.id === String(params.id));
      return HttpResponse.json(cobroQr(String(params.id), generado?.importe ?? 0, estadoRemoto));
    }),
    http.post('/api/caja/cobros-qr/:id/cancelar', ({ params }) => {
      cancelados.push(String(params.id));
      return HttpResponse.json(cobroQr(String(params.id), 0, 'CANCELADO'));
    }),
  );
}

export function dadoUnServidorQueNoPuedeGenerarCobrosQr() {
  server.use(http.post('/api/caja/cobros-qr', () => new HttpResponse(null, { status: 500 })));
}

export function dadoUnServidorQueGeneraUnCobroVencido() {
  server.use(
    http.post('/api/caja/cobros-qr', async ({ request }) => {
      const solicitud = (await request.json()) as SolicitudCobroQr;
      generados.push(solicitud);
      return HttpResponse.json(
        cobroQr(solicitud.id, solicitud.importe, 'VENCIDO', '2020-11-19T18:45:00Z'),
      );
    }),
  );
}

/* cuando */

export const cuandoSeCobraConQr = (user: Usuario) =>
  user.click(screen.getByRole('button', { name: 'Cobrar con QR' }));

export const cuandoYaLeTransfirieron = async (user: Usuario) => {
  void user;
  estadoRemoto = 'COBRADO';
  await waitFor(
    () => expect(within(dialogoDelQr()).getByText('Pago acreditado por Mercado Pago.')).toBeVisible(),
    { timeout: 2_500 },
  );
};

export const cuandoSeCancelaElCobroConQr = (user: Usuario) =>
  user.click(within(dialogoDelQr()).getByRole('button', { name: 'Cancelar cobro con QR' }));

export const cuandoSeCopiaElAlias = (user: Usuario) =>
  user.click(within(dialogoDelQr()).getByText('Usar el QR fijo del mostrador'));

/* entonces */

export const entoncesNoSeOfreceCobrarConQr = () =>
  expect(screen.queryByRole('button', { name: 'Cobrar con QR' })).toBeNull();

export const entoncesSeOfreceCobrarConQr = () =>
  expect(screen.getByRole('button', { name: 'Cobrar con QR' })).toBeEnabled();

export async function entoncesSeMuestraElQrPor(monto: RegExp) {
  const dialogo = within(await screen.findByRole('dialog', { name: 'Cobrar con QR' }));
  expect(await dialogo.findByText(monto)).toBeVisible();
  expect(
    await dialogo.findByRole('img', {
      name: 'QR dinámico de Mercado Pago con el importe de la compra',
    }, ESPERA),
  ).toBeVisible();
}

export async function entoncesSeMuestranElAliasYElTitular() {
  const dialogo = within(await screen.findByRole('dialog', { name: 'Cobrar con QR' }));
  expect(await dialogo.findByText('Usar el QR fijo del mostrador')).toBeVisible();
}

export async function entoncesSeAvisaQueEnsamblaNoVerificoElPago() {
  const dialogo = within(await screen.findByRole('dialog', { name: 'Cobrar con QR' }));
  expect(await dialogo.findByText(/confirmará la venta automáticamente/)).toBeVisible();
}

export async function entoncesSeExplicaComoIngresarElImporte() {
  const dialogo = within(await screen.findByRole('dialog', { name: 'Cobrar con QR' }));
  expect(
    await dialogo.findByText('Escanealo con Mercado Pago. El importe ya está cargado y no hace falta escribirlo.'),
  ).toBeVisible();
}

export async function entoncesSeVerificaAntesDeCancelar() {
  const dialogo = within(dialogoDelQr());
  await waitFor(
    () => expect(dialogo.getByRole('button', { name: 'Verificar y guardar venta' })).toBeEnabled(),
    ESPERA,
  );
  expect(dialogo.getByRole('button', { name: 'Cancelar cobro con QR' })).toBeDisabled();
  expect(dialogo.getByRole('button', { name: 'Cerrar' })).toBeDisabled();
}

export async function entoncesSeIndicaHastaCuandoVale() {
  const dialogo = within(await screen.findByRole('dialog', { name: 'Cobrar con QR' }));
  expect(await dialogo.findByText(/Vale hasta las 15:45/)).toBeVisible();
}

export async function entoncesSeIndicaQueElCobroVencio() {
  const dialogo = within(await screen.findByRole('dialog', { name: 'Cobrar con QR' }));
  expect(await dialogo.findByText(/Este QR venció/)).toBeVisible();
  expect(dialogo.getByRole('button', { name: 'Esperando el pago…' })).toBeDisabled();
}

export async function entoncesSeCopioElAlias() {
  const dialogo = within(dialogoDelQr());
  expect(await dialogo.findByRole('img', { name: 'QR fijo de la caja de Mercado Pago' })).toBeVisible();
}

export async function entoncesSePideCargarElAlias() {
  const dialogo = within(await screen.findByRole('dialog', { name: 'Cobrar con QR' }));
  expect(await dialogo.findByRole('img', {
    name: 'QR dinámico de Mercado Pago con el importe de la compra',
  })).toBeVisible();
}

export async function entoncesSeAvisaQueNoSePudoGenerarElQr() {
  const dialogo = within(await screen.findByRole('dialog', { name: 'Cobrar con QR' }));
  expect(await dialogo.findByRole('alert')).toHaveTextContent(
    'No pudimos generar el QR. Probá de nuevo en un rato.',
  );
}

export async function entoncesElQrMuestraElError(mensaje: string) {
  const dialogo = within(dialogoDelQr());
  expect(await dialogo.findByRole('alert')).toHaveTextContent(mensaje);
}

export const entoncesSePidioElQrPorImporte = (importe: number) =>
  expect(generados.map((cobro) => cobro.importe)).toEqual([importe]);

export const entoncesSeCanceloElCobroQrGenerado = () =>
  waitFor(() => expect(cancelados).toEqual(generados.map((cobro) => cobro.id)), ESPERA);

export const entoncesNoSeCancelaNingunCobroQr = () => expect(cancelados).toEqual([]);

export const entoncesSeGeneroUnSoloCobroQr = () => expect(generados).toHaveLength(1);

export const elIdDelCobroQrGenerado = () => generados[0]?.id;
