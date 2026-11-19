import { screen, waitFor } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { makeTemplate } from '@/lib/sistema/core';
import { estado } from '@/mocks/db/estado';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  setupServerError,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
} from '@/test/testHelpers';
import { useSistemaStore } from '@/stores/sistemaStore';
import type { Sistema } from '@/lib/sistema/tipos';

let email = '';
let sistema: Sistema;

describe('los sistemas llegan del servidor y vuelven a él', () => {
  it('carga los sistemas de la cuenta antes de mostrar el inicio', async () => {
    dadaUnaCuentaConUnSistema(true);

    await cuandoSeAbreElInicio();

    await entoncesSeMuestraElSistemaDeLaCuenta();
  });

  it('guarda en el servidor lo que se cambia, junto y sin repetirlo', async () => {
    dadaUnaCuentaConUnSistema(true);
    await cuandoSeAbreElInicio();
    cuandoSeAbreElSistemaParaEditar();

    cuandoSeRenombraTresVeces();

    await entoncesElServidorTieneElNombreFinal('Mi almacén de barrio');
    await entoncesNoQuedanCambiosPendientes();
  });

  it('si no se puede guardar avisa', async () => {
    dadaUnaCuentaConUnSistema(false);
    await cuandoSeAbreElInicio();
    cuandoSeAbreElSistemaParaEditar();
    dadoQueElServidorNoPuedeGuardar();

    cuandoSeRenombraUnaVez('Otro nombre');

    await entoncesSeAvisaQueNoSePudoGuardar();
  });

  it('si no se puede guardar deja el cambio pendiente para reintentar', async () => {
    dadaUnaCuentaConUnSistema(false);
    await cuandoSeAbreElInicio();
    cuandoSeAbreElSistemaParaEditar();
    dadoQueElServidorNoPuedeGuardar();

    cuandoSeRenombraUnaVez('Otro nombre');

    await entoncesSeAvisaQueNoSePudoGuardar();
    entoncesElSistemaQuedaPendiente();
  });

  it('al cerrar la sesión no queda nada de la cuenta en memoria', async () => {
    dadaUnaCuentaConUnSistema(false);
    await cuandoSeAbreElInicio();

    cuandoSeCierraLaSesion();

    entoncesNoQuedanSistemasEnMemoria();
  });
});

function dadaUnaCuentaConUnSistema(conEjemplos: boolean) {
  email = setupUsuarioConSesion().email;
  sistema = makeTemplate('shop', {}, conEjemplos);
  estado.sistemas.set(email, [sistema]);
}

function dadoQueElServidorNoPuedeGuardar() {
  setupServerError('put', `/sistemas/${sistema.id}`, 500, 'No se pudo guardar');
}

async function cuandoSeAbreElInicio() {
  renderApp('/inicio');
  await verifyHeadingPresentAsync(/En qué puedo/);
}

function cuandoSeAbreElSistemaParaEditar() {
  useSistemaStore.getState().abrirSistema(sistema.id, 'build');
}

function cuandoSeRenombraUnaVez(nombre: string) {
  useSistemaStore.getState().commit((s) => {
    s.name = nombre;
  });
}

function cuandoSeRenombraTresVeces() {
  for (const nombre of ['Uno', 'Dos', 'Mi almacén de barrio']) cuandoSeRenombraUnaVez(nombre);
}

function cuandoSeCierraLaSesion() {
  useSistemaStore.getState().reiniciar();
}

async function entoncesSeMuestraElSistemaDeLaCuenta() {
  expect(await screen.findByRole('button', { name: 'Mi negocio' }, ESPERA)).toBeVisible();
  await waitFor(() => expect(useSistemaStore.getState().sistemas).toHaveLength(1), ESPERA);
}

async function entoncesElServidorTieneElNombreFinal(nombre: string) {
  await waitFor(() => expect(estado.sistemas.get(email)?.[0]?.name).toBe(nombre), ESPERA);
}

async function entoncesNoQuedanCambiosPendientes() {
  await waitFor(() => expect(useSistemaStore.getState().pendientes).toEqual([]), ESPERA);
}

async function entoncesSeAvisaQueNoSePudoGuardar() {
  await waitFor(
    () => expect(useSistemaStore.getState().aviso?.texto).toMatch(/No pudimos guardar tus cambios/),
    ESPERA,
  );
}

function entoncesElSistemaQuedaPendiente() {
  expect(useSistemaStore.getState().pendientes).toEqual([sistema.id]);
}

function entoncesNoQuedanSistemasEnMemoria() {
  expect(useSistemaStore.getState().sistemas).toEqual([]);
}
