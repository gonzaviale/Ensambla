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
import {
  ESPERA,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
  verifyPathAsync,
} from '@/test/testHelpers';

const sinVentas = {
  ingresos: 0,
  unidades: 0,
  cantidad: 0,
  sinPrecio: 0,
  ingresosAnteriores: 0,
  fiado: 0,
  ventasFiadas: 0,
};

/** Un cuaderno con productos pero sin ventas, sin faltantes y sin parados. */
const resumenSinNovedades = () => ({
  sistema: 'x',
  hoy: hoy(),
  hayVentas: false,
  hayProductos: true,
  ventas: { hoy: sinVentas, semana: sinVentas, mes: sinVentas },
  ventasPorDia: [],
  ultimoDiaConVentas: null,
  masVendido: null,
  faltantes: { total: 0, primeros: [] },
  parados: [],
});

const abrirInicio = async (sistemas: Sistema[]) => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  estado.sistemas.set(usuario.email, sistemas);
  const router = renderApp('/inicio');
  await verifyHeadingPresentAsync(/En qué puedo/);
  return Object.assign(userEvent.setup(), { router });
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
    // Los números llegan después del saludo: primero se espera a que estén.
    expect(
      await within(seccion).findByRole('heading', { name: 'Esta semana vendiste' }, ESPERA),
    ).toBeVisible();
    expect(
      within(seccion).getByRole('heading', { level: 1, name: 'Hola, Ana Pérez.' }),
    ).toBeVisible();
    expect(within(seccion).getByText('Almacén de barrio')).toBeVisible();
    expect(
      within(seccion).getByRole('list', { name: 'Ventas de los últimos 7 días' }),
    ).toBeVisible();
    expect(within(seccion).queryByRole('combobox')).toBeNull();
    expect(
      within(seccion).getByRole('heading', { name: 'Lo que más salió este mes' }),
    ).toBeVisible();
    expect(within(seccion).getByRole('heading', { name: 'Se te está acabando' })).toBeVisible();
  });

  it('la tarjeta de ventas arranca en la semana y deja mirar hoy o el mes', async () => {
    const user = await abrirInicio([almacen('Almacén de barrio', '2026-10-20T10:00:00.000Z')]);
    const seccion = await screen.findByRole('region', { name: 'Cómo te va' }, ESPERA);
    expect(
      await within(seccion).findByRole('button', { name: 'Esta semana' }, ESPERA),
    ).toHaveAttribute('aria-pressed', 'true');

    await user.click(within(seccion).getByRole('button', { name: 'Hoy' }));
    expect(within(seccion).getByRole('heading', { name: 'Hoy vendiste' })).toBeVisible();

    await user.click(within(seccion).getByRole('button', { name: 'Este mes' }));
    expect(within(seccion).getByRole('heading', { name: 'Este mes vendiste' })).toBeVisible();
  });

  it('con varios cuadernos deja elegir de cuál ver los números y se acuerda de la elección', async () => {
    const pedidos: string[] = [];
    server.use(
      http.get('/api/proyectos/:sistemaId/resumen/inicio', ({ params }) => {
        pedidos.push(String(params.sistemaId));
        return HttpResponse.json(resumenSinNovedades());
      }),
    );
    const reciente = almacen('Kiosco Centro', '2026-10-21T10:00:00.000Z');
    const viejo = almacen('Almacén Norte', '2026-10-01T10:00:00.000Z');
    const user = await abrirInicio([viejo, reciente]);

    const selector = await screen.findByRole('button', { name: 'Cuaderno: Kiosco Centro' }, ESPERA);
    expect(pedidos).toEqual([reciente.id]);

    await user.click(selector);
    const lista = screen.getByRole('listbox', { name: 'Cuadernos' });
    expect(within(lista).getByRole('option', { name: 'Kiosco Centro' })).toHaveAttribute(
      'aria-selected',
      'true',
    );
    await user.click(within(lista).getByRole('option', { name: 'Almacén Norte' }));

    await waitFor(() => expect(pedidos).toContain(viejo.id), ESPERA);
    expect(screen.queryByRole('listbox')).toBeNull();
    expect(screen.getByRole('button', { name: 'Cuaderno: Almacén Norte' })).toBeVisible();
    expect(window.localStorage.getItem('ensambla.inicio.cuaderno')).toBe(viejo.id);
  });

  it('la lista de cuadernos se maneja con el teclado y se cierra con Escape', async () => {
    const reciente = almacen('Kiosco Centro', '2026-10-21T10:00:00.000Z');
    const viejo = almacen('Almacén Norte', '2026-10-01T10:00:00.000Z');
    const user = await abrirInicio([viejo, reciente]);
    const selector = await screen.findByRole('button', { name: 'Cuaderno: Kiosco Centro' }, ESPERA);

    selector.focus();
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('listbox', { name: 'Cuadernos' })).toBeVisible();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('listbox')).toBeNull();
    expect(selector).toHaveFocus();

    await user.keyboard('{ArrowDown}{ArrowDown}{Enter}');

    expect(
      await screen.findByRole('button', { name: 'Cuaderno: Almacén Norte' }, ESPERA),
    ).toBeVisible();
    expect(window.localStorage.getItem('ensambla.inicio.cuaderno')).toBe(viejo.id);
  });

  it('si el servidor no contesta lo dice en lugar de mostrar ceros', async () => {
    escucharPedidos();

    await abrirInicio([almacen('Almacén de barrio', '2026-10-20T10:00:00.000Z')]);

    const seccion = await screen.findByRole('region', { name: 'Cómo te va' }, ESPERA);
    expect(
      await within(seccion).findByText(/No pudimos traer tus números/, {}, ESPERA),
    ).toBeVisible();
    expect(within(seccion).queryByRole('heading', { name: /vendiste/ })).toBeNull();
  });

  it('«Primera vez» y «Mis métricas» quedan arriba, junto al saludo', async () => {
    const user = await abrirInicio([almacen('Almacén de barrio', '2026-10-20T10:00:00.000Z')]);
    const seccion = await screen.findByRole('region', { name: 'Cómo te va' }, ESPERA);
    expect(within(seccion).getByRole('button', { name: 'Primera vez' })).toBeVisible();

    await user.click(within(seccion).getByRole('button', { name: 'Mis métricas' }));

    await verifyPathAsync(user.router, '/metricas');
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Mis métricas' }, ESPERA),
    ).toBeVisible();
  });

  it('si no falta nada dice «Tu stock está al día»', async () => {
    server.use(
      http.get('/api/proyectos/:sistemaId/resumen/inicio', () =>
        HttpResponse.json(resumenSinNovedades()),
      ),
    );

    await abrirInicio([almacen('Almacén de barrio', '2026-10-20T10:00:00.000Z')]);

    const seccion = await screen.findByRole('region', { name: 'Cómo te va' }, ESPERA);
    expect(await within(seccion).findByText('Tu stock está al día', {}, ESPERA)).toBeVisible();
    expect(within(seccion).queryByRole('heading', { name: 'Se te está acabando' })).toBeNull();
  });

  it('los productos que se acaban se destacan, cada uno en su recuadro', async () => {
    server.use(
      http.get('/api/proyectos/:sistemaId/resumen/inicio', () =>
        HttpResponse.json({
          ...resumenSinNovedades(),
          faltantes: {
            total: 2,
            primeros: [
              { nombre: 'Yerba', cantidad: 0, minimo: 5, pedir: null },
              { nombre: 'Fideos', cantidad: 3, minimo: 6, pedir: null },
            ],
          },
        }),
      ),
    );

    await abrirInicio([almacen('Almacén de barrio', '2026-10-20T10:00:00.000Z')]);

    const tarjeta = await screen.findByRole('heading', { name: 'Se te está acabando' }, ESPERA);
    const lista = within(tarjeta.closest('article') as HTMLElement).getByRole('list');
    const recuadros = within(lista).getAllByRole('listitem');
    expect(recuadros).toHaveLength(2);
    expect(recuadros[0]).toHaveTextContent('YerbaAgotado');
    expect(recuadros[1]).toHaveTextContent('FideosQuedan 3');
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
    expect(
      await within(seccion).findByRole('heading', { name: 'Se te está acabando' }, ESPERA),
    ).toBeVisible();
    expect(within(seccion).queryByRole('heading', { name: /vendiste/ })).toBeNull();
  });
});
