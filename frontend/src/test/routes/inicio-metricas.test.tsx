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

/** Una sección con productos pero sin ventas, sin faltantes y sin parados. */
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

  it('sin secciones no muestra métricas ni las pide', async () => {
    const pedidos = escucharPedidos();

    await abrirInicio([]);

    expect(screen.queryByRole('region', { name: 'Cómo te va' })).toBeNull();
    expect(pedidos).toEqual([]);
  });

  it('una sección con sólo filas de ejemplo no muestra métricas ni las pide', async () => {
    const pedidos = escucharPedidos();

    await abrirInicio([makeTemplate('shop', {}, true)]);

    expect(screen.queryByRole('region', { name: 'Cómo te va' })).toBeNull();
    expect(pedidos).toEqual([]);
  });

  it('con una sección cargada muestra lo vendido y lo parado, sin lo que más salió ni lo que falta', async () => {
    await abrirInicio([almacen('Almacén de barrio', '2026-10-20T10:00:00.000Z')]);

    const seccion = await screen.findByRole('region', { name: 'Cómo te va' }, ESPERA);
    // Los números llegan después del saludo: primero se espera a que estén.
    expect(
      await within(seccion).findByRole('heading', { name: 'Esta semana vendiste' }, ESPERA),
    ).toBeVisible();
    expect(within(seccion).queryByRole('heading', { level: 1 })).toBeNull();
    expect(within(seccion).getByText('Almacén de barrio')).toBeVisible();
    expect(
      within(seccion).getByRole('list', { name: 'Ventas de los últimos 7 días' }),
    ).toBeVisible();
    expect(within(seccion).queryByRole('combobox')).toBeNull();
    expect(
      within(seccion).queryByRole('heading', { name: 'Sin venderse hace 2 semanas' }),
    ).toBeNull();
    expect(
      within(seccion).queryByRole('heading', { name: 'Lo que más salió este mes' }),
    ).toBeNull();
    expect(within(seccion).queryByRole('heading', { name: 'Se te está acabando' })).toBeNull();
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

  it('con varias secciones deja elegir de cuál ver los números y se acuerda de la elección', async () => {
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

    const selector = await screen.findByRole('button', { name: 'Sección: Kiosco Centro' }, ESPERA);
    expect(pedidos).toEqual([reciente.id]);

    await user.click(selector);
    const lista = screen.getByRole('listbox', { name: 'Secciones' });
    expect(within(lista).getByRole('option', { name: 'Kiosco Centro' })).toHaveAttribute(
      'aria-selected',
      'true',
    );
    await user.click(within(lista).getByRole('option', { name: 'Almacén Norte' }));

    await waitFor(() => expect(pedidos).toContain(viejo.id), ESPERA);
    expect(screen.queryByRole('listbox')).toBeNull();
    expect(screen.getByRole('button', { name: 'Sección: Almacén Norte' })).toBeVisible();
    expect(window.localStorage.getItem('ensambla.inicio.sistema')).toBe(viejo.id);
  });

  it('la lista de secciones se maneja con el teclado y se cierra con Escape', async () => {
    const reciente = almacen('Kiosco Centro', '2026-10-21T10:00:00.000Z');
    const viejo = almacen('Almacén Norte', '2026-10-01T10:00:00.000Z');
    const user = await abrirInicio([viejo, reciente]);
    const selector = await screen.findByRole('button', { name: 'Sección: Kiosco Centro' }, ESPERA);

    selector.focus();
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('listbox', { name: 'Secciones' })).toBeVisible();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('listbox')).toBeNull();
    expect(selector).toHaveFocus();

    await user.keyboard('{ArrowDown}{ArrowDown}{Enter}');

    expect(
      await screen.findByRole('button', { name: 'Sección: Almacén Norte' }, ESPERA),
    ).toBeVisible();
    expect(window.localStorage.getItem('ensambla.inicio.sistema')).toBe(viejo.id);
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

  it('el saludo y «Primera vez» van en la bienvenida compacta, no en el tablero, y no hay acceso a «Mis métricas»', async () => {
    await abrirInicio([almacen('Almacén de barrio', '2026-10-20T10:00:00.000Z')]);
    const seccion = await screen.findByRole('region', { name: 'Cómo te va' }, ESPERA);
    expect(within(seccion).queryByRole('button', { name: /Primera vez/ })).toBeNull();
    const bienvenida = screen.getByRole('region', { name: 'Empezar algo nuevo' });
    expect(within(bienvenida).getByRole('button', { name: /Primera vez/ })).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Mis métricas' })).toBeNull();
  });

  it('sin ventas anotadas no muestra la tarjeta de ventas', async () => {
    const sistema = almacen('Almacén de barrio', '2026-10-20T10:00:00.000Z');
    sistema.sections.forEach((seccion) =>
      seccion.blocks.forEach((bloque) => {
        if (bloque.type === 'sales') bloque.rows = [];
      }),
    );

    await abrirInicio([sistema]);

    const seccion = await screen.findByRole('region', { name: 'Cómo te va' }, ESPERA);
    expect(
      await within(seccion).findByRole('article', { name: 'Aviso económico' }, ESPERA),
    ).toBeVisible();
    expect(within(seccion).queryByRole('heading', { name: /vendiste/ })).toBeNull();
  });
});

describe('inicio: aviso económico', () => {
  beforeEach(() => window.localStorage.clear());

  it('muestra el aviso de la semana con su prioridad y sus números', async () => {
    await abrirInicio([almacen('Almacén de barrio', '2026-10-20T10:00:00.000Z')]);

    const tarjeta = await screen.findByRole('article', { name: 'Aviso económico' }, ESPERA);
    expect(
      within(tarjeta).getByRole('heading', {
        name: 'Las ventas subieron 12 %, pero la ganancia bajó 4 %',
      }),
    ).toBeVisible();
    expect(within(tarjeta).getByText('Prioridad alta')).toBeVisible();
    expect(within(tarjeta).getByText('Ventas').nextSibling).toHaveTextContent('+12 %');
    expect(within(tarjeta).getByText('Ganancia').nextSibling).toHaveTextContent('−4 %');
    expect(within(tarjeta).getByText('Productos involucrados').nextSibling).toHaveTextContent('3');
    expect(within(tarjeta).queryByText('Confianza')).toBeNull();
  });

  it('cuando el negocio mejora lo dice y ofrece ver los objetivos', async () => {
    server.use(
      http.get('/api/proyectos/:sistemaId/resumen/aviso-economico', () =>
        HttpResponse.json({
          aviso: {
            prioridad: 'BAJA',
            tendencia: 'MEJORA',
            categoria: 'Ventas',
            titulo: 'Las ventas subieron 20 % frente a la semana pasada',
            explicacion: 'Tu negocio está mejorando: vendiste más que la semana pasada.',
            variacionVentas: 20,
            variacionResultadoBruto: null,
            productos: ['Yerba'],
            confianza: 'BAJA',
          },
        }),
      ),
    );
    const user = await abrirInicio([almacen('Almacén de barrio', '2026-10-20T10:00:00.000Z')]);

    const tarjeta = await screen.findByRole('article', { name: 'Aviso económico' }, ESPERA);
    expect(within(tarjeta).getByText('Vas mejorando')).toBeVisible();
    expect(within(tarjeta).getByText('Ganancia').nextSibling).toHaveTextContent('Sin costos');
    expect(within(tarjeta).queryByRole('button', { name: 'Probar una solución' })).toBeNull();

    await user.click(within(tarjeta).getByRole('button', { name: 'Ver mis objetivos' }));

    await verifyPathAsync(user.router, '/objetivos');
  });

  it('sin una semana anterior para comparar no muestra el aviso', async () => {
    server.use(
      http.get('/api/proyectos/:sistemaId/resumen/aviso-economico', () =>
        HttpResponse.json({ aviso: null }),
      ),
    );
    await abrirInicio([almacen('Almacén de barrio', '2026-10-20T10:00:00.000Z')]);

    const seccion = await screen.findByRole('region', { name: 'Cómo te va' }, ESPERA);
    await within(seccion).findByRole('heading', { name: /vendiste/ }, ESPERA);
    expect(within(seccion).queryByRole('article', { name: 'Aviso económico' })).toBeNull();
  });

  it('«Productos involucrados» abre un modal con la lista y se cierra', async () => {
    const user = await abrirInicio([almacen('Almacén de barrio', '2026-10-20T10:00:00.000Z')]);
    const tarjeta = await screen.findByRole('article', { name: 'Aviso económico' }, ESPERA);
    expect(within(tarjeta).queryByRole('list', { name: 'Productos involucrados' })).toBeNull();

    await user.click(within(tarjeta).getByRole('button', { name: /Productos involucrados|3/ }));
    const modal = await screen.findByRole('dialog');
    const lista = within(modal).getByRole('list', { name: 'Productos involucrados' });
    expect(
      within(lista)
        .getAllByRole('listitem')
        .map((item) => item.textContent),
    ).toEqual(['Aceite', 'Leche', 'Yerba']);

    await user.click(within(modal).getByRole('button', { name: 'Listo' }));
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it.each(['Entender por qué', 'Probar una solución'])('«%s» lleva a Objetivos', async (boton) => {
    const user = await abrirInicio([almacen('Almacén de barrio', '2026-10-20T10:00:00.000Z')]);
    const tarjeta = await screen.findByRole('article', { name: 'Aviso económico' }, ESPERA);

    await user.click(within(tarjeta).getByRole('button', { name: boton }));

    await verifyPathAsync(user.router, '/objetivos');
  });
});
