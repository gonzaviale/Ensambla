import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, http } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import type { Sistema } from '@/lib/sistema/tipos';
import { estado } from '@/mocks/db/estado';
import { metricasDe } from '@/mocks/handlers/reportes';
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
    expect(within(resumen).getByRole('heading', { name: 'Unidades vendidas' })).toBeVisible();
    expect(within(resumen).getByRole('heading', { name: 'Por día, en promedio' })).toBeVisible();
    const grafico = screen.getByRole('region', { name: 'Día por día' });
    expect(within(grafico).getAllByRole('listitem')).toHaveLength(7);
    expect(screen.getByRole('region', { name: 'Lo que más se vendió' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Esta semana' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
  });

  it('muestra el mismo detalle que el PDF: ingresos y ganancia de cada producto y el valor del stock', async () => {
    await abrirMetricas([almacen()]);

    const resumen = await screen.findByRole('region', { name: 'Resumen' }, ESPERA);
    expect(within(resumen).getByRole('heading', { name: 'Valor del stock' })).toBeVisible();
    const panel = screen.getByRole('region', { name: 'Lo que más se vendió' });
    const tabla = within(panel).getByRole('table', { name: 'Lo que más se vendió' });
    expect(
      within(tabla)
        .getAllByRole('columnheader')
        .map((columna) => columna.textContent),
    ).toEqual(['Producto', 'Unidades', 'Ingresos', 'Ganancia']);
    const primera = within(tabla).getAllByRole('row')[1];
    expect(primera).toBeDefined();
    expect(within(primera as HTMLElement).getAllByRole('cell')[1]).toHaveTextContent('$');
  });

  it('muestra los avisos de Sami pendientes del cuaderno y lo que se hizo con ellos', async () => {
    const sistema = almacen();
    const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
    estado.sistemas.set(usuario.email, [sistema]);
    const aviso = (
      id: string,
      proyectoId: string | null,
      tipo: 'STOCK' | 'COMBO',
      est: 'PENDIENTE' | 'APLICADO',
    ) => ({
      id,
      proyectoId,
      tipo,
      grupo: tipo === 'STOCK' ? ('STOCK' as const) : ('VENTAS' as const),
      prioridad: 1,
      estado: est,
      entidad: 'Yerba',
      titulo: 'Aviso',
      detalle: 'Detalle',
      motivos: [],
      acciones: [],
      creadoEn: new Date().toISOString(),
      resueltoEn: est === 'APLICADO' ? new Date().toISOString() : null,
    });
    estado.avisosDelServidor.set(usuario.email, [
      aviso('a1', sistema.id, 'STOCK', 'PENDIENTE'),
      aviso('a2', sistema.id, 'STOCK', 'PENDIENTE'),
      aviso('a3', sistema.id, 'COMBO', 'PENDIENTE'),
      aviso('a4', sistema.id, 'STOCK', 'APLICADO'),
      aviso('a5', 'otro-cuaderno', 'COMBO', 'PENDIENTE'),
    ]);
    renderApp('/metricas');

    const panel = await screen.findByRole('region', { name: 'Avisos de Sami' }, ESPERA);

    expect(panel).toHaveTextContent('3 pendientes');
    expect(panel).toHaveTextContent('aplicaste 1 y descartaste 0');
    const filas = within(within(panel).getByRole('table')).getAllByRole('row');
    expect(filas[1]).toHaveTextContent('Stock2');
    expect(filas[2]).toHaveTextContent('Combos1');
  });

  it('sin avisos pendientes lo dice', async () => {
    await abrirMetricas([almacen()]);

    const panel = await screen.findByRole('region', { name: 'Avisos de Sami' }, ESPERA);

    expect(within(panel).getByText('No tenés avisos pendientes.')).toBeVisible();
  });

  it('muestra los precios frente a la zona cuando el servidor tiene rangos comparables', async () => {
    const sistema = almacen();
    server.use(
      http.get('/api/proyectos/:sistemaId/resumen/metricas', () =>
        HttpResponse.json({
          ...metricasDe(sistema, 'SEMANA', hoy()),
          zona: {
            nombre: 'San Justo',
            comercios: 8,
            debajo: 1,
            dentro: 2,
            encima: 0,
            desalineados: [
              { nombre: 'Yerba', precio: 3400, minimo: 3500, maximo: 4800, porcentaje: -3 },
            ],
          },
        }),
      ),
    );
    await abrirMetricas([sistema]);

    const panel = await screen.findByRole(
      'region',
      { name: 'Tus precios frente a la zona (San Justo)' },
      ESPERA,
    );

    expect(panel).toHaveTextContent('1 por debajo del rango, 2 dentro y 0 por encima');
    expect(panel).toHaveTextContent('datos agregados y anónimos de 8 comercios');
    const fila = within(within(panel).getByRole('table')).getAllByRole('row')[1];
    expect(fila).toHaveTextContent('Yerba');
    expect(fila).toHaveTextContent('-3 %');
  });

  it('sin zona no muestra la sección de precios frente a la zona', async () => {
    await abrirMetricas([almacen()]);
    await screen.findByRole('region', { name: 'Resumen' }, ESPERA);

    expect(screen.queryByRole('region', { name: /frente a la zona/ })).toBeNull();
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
