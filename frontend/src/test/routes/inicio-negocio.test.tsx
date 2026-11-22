import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { makeTemplate } from '@/lib/sistema/core';
import { estado } from '@/mocks/db/estado';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion, verifyHeadingPresentAsync } from '@/test/testHelpers';

const cuadernoLlamado = (nombre: string) => {
  const sistema = makeTemplate('shop', {}, true);
  sistema.name = nombre;
  return sistema;
};

const dadoNegocios = (email: string, nombres: string[]) => {
  if (nombres.length) estado.sistemas.set(email, nombres.map(cuadernoLlamado));
};

const cuandoAbreInicio = async (nombres: string[] = []) => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  dadoNegocios(usuario.email, nombres);
  const router = renderApp('/inicio');
  await verifyHeadingPresentAsync(/En qué puedo/);
  return { router, user: userEvent.setup() };
};

const bloqueDeTuNegocio = async () =>
  within(await screen.findByRole('region', { name: 'Tu negocio' }, ESPERA));

const cuandoTocaMiNegocio = async (user: ReturnType<typeof userEvent.setup>) =>
  user.click(screen.getByRole('button', { name: 'Mi negocio' }));

const entoncesSeOfreceCrearElNegocio = async () =>
  expect(
    await screen.findByRole('heading', { name: /Un lienzo en blanco/ }, ESPERA),
  ).toBeVisible();

describe('inicio · tu negocio', () => {
  it('sin negocios, «Mi negocio» abre las opciones para crear el primero', async () => {
    const { user } = await cuandoAbreInicio();

    await cuandoTocaMiNegocio(user);

    await entoncesSeOfreceCrearElNegocio();
  });

  it('sin negocios no muestra el bloque: la bienvenida invita a crear el primero', async () => {
    await cuandoAbreInicio();

    expect(screen.queryByRole('region', { name: /Kiosco|Almacén/ })).toBeNull();
    expect(screen.getByRole('button', { name: /Crear con Sami/ })).toBeVisible();
  });

  it('muestra el bloque del negocio activo; sus tablas se abren desde «Mi negocio»', async () => {
    await cuandoAbreInicio(['Kiosco Centro']);

    const bloque = await bloqueDeTuNegocio();

    expect(bloque).toBeDefined();
    expect(screen.queryByRole('button', { name: /Abrir Kiosco Centro/ })).toBeNull();
    expect(screen.getByRole('button', { name: 'Mi negocio' })).toBeVisible();
  });

  it('con varios negocios sólo muestra el activo, que es el primero hasta que se elige otro', async () => {
    await cuandoAbreInicio(['Kiosco Centro', 'Almacén Norte']);

    await bloqueDeTuNegocio();

    expect(screen.queryByRole('region', { name: /Almacén Norte/ })).toBeNull();
    expect(screen.getAllByRole('region', { name: 'Tu negocio' })).toHaveLength(1);
  });

  it('«Mi negocio», en la barra, abre las tablas del negocio', async () => {
    const { router, user } = await cuandoAbreInicio(['Kiosco Centro']);
    await bloqueDeTuNegocio();

    await user.click(screen.getByRole('button', { name: 'Mi negocio' }));

    await waitFor(() => expect(router.state.location.pathname).toMatch(/^\/sistemas\//), ESPERA);
  });

  it('no ofrece crear secciones ni agregar negocios: eso queda en el selector de arriba', async () => {
    await cuandoAbreInicio(['Kiosco Centro']);
    const bloque = await bloqueDeTuNegocio();

    expect(bloque.queryByRole('button', { name: 'Crear una sección' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Agregar un negocio' })).toBeNull();
    expect(screen.queryByText(/negocios en tu cuenta/)).toBeNull();
  });
});
