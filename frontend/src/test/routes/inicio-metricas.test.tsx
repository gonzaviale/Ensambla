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

  it('con varios negocios muestra los números del activo y no deja elegir otro acá', async () => {
    const pedidos: string[] = [];
    server.use(
      http.get('/api/proyectos/:sistemaId/resumen/inicio', ({ params }) => {
        pedidos.push(String(params.sistemaId));
        return HttpResponse.json(resumenSinNovedades());
      }),
    );
    const activo = almacen('Almacén Norte', '2026-10-01T10:00:00.000Z');
    const otro = almacen('Kiosco Centro', '2026-10-21T10:00:00.000Z');

    await abrirInicio([activo, otro]);

    await waitFor(() => expect(pedidos).toEqual([activo.id]), ESPERA);
    expect(screen.queryByRole('button', { name: /^Sección:/ })).toBeNull();
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

  it('sin ventas anotadas no muestra métricas: se ve el inicio común', async () => {
    const sistema = almacen('Almacén de barrio', '2026-10-20T10:00:00.000Z');
    sistema.sections.forEach((seccion) =>
      seccion.blocks.forEach((bloque) => {
        if (bloque.type === 'sales') bloque.rows = [];
      }),
    );

    await abrirInicio([sistema]);

    expect(
      await screen.findByText(/Tocá una ficha para arrancar/, undefined, ESPERA),
    ).toBeVisible();
    expect(screen.queryByRole('region', { name: 'Cómo te va' })).toBeNull();
  });
});

describe('inicio: carrusel de rentabilidad', () => {
  beforeEach(() => window.localStorage.clear());

  const tarjetas = async () =>
    within(
      await screen.findByRole('region', { name: 'Avisos de rentabilidad' }, ESPERA),
    ).getAllByRole('article', { name: 'Aviso económico' });

  it('parte el aviso en tarjetas cortas: ventas, ganancia y productos', async () => {
    await abrirInicio([almacen('Almacén de barrio', '2026-10-20T10:00:00.000Z')]);

    const [ventas, ganancia, productos] = await tarjetas();
    expect(within(ventas as HTMLElement).getByText('Ventas')).toBeVisible();
    expect(within(ventas as HTMLElement).getByText('+12 %')).toBeVisible();
    expect(within(ganancia as HTMLElement).getByText('Ganancia')).toBeVisible();
    expect(within(ganancia as HTMLElement).getByText('−4 %')).toBeVisible();
    expect(within(productos as HTMLElement).queryByText('Aceite')).toBeNull();

    await userEvent.click(
      within(productos as HTMLElement).getByRole('button', {
        name: /Productos que te dejaron menos/,
      }),
    );
    const lista = await screen.findByRole('list', { name: 'Productos involucrados' });
    const filas = within(lista).getAllByRole('listitem');
    expect(filas.map((item) => item.textContent)).toEqual([
      expect.stringContaining('Aceite'),
      expect.stringContaining('Leche'),
      expect.stringContaining('Yerba'),
    ]);
    expect(filas[0]).toHaveTextContent(/Te dejó \$\s?4\.200 menos de ganancia/);
  });

  it('no manda a ninguna otra pantalla ni abre ventanas solas', async () => {
    await abrirInicio([almacen('Almacén de barrio', '2026-10-20T10:00:00.000Z')]);
    await tarjetas();

    expect(screen.queryByRole('button', { name: 'Entender por qué' })).toBeNull();
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('sin costos cargados lo dice en la tarjeta de ganancia', async () => {
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
            cambios: [{ nombre: 'Yerba', monto: 800 }],
            confianza: 'BAJA',
          },
        }),
      ),
    );
    await abrirInicio([almacen('Almacén de barrio', '2026-10-20T10:00:00.000Z')]);

    const [ventas, costos] = await tarjetas();
    expect(within(ventas as HTMLElement).getByText('+20 %')).toBeVisible();
    expect(within(costos as HTMLElement).getByText('Sin costos')).toBeVisible();
  });

  it('sin una semana anterior para comparar no muestra el carrusel', async () => {
    server.use(
      http.get('/api/proyectos/:sistemaId/resumen/aviso-economico', () =>
        HttpResponse.json({ aviso: null }),
      ),
    );
    await abrirInicio([almacen('Almacén de barrio', '2026-10-20T10:00:00.000Z')]);

    const seccion = await screen.findByRole('region', { name: 'Cómo te va' }, ESPERA);
    await within(seccion).findByRole('heading', { name: /vendiste/ }, ESPERA);
    expect(screen.queryByRole('region', { name: 'Avisos de rentabilidad' })).toBeNull();
  });

  it('cada tarjeta se descarta sola y no vuelve a aparecer', async () => {
    const user = await abrirInicio([almacen('Almacén de barrio', '2026-10-20T10:00:00.000Z')]);
    const [ventas] = await tarjetas();

    await user.click(
      within(ventas as HTMLElement).getByRole('button', { name: 'Descartar aviso' }),
    );

    const quedan = await tarjetas();
    expect(quedan).toHaveLength(2);
    expect(screen.queryByText('+12 %')).toBeNull();
    expect(window.localStorage.getItem('ensambla.inicio.rentabilidadDescartada')).toContain(
      ':ventas',
    );
  });

  it('con un solo negocio muestra solo sus tarjetas, no una por negocio', async () => {
    await abrirInicio([
      almacen('Almacén de barrio', '2026-10-20T10:00:00.000Z'),
      almacen('Kiosco del centro', '2026-10-21T10:00:00.000Z'),
    ]);

    expect(await tarjetas()).toHaveLength(3);
  });
});
