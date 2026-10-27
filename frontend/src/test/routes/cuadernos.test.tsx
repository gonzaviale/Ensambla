import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { makeTemplate } from '@/lib/sistema/core';
import { estado } from '@/mocks/db/estado';
import { useSistemaStore } from '@/stores/sistemaStore';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion, verifyPathAsync } from '@/test/testHelpers';

const abrirCuadernos = async (cantidad = 0) => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  if (cantidad) {
    estado.sistemas.set(
      usuario.email,
      Array.from({ length: cantidad }, (_, indice) => {
        const sistema = makeTemplate('shop', {}, true);
        sistema.name = indice ? `Cuaderno ${indice + 1}` : 'Mi almacén';
        return sistema;
      }),
    );
  }
  const router = renderApp('/cuadernos');
  await screen.findByRole('heading', { level: 1, name: 'Mis cuadernos' }, ESPERA);
  return { router, user: userEvent.setup() };
};

describe('mis cuadernos', () => {
  it('sin cuadernos invita a crear el primero con las tres maneras de empezar', async () => {
    await abrirCuadernos();

    expect(screen.getByRole('heading', { name: 'Todavía no tenés cuadernos' })).toBeVisible();
    expect(screen.getByRole('button', { name: /Crear con Sami/ })).toBeVisible();
    expect(screen.getByRole('button', { name: /Elegir una plantilla/ })).toBeVisible();
    expect(screen.getByRole('button', { name: /Empezar de cero/ })).toBeVisible();
  });

  it('marca «Mis cuadernos» en el navbar', async () => {
    await abrirCuadernos();
    const nav = screen.getByRole('navigation', { name: 'Principal' });
    expect(within(nav).getByRole('button', { name: 'Mis cuadernos' })).toHaveClass('active');
  });

  it('lista los cuadernos guardados y abre uno', async () => {
    const { user, router } = await abrirCuadernos(1);

    await user.click(await screen.findByRole('button', { name: /^Mi almacén/ }, ESPERA));

    await verifyPathAsync(router, `/sistemas/${useSistemaStore.getState().activeId}`);
    expect(useSistemaStore.getState().modo).toBe('use');
  });

  it('con cuadernos ofrece crear otro', async () => {
    await abrirCuadernos(1);
    await screen.findByRole('button', { name: /^Mi almacén/ }, ESPERA);
    expect(screen.queryByRole('heading', { name: 'Todavía no tenés cuadernos' })).toBeNull();
    expect(screen.getByRole('heading', { name: 'Crear otro cuaderno' })).toBeVisible();
    expect(screen.queryByText(/registros/)).toBeNull();
  });

  it('con muchos cuadernos deja buscarlos por nombre', async () => {
    const { user } = await abrirCuadernos(4);
    await screen.findByRole('button', { name: /^Mi almacén/ }, ESPERA);

    await user.type(screen.getByRole('searchbox', { name: 'Buscar un cuaderno' }), 'cuaderno 3');

    expect(screen.getByRole('button', { name: /^Cuaderno 3/ })).toBeVisible();
    expect(screen.queryByRole('button', { name: /^Mi almacén/ })).toBeNull();
  });
});
