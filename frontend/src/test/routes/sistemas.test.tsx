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

describe('los sistemas llegan del servidor y vuelven a él', () => {
  it('carga los sistemas de la cuenta antes de mostrar el inicio', async () => {
    const usuario = setupUsuarioConSesion();
    estado.sistemas.set(usuario.email, [makeTemplate('shop', {}, true)]);

    renderApp('/inicio');

    await verifyHeadingPresentAsync(/En qué puedo/);
    expect(await screen.findByRole('button', { name: 'Mis sistemas · 1' }, ESPERA)).toBeVisible();
    expect(useSistemaStore.getState().sistemas).toHaveLength(1);
  });

  it('guarda en el servidor lo que se cambia, junto y sin repetirlo', async () => {
    const usuario = setupUsuarioConSesion();
    const sistema = makeTemplate('shop', {}, true);
    estado.sistemas.set(usuario.email, [sistema]);
    renderApp('/inicio');
    await verifyHeadingPresentAsync(/En qué puedo/);
    useSistemaStore.getState().abrirSistema(sistema.id, 'build');

    for (const nombre of ['Uno', 'Dos', 'Mi almacén de barrio']) {
      useSistemaStore.getState().commit((s) => {
        s.name = nombre;
      });
    }

    await waitFor(
      () => expect(estado.sistemas.get(usuario.email)?.[0]?.name).toBe('Mi almacén de barrio'),
      ESPERA,
    );
    await waitFor(() => expect(useSistemaStore.getState().pendientes).toEqual([]), ESPERA);
  });

  it('si no se puede guardar avisa y lo deja pendiente para reintentar', async () => {
    const usuario = setupUsuarioConSesion();
    const sistema = makeTemplate('shop', {}, false);
    estado.sistemas.set(usuario.email, [sistema]);
    renderApp('/inicio');
    await verifyHeadingPresentAsync(/En qué puedo/);
    useSistemaStore.getState().abrirSistema(sistema.id, 'build');
    setupServerError('put', `/sistemas/${sistema.id}`, 500, 'No se pudo guardar');

    useSistemaStore.getState().commit((s) => {
      s.name = 'Otro nombre';
    });

    await waitFor(
      () =>
        expect(useSistemaStore.getState().aviso?.texto).toMatch(/No pudimos guardar tus cambios/),
      ESPERA,
    );
    expect(useSistemaStore.getState().pendientes).toEqual([sistema.id]);
  });

  it('al cerrar la sesión no queda nada de la cuenta en memoria', async () => {
    const usuario = setupUsuarioConSesion();
    estado.sistemas.set(usuario.email, [makeTemplate('shop', {}, false)]);
    renderApp('/inicio');
    await verifyHeadingPresentAsync(/En qué puedo/);

    useSistemaStore.getState().reiniciar();

    expect(useSistemaStore.getState().sistemas).toEqual([]);
  });
});
