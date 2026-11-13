import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { makeTemplate } from '@/lib/sistema/core';
import { estado } from '@/mocks/db/estado';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
  verifyPathAsync,
} from '@/test/testHelpers';

const abrirInicio = async (nombres: string[] = []) => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  if (nombres.length) {
    estado.sistemas.set(
      usuario.email,
      nombres.map((nombre) => {
        const sistema = makeTemplate('shop', {}, true);
        sistema.name = nombre;
        return sistema;
      }),
    );
  }
  const router = renderApp('/inicio');
  await verifyHeadingPresentAsync(/En qué puedo/);
  return { usuario, router, user: userEvent.setup() };
};

const negocio = async () =>
  within(await screen.findByRole('region', { name: /Negocio de Ana Pérez/ }, ESPERA));

describe('inicio · tu negocio', () => {
  it('sin secciones no muestra el negocio: la bienvenida invita a crear la primera', async () => {
    await abrirInicio();

    expect(screen.queryByRole('region', { name: /Negocio de Ana Pérez/ })).toBeNull();
    expect(screen.getByRole('button', { name: /Crear con Sami/ })).toBeVisible();
  });

  it('muestra el nombre del negocio y cuántas secciones tiene', async () => {
    await abrirInicio(['Ventas', 'Stock']);

    const bloque = await negocio();

    expect(bloque.getByRole('heading', { level: 2, name: 'Negocio de Ana Pérez' })).toBeVisible();
    expect(bloque.getByText(/2 secciones/)).toBeVisible();
  });

  it('con una sola sección lo dice en singular', async () => {
    await abrirInicio(['Ventas']);

    expect((await negocio()).getByText(/1 sección\b/)).toBeVisible();
  });

  it('muestra cada sección como un acceso directo y abre la que se toca', async () => {
    const { router, user } = await abrirInicio(['Ventas', 'Stock']);
    const lista = within(
      await screen.findByRole('list', { name: 'Secciones de tu negocio' }, ESPERA),
    );

    expect(lista.getAllByRole('button')).toHaveLength(2);
    await user.click(lista.getByRole('button', { name: /^Stock/ }));

    await waitFor(() => expect(router.state.location.pathname).toMatch(/^\/sistemas\//), ESPERA);
  });

  it('el botón «Crear una sección» ofrece las tres maneras de empezar y se vuelve a cerrar', async () => {
    const { user } = await abrirInicio(['Ventas']);
    const bloque = await negocio();
    const abrir = bloque.getByRole('button', { name: 'Crear una sección' });
    expect(abrir).toHaveAttribute('aria-expanded', 'false');
    expect(bloque.queryByRole('button', { name: /Crear con Sami/ })).toBeNull();

    await user.click(abrir);

    expect(bloque.getByRole('button', { name: 'Cerrar' })).toHaveAttribute('aria-expanded', 'true');
    expect(bloque.getByRole('button', { name: /Crear con Sami/ })).toBeVisible();
    expect(bloque.getByRole('button', { name: /Elegir una plantilla/ })).toBeVisible();
    expect(bloque.getByRole('button', { name: /Empezar de cero/ })).toBeVisible();

    await user.click(bloque.getByRole('button', { name: 'Cerrar' }));

    expect(bloque.queryByRole('button', { name: /Crear con Sami/ })).toBeNull();
  });

  it('desde «Crear una sección» se puede elegir una plantilla', async () => {
    const { router, user } = await abrirInicio(['Ventas']);
    const bloque = await negocio();

    await user.click(bloque.getByRole('button', { name: 'Crear una sección' }));
    await user.click(bloque.getByRole('button', { name: /Elegir una plantilla/ }));

    await verifyPathAsync(router, '/plantillas');
  });
});
