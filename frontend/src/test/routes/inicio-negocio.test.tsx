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

const givenNegocios = (email: string, nombres: string[]) => {
  if (nombres.length) estado.sistemas.set(email, nombres.map(cuadernoLlamado));
};

const whenAbreInicio = async (nombres: string[] = []) => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  givenNegocios(usuario.email, nombres);
  const router = renderApp('/inicio');
  await verifyHeadingPresentAsync(/En qué puedo/);
  return { router, user: userEvent.setup() };
};

const bloqueDe = async (nombre: string) =>
  within(await screen.findByRole('region', { name: nombre }, ESPERA));

describe('inicio · tu negocio', () => {
  it('sin negocios no muestra el bloque: la bienvenida invita a crear el primero', async () => {
    await whenAbreInicio();

    expect(screen.queryByRole('button', { name: /^Abrir .*\d+ tablas?$/ })).toBeNull();
    expect(screen.getByRole('button', { name: /Crear con Sami/ })).toBeVisible();
  });

  it('muestra el nombre del negocio activo y cuántas tablas tiene', async () => {
    await whenAbreInicio(['Kiosco Centro']);

    const bloque = await bloqueDe('Kiosco Centro');

    expect(bloque.getByRole('heading', { level: 2, name: 'Kiosco Centro' })).toBeVisible();
    expect(bloque.getAllByText(/\d+ tablas?/).length).toBeGreaterThan(0);
  });

  it('con varios negocios sólo muestra el activo, que es el primero hasta que se elige otro', async () => {
    await whenAbreInicio(['Kiosco Centro', 'Almacén Norte']);

    const bloque = await bloqueDe('Kiosco Centro');

    expect(bloque.getByRole('button', { name: /Abrir Kiosco Centro/ })).toBeVisible();
    expect(screen.queryByRole('button', { name: /Abrir Almacén Norte/ })).toBeNull();
  });

  it('el acceso abre el cuaderno del negocio', async () => {
    const { router, user } = await whenAbreInicio(['Kiosco Centro']);
    const bloque = await bloqueDe('Kiosco Centro');

    await user.click(bloque.getByRole('button', { name: /Abrir Kiosco Centro/ }));

    await waitFor(() => expect(router.state.location.pathname).toMatch(/^\/sistemas\//), ESPERA);
  });

  it('no ofrece crear secciones: otro negocio se agrega desde «Agregar un negocio»', async () => {
    await whenAbreInicio(['Kiosco Centro']);
    const bloque = await bloqueDe('Kiosco Centro');

    expect(bloque.queryByRole('button', { name: 'Crear una sección' })).toBeNull();
    expect(screen.getByRole('button', { name: 'Agregar un negocio' })).toBeVisible();
  });
});
