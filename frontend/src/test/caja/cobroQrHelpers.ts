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

const dialogoDelQr = () => screen.getByRole('dialog', { name: 'Cobrar con QR' });

const cobroQr = (id: string, importe: number, estadoDelCobro: CobroQr['estado']): CobroQr => ({
  id,
  importe,
  alias: ALIAS_DEL_COMERCIO,
  titular: TITULAR_DEL_COMERCIO,
  estado: estadoDelCobro,
  venceEn: '2026-11-19T18:45:00Z',
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
  server.use(
    http.post('/api/caja/cobros-qr', async ({ request }) => {
      const solicitud = (await request.json()) as SolicitudCobroQr;
      generados.push(solicitud);
      return HttpResponse.json(cobroQr(solicitud.id, solicitud.importe, 'PENDIENTE'));
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

/* cuando */

export const cuandoSeCobraConQr = (user: Usuario) =>
  user.click(screen.getByRole('button', { name: 'Cobrar con QR' }));

export const cuandoYaLeTransfirieron = (user: Usuario) =>
  user.click(within(dialogoDelQr()).getByRole('button', { name: 'Ya me transfirieron' }));

export const cuandoSeCancelaElCobroConQr = (user: Usuario) =>
  user.click(within(dialogoDelQr()).getByRole('button', { name: 'Cancelar cobro con QR' }));

export const cuandoSeCopiaElAlias = (user: Usuario) =>
  user.click(within(dialogoDelQr()).getByRole('button', { name: 'Copiar alias' }));

/* entonces */

export const entoncesNoSeOfreceCobrarConQr = () =>
  expect(screen.queryByRole('button', { name: 'Cobrar con QR' })).toBeNull();

export const entoncesSeOfreceCobrarConQr = () =>
  expect(screen.getByRole('button', { name: 'Cobrar con QR' })).toBeEnabled();

export async function entoncesSeMuestraElQrPor(monto: RegExp) {
  const dialogo = within(await screen.findByRole('dialog', { name: 'Cobrar con QR' }));
  expect(await dialogo.findByText(monto)).toBeVisible();
  expect(dialogo.getByRole('img', { name: 'QR con tu alias o CVU para transferir' })).toBeVisible();
}

export async function entoncesSeMuestranElAliasYElTitular() {
  const dialogo = within(await screen.findByRole('dialog', { name: 'Cobrar con QR' }));
  expect(await dialogo.findByText(ALIAS_DEL_COMERCIO)).toBeVisible();
  expect(dialogo.getByText(`Titular: ${TITULAR_DEL_COMERCIO}`)).toBeVisible();
}

export async function entoncesSeAvisaQueEnsamblaNoVerificoElPago() {
  const dialogo = within(await screen.findByRole('dialog', { name: 'Cobrar con QR' }));
  expect(await dialogo.findByText(/Ensambla no verificó el pago/)).toBeVisible();
}

export async function entoncesSeIndicaHastaCuandoVale() {
  const dialogo = within(await screen.findByRole('dialog', { name: 'Cobrar con QR' }));
  expect(await dialogo.findByText('Vale hasta las 15:45.')).toBeVisible();
}

export async function entoncesSeCopioElAlias() {
  const dialogo = within(dialogoDelQr());
  expect(await dialogo.findByText('Alias copiado.')).toBeVisible();
}

export async function entoncesSePideCargarElAlias() {
  const dialogo = within(await screen.findByRole('dialog', { name: 'Cobrar con QR' }));
  expect(await dialogo.findByText(/Todavía no cargaste tu alias o CVU/)).toBeVisible();
  expect(dialogo.getByRole('link', { name: 'Cargar mi alias o CVU' })).toHaveAttribute(
    'href',
    '/cuenta?seccion=perfil',
  );
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
