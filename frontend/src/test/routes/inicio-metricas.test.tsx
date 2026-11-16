import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, http } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import { makeTemplate } from '@/lib/sistema/core';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import type { Sistema } from '@/lib/sistema/tipos';
import { estado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { renderApp } from '@/test/renderApp';
import { ESPERA, setupUsuarioConSesion, verifyHeadingPresentAsync } from '@/test/testHelpers';

const almacen = (nombre: string): Sistema => ({ ...crearAlmacenCargado(hoy()), name: nombre });

const givenNegocioSinVentas = (): Sistema => ({
  ...makeTemplate('shop', {}, true),
  name: 'Kiosco nuevo',
});

const whenAbreInicio = async (sistemas: Sistema[]) => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  estado.sistemas.set(usuario.email, sistemas);
  const router = renderApp('/inicio');
  await verifyHeadingPresentAsync(/En qué puedo/);
  return Object.assign(userEvent.setup(), { router });
};

const sinVentas = {
  ingresos: 0,
  unidades: 0,
  cantidad: 0,
  sinPrecio: 0,
  ingresosAnteriores: 0,
  fiado: 0,
  ventasFiadas: 0,
};

/** Una sola venta, hoy: sin semana anterior ni otro día con qué compararla. */
const givenUnaSolaVentaHoy = () => {
  const venta = { ...sinVentas, ingresos: 1500, unidades: 1, cantidad: 1 };
  server.use(
    http.get('/api/proyectos/:sistemaId/resumen/inicio', () =>
      HttpResponse.json({
        sistema: 'x',
        hoy: hoy(),
        hayVentas: true,
        hayProductos: true,
        ventas: { hoy: venta, semana: venta, mes: venta },
        ventasPorDia: [{ fecha: hoy(), ingresos: 1500 }],
        ultimoDiaConVentas: { fecha: hoy(), ingresos: 1500 },
        masVendido: null,
        faltantes: { total: 0, primeros: [] },
        parados: [],
      }),
    ),
  );
};

const givenSinAvisoEconomico = () =>
  server.use(
    http.get('/api/proyectos/:sistemaId/resumen/aviso-economico', () =>
      HttpResponse.json({ aviso: null }),
    ),
  );

const givenAvisoSinCostos = () =>
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

const thenTarjetas = async () =>
  within(
    await screen.findByRole('region', { name: 'Avisos de rentabilidad' }, ESPERA),
  ).getAllByRole('article', { name: 'Aviso económico' });

const thenNoHayCarrusel = async () => {
  await screen.findByRole('region', { name: 'Tu flujo de caja' }, ESPERA);
  expect(screen.queryByRole('region', { name: 'Avisos de rentabilidad' })).toBeNull();
};

describe('inicio: sin tablero ni decisiones', () => {
  beforeEach(() => window.localStorage.clear());

  it('con una sección cargada no muestra «Cómo te va» ni «Para decidir hoy»', async () => {
    await whenAbreInicio([almacen('Almacén de barrio')]);

    await thenTarjetas();
    expect(screen.queryByText('Cómo te va')).toBeNull();
    expect(screen.queryByText('Para decidir hoy')).toBeNull();
  });
});

describe('inicio: métricas', () => {
  beforeEach(() => window.localStorage.clear());

  it('dentro de «Tu negocio» muestra lo vendido junto al cuaderno, sin elegir sección', async () => {
    await whenAbreInicio([almacen('Almacén de barrio')]);

    const negocio = within(screen.getByRole('region', { name: 'Almacén de barrio' }));
    const metricas = await negocio.findByRole('region', { name: 'Tus métricas' }, ESPERA);
    expect(
      await within(metricas).findByRole('heading', { name: 'Esta semana vendiste' }, ESPERA),
    ).toBeVisible();
    expect(negocio.getByRole('button', { name: /Abrir Almacén de barrio/ })).toBeVisible();
    expect(screen.queryByText('Así viene')).toBeNull();
    expect(screen.queryByText('Cómo te va')).toBeNull();
  });

  it('con un negocio sin ventas muestra qué se va a ver, junto al cuaderno', async () => {
    await whenAbreInicio([givenNegocioSinVentas()]);

    const negocio = within(await screen.findByRole('region', { name: 'Kiosco nuevo' }, ESPERA));
    expect(
      await negocio.findByRole('heading', { name: 'Acá vas a ver tus ventas' }, ESPERA),
    ).toBeVisible();
    expect(negocio.getByRole('button', { name: /Abrir Kiosco nuevo/ })).toBeVisible();
  });

  it('sin ventas, «Cargar mis primeras ventas» lleva al negocio para anotarlas', async () => {
    const user = await whenAbreInicio([givenNegocioSinVentas()]);

    await user.click(
      await screen.findByRole('button', { name: 'Cargar mis primeras ventas' }, ESPERA),
    );

    await waitFor(
      () => expect(user.router.state.location.pathname).toMatch(/^\/sistemas\//),
      ESPERA,
    );
  });
});

describe('inicio: carrusel de rentabilidad', () => {
  beforeEach(() => window.localStorage.clear());

  it('con dos semanas de ventas aparece debajo de «Tu negocio»', async () => {
    await whenAbreInicio([almacen('Almacén de barrio')]);

    await thenTarjetas();
    const negocio = screen.getByRole('heading', { level: 2, name: 'Almacén de barrio' });
    const carrusel = screen.getByRole('region', { name: 'Avisos de rentabilidad' });
    expect(
      negocio.compareDocumentPosition(carrusel) & Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy();
  });

  it('con una sola venta en un solo día no aparece', async () => {
    givenUnaSolaVentaHoy();
    await whenAbreInicio([almacen('Almacén de barrio')]);

    await thenNoHayCarrusel();
  });

  it('parte el aviso en tarjetas cortas: ventas, ganancia y productos', async () => {
    const user = await whenAbreInicio([almacen('Almacén de barrio')]);

    const [ventas, ganancia, productos] = await thenTarjetas();
    expect(within(ventas as HTMLElement).getByText('Ventas')).toBeVisible();
    expect(within(ventas as HTMLElement).getByText('+12 %')).toBeVisible();
    expect(within(ganancia as HTMLElement).getByText('Ganancia')).toBeVisible();
    expect(within(ganancia as HTMLElement).getByText('−4 %')).toBeVisible();
    expect(within(productos as HTMLElement).queryByText('Aceite')).toBeNull();

    await user.click(
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
    await whenAbreInicio([almacen('Almacén de barrio')]);
    await thenTarjetas();

    expect(screen.queryByRole('button', { name: 'Entender por qué' })).toBeNull();
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('sin costos cargados lo dice en la tarjeta de ganancia', async () => {
    givenAvisoSinCostos();
    await whenAbreInicio([almacen('Almacén de barrio')]);

    const [ventas, costos] = await thenTarjetas();
    expect(within(ventas as HTMLElement).getByText('+20 %')).toBeVisible();
    expect(within(costos as HTMLElement).getByText('Sin costos')).toBeVisible();
  });

  it('sin una semana anterior para comparar no aparece', async () => {
    givenSinAvisoEconomico();
    await whenAbreInicio([almacen('Almacén de barrio')]);

    await thenNoHayCarrusel();
  });

  it('cada tarjeta se descarta sola y no vuelve a aparecer', async () => {
    const user = await whenAbreInicio([almacen('Almacén de barrio')]);
    const [ventas] = await thenTarjetas();

    await user.click(
      within(ventas as HTMLElement).getByRole('button', { name: 'Descartar aviso' }),
    );

    expect(await thenTarjetas()).toHaveLength(2);
    expect(screen.queryByText('+12 %')).toBeNull();
    expect(window.localStorage.getItem('ensambla.inicio.rentabilidadDescartada')).toContain(
      ':ventas',
    );
  });

  it('con varias secciones muestra solo las tarjetas de una', async () => {
    await whenAbreInicio([almacen('Almacén de barrio'), almacen('Kiosco del centro')]);

    expect(await thenTarjetas()).toHaveLength(3);
  });
});
