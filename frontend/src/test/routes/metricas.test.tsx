import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, http } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import type { Sistema } from '@/lib/sistema/tipos';
import { estado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
  verifyPathAsync,
} from '@/test/testHelpers';

const almacen = (): Sistema => ({ ...crearAlmacenCargado(hoy()), name: 'Almacén de barrio' });

const abrirMetricas = async (sistemas: Sistema[]) => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  estado.sistemas.set(usuario.email, sistemas);
  const router = renderApp('/metricas');
  await screen.findByRole('heading', { level: 1, name: 'Mis métricas' }, ESPERA);
  return { router, user: userEvent.setup() };
};

describe('mis métricas', () => {
  beforeEach(() => window.localStorage.clear());

  it('se abre desde el navbar y queda marcada', async () => {
    const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
    estado.sistemas.set(usuario.email, [almacen()]);
    const router = renderApp('/inicio');
    await verifyHeadingPresentAsync(/En qué puedo/);
    const user = userEvent.setup();
    const nav = screen.getByRole('navigation', { name: 'Principal' });

    await user.click(within(nav).getByRole('button', { name: 'Mis métricas' }));

    await verifyPathAsync(router, '/metricas');
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Mis métricas' }, ESPERA),
    ).toBeVisible();
    expect(
      within(screen.getByRole('navigation', { name: 'Principal' })).getByRole('button', {
        name: 'Mis métricas',
      }),
    ).toHaveClass('active');
  });

  it('arranca en la semana con el resumen, el gráfico día por día y lo más vendido', async () => {
    await abrirMetricas([almacen()]);

    const resumen = await screen.findByRole('region', { name: 'Resumen' }, ESPERA);
    expect(within(resumen).getByRole('heading', { name: 'Esta semana vendiste' })).toBeVisible();
    expect(within(resumen).getByRole('heading', { name: 'Ventas anotadas' })).toBeVisible();
    expect(within(resumen).getByRole('heading', { name: 'Por día, en promedio' })).toBeVisible();
    const grafico = screen.getByRole('region', { name: 'Día por día' });
    expect(within(grafico).getAllByRole('listitem')).toHaveLength(7);
    expect(screen.getByRole('region', { name: 'Lo que más se vendió' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Esta semana' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
  });

  it('deja mirar sólo hoy o el mes completo', async () => {
    const { user } = await abrirMetricas([almacen()]);
    await screen.findByRole('region', { name: 'Resumen' }, ESPERA);

    await user.click(screen.getByRole('button', { name: 'Este mes' }));
    expect(await screen.findByRole('heading', { name: 'Este mes vendiste' }, ESPERA)).toBeVisible();
    expect(
      within(screen.getByRole('region', { name: 'Día por día' })).getAllByRole('listitem'),
    ).toHaveLength(30);

    await user.click(screen.getByRole('button', { name: 'Hoy' }));
    expect(await screen.findByRole('heading', { name: 'Hoy vendiste' }, ESPERA)).toBeVisible();
    expect(screen.queryByRole('region', { name: 'Día por día' })).toBeNull();
    expect(screen.queryByRole('heading', { name: 'Por día, en promedio' })).toBeNull();
  });

  it('sin cuadernos cargados explica qué hacer en lugar de mostrar ceros', async () => {
    await abrirMetricas([]);

    expect(
      screen.getByRole('heading', { name: 'Todavía no hay números para mostrar' }),
    ).toBeVisible();
    expect(screen.getByRole('button', { name: 'Ir a mis cuadernos' })).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Esta semana' })).toBeNull();
  });

  it('si el servidor no contesta lo dice y deja reintentar', async () => {
    server.use(http.get('/api/proyectos/:sistemaId/resumen/metricas', () => HttpResponse.error()));

    await abrirMetricas([almacen()]);

    expect(await screen.findByText(/No pudimos traer tus métricas/, {}, ESPERA)).toBeVisible();
    expect(screen.getByRole('button', { name: 'Probar de nuevo' })).toBeVisible();
  });
});
