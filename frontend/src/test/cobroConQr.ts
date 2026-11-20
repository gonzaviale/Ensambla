import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { expect } from 'vitest';
import { estado } from '@/mocks/db/estado';
import {
  clickButton,
  setupServerError,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
  verifyTextPresentAsync,
  verifyToastAsync,
  ESPERA,
} from '@/test/testHelpers';
import { renderApp } from '@/test/renderApp';

type Usuario = ReturnType<typeof userEvent.setup>;

const ALIAS_DE_PRUEBA = 'mi.alias.mp';
const TITULAR_DE_PRUEBA = 'Ana Pérez';
const QR_DE_PRUEBA = 'QR de prueba con tu alias o CVU';

let user: Usuario;
let email = '';

const campoDelAlias = () => screen.getByLabelText('Alias o CVU');
const campoDelTitular = () => screen.getByLabelText('Nombre del titular (opcional)');
const botonDeGuardar = () => screen.getByRole('button', { name: 'Guardar cobro con QR' });
const destinoGuardado = () => estado.negocios.get(email);

async function abrirLaCuenta() {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez', email: 'ana@example.com' });
  email = usuario.email;
  renderApp('/cuenta');
  user = userEvent.setup();
  await verifyHeadingPresentAsync('Cobro con QR');
}

function sembrarElCobroGuardado() {
  estado.negocios.set('ana@example.com', {
    nombre: 'Mi negocio',
    rubro: 'OTRO',
    ubicacion: null,
    comparteDatosZona: true,
    zona: null,
    cobroAlias: ALIAS_DE_PRUEBA,
    cobroTitular: TITULAR_DE_PRUEBA,
  });
}

/* dado */

export const dadaLaCuentaSinCobroConQr = () => abrirLaCuenta();

export async function dadaLaCuentaConElCobroGuardado() {
  setupUsuarioConSesion({ nombre: 'Ana Pérez', email: 'ana@example.com' });
  sembrarElCobroGuardado();
  email = 'ana@example.com';
  renderApp('/cuenta');
  user = userEvent.setup();
  await verifyHeadingPresentAsync('Cobro con QR');
}

export const dadoQueNoSePuedeGuardarElNegocio = () =>
  setupServerError('patch', '/business', 500, 'Falló');

/* cuando */

export const cuandoSeEscribeElAlias = (alias: string) => user.type(campoDelAlias(), alias);

export const cuandoSeEscribeElTitular = (titular: string) => user.type(campoDelTitular(), titular);

export const cuandoSeBorraElAlias = () => user.clear(campoDelAlias());

export const cuandoSeGuardaElCobro = () => clickButton(user, 'Guardar cobro con QR');

/* entonces */

export async function entoncesSePuedeCargarElAliasYElTitular() {
  expect(await screen.findByLabelText('Alias o CVU')).toBeVisible();
  expect(campoDelTitular()).toBeVisible();
}

export const entoncesNoSeMuestraElQrDePrueba = () =>
  expect(screen.queryByRole('img', { name: QR_DE_PRUEBA })).toBeNull();

export const entoncesSeMuestraElQrDePrueba = async () =>
  expect(await screen.findByRole('img', { name: QR_DE_PRUEBA })).toBeVisible();

export const entoncesNoSePuedeGuardar = () => expect(botonDeGuardar()).toBeDisabled();

export const entoncesSePuedeGuardar = () => expect(botonDeGuardar()).toBeEnabled();

export const entoncesElCampoDelAliasTiene = (alias: string) =>
  expect(campoDelAlias()).toHaveValue(alias);

export const entoncesElCampoDelTitularTiene = (titular: string) =>
  expect(campoDelTitular()).toHaveValue(titular);

export const entoncesSeAvisaQueSeGuardo = () => verifyToastAsync('Guardamos tu cobro con QR.');

export const entoncesSeAvisaQueNoSePudoGuardar = () =>
  verifyTextPresentAsync('No pudimos guardar tu cobro con QR. Probá de nuevo en un rato.');

export const entoncesSeAclaraQueNoSeVerificaElPago = () =>
  verifyTextPresentAsync('Ensambla no verifica el pago ni toca tu plata: va directo a tu cuenta.');

export const entoncesElNegocioCobraEn = (alias: string | null, titular: string | null) =>
  waitFor(() => {
    expect(destinoGuardado()?.cobroAlias).toBe(alias);
    expect(destinoGuardado()?.cobroTitular).toBe(titular);
  }, ESPERA);

export const entoncesElNegocioNoGuardoNada = () => expect(destinoGuardado()).toBeUndefined();

export { ALIAS_DE_PRUEBA, TITULAR_DE_PRUEBA };
