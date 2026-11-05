import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, http } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import { makeTemplate } from '@/lib/sistema/core';
import type { Sistema } from '@/lib/sistema/tipos';
import { estado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion, verifyHeadingPresentAsync } from '@/test/testHelpers';

const abrirInicio = async (sistemas: Sistema[]) => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  estado.sistemas.set(usuario.email, sistemas);
  renderApp('/inicio');
  await verifyHeadingPresentAsync(/En qué puedo/);
  return userEvent.setup();
};

const almacen = (nombre: string, actualizadoEn: string): Sistema => ({
  ...crearAlmacenCargado(hoy()),
  name: nombre,
  updatedAt: actualizadoEn,
});

/** Cuenta los pedidos de métricas que llegan al servidor. */
const escucharPedidos = () => {
  const ids: string[] = [];
  server.use(
    http.get('/api/proyectos/:sistemaId/resumen/inicio', ({ params }) => {
      ids.push(String(params.sistemaId));
      return HttpResponse.error();
    }),
  );
  return ids;
};

describe('inicio: cómo te va', () => {
  beforeEach(() => window.localStorage.clear());

  it('sin cuadernos no muestra métricas ni las pide', async () => {
    const pedidos = escucharPedidos();

    await abrirInicio([]);

    expect(screen.queryByRole('region', { name: 'Cómo te va' })).toBeNull();
    expect(pedidos).toEqual([]);
  });

  it('un cuaderno con sólo filas de ejemplo no muestra métricas ni las pide', async () => {
    const pedidos = escucharPedidos();

    await abrirInicio([makeTemplate('shop', {}, true)]);

    expect(screen.queryByRole('region', { name: 'Cómo te va' })).toBeNull();
    expect(pedidos).toEqual([]);
  });

  it('con un cuaderno cargado muestra lo vendido hoy, lo que más salió, lo que falta y lo parado', async () => {
    await abrirInicio([almacen('Almacén de barrio', '2026-10-20T10:00:00.000Z')]);

    const seccion = await screen.findByRole('region', { name: 'Cómo te va' }, ESPERA);
    expect(within(seccion).getByText('Almacén de barrio')).toBeVisible();
    expect(within(seccion).queryByRole('combobox')).toBeNull();
    expect(within(seccion).getByRole('heading', { name: 'Hoy vendiste' })).toBeVisible();
    expect(
      within(seccion).getByRole('heading', { name: 'Lo que más salió este mes' }),
    ).toBeVisible();
    expect(within(seccion).getByRole('heading', { name: 'Se te está acabando' })).toBeVisible();
  });

  it('con varios cuadernos deja elegir de cuál ver los números y se acuerda de la elección', async () => {
    const pedidos: string[] = [];
    server.use(
      http.get('/api/proyectos/:sistemaId/resumen/inicio', ({ params }) => {
        pedidos.push(String(params.sistemaId));
        return HttpResponse.json({
          sistema: 'x',
          hoy: hoy(),
          hayVentas: false,
          hayProductos: true,
          ventasDeHoy: { ingresos: 0, unidades: 0, cantidad: 0, sinPrecio: 0, ingresosSemanaPasada: 0 },
          ultimoDiaConVentas: null,
          masVendido: null,
          faltantes: { total: 0, primeros: [] },
          parados: [],
        });
      }),
    );
    const reciente = almacen('Kiosco Centro', '2026-10-21T10:00:00.000Z');
    const viejo = almacen('Almacén Norte', '2026-10-01T10:00:00.000Z');
    const user = await abrirInicio([viejo, reciente]);

    const selector = await screen.findByRole('combobox', { name: 'Cuaderno' }, ESPERA);
    expect(selector).toHaveValue(reciente.id);
    expect(pedidos).toEqual([reciente.id]);

    await user.selectOptions(selector, viejo.id);

    await waitFor(() => expect(pedidos).toContain(viejo.id), ESPERA);
    expect(window.localStorage.getItem('ensambla.inicio.cuaderno')).toBe(viejo.id);
  });

  it('si el servidor no contesta no muestra nada en lugar de ceros', async () => {
    escucharPedidos();

    await abrirInicio([almacen('Almacén de barrio', '2026-10-20T10:00:00.000Z')]);

    expect(await screen.findByRole('heading', { name: /Plantillas para arrancar/ })).toBeVisible();
    expect(screen.queryByRole('region', { name: 'Cómo te va' })).toBeNull();
  });

  it('sin ventas anotadas muestra sólo el stock', async () => {
    const sistema = almacen('Almacén de barrio', '2026-10-20T10:00:00.000Z');
    sistema.sections.forEach((seccion) =>
      seccion.blocks.forEach((bloque) => {
        if (bloque.type === 'sales') bloque.rows = [];
      }),
    );

    await abrirInicio([sistema]);

    const seccion = await screen.findByRole('region', { name: 'Cómo te va' }, ESPERA);
    expect(within(seccion).queryByRole('heading', { name: /Hoy/ })).toBeNull();
    expect(within(seccion).getByRole('heading', { name: 'Se te está acabando' })).toBeVisible();
  });
});
