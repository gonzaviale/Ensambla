import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import { makeTemplate } from '@/lib/sistema/core';
import type { Sistema } from '@/lib/sistema/tipos';
import { estado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  clickButton,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
  verifyPathAsync,
} from '@/test/testHelpers';

const abrirInicio = async (sistemas: Sistema[]) => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  estado.sistemas.set(usuario.email, sistemas);
  const router = renderApp('/inicio');
  await verifyHeadingPresentAsync(/En qué puedo/);
  return { router, user: userEvent.setup(), sistema: sistemas[0] };
};

const titulo = () => screen.queryByRole('heading', { name: 'Cómo viene tu negocio' });
/** El título aparece mientras carga: lo que hay que esperar son las cifras. */
const cifrasAsync = () => screen.findByText('Vendiste', undefined, ESPERA);

describe('inicio: panel del negocio', () => {
  it('con ventas muestra las métricas y el flujo de caja de los últimos 30 días', async () => {
    await abrirInicio([crearAlmacenCargado(hoy())]);

    await cifrasAsync();
    const panel = screen.getByRole('region', { name: 'Cómo viene tu negocio' });
    for (const etiqueta of ['Vendiste', 'Te queda', 'Te deben', 'Margen']) {
      expect(within(panel).getByText(etiqueta)).toBeVisible();
    }
    expect(within(panel).getByText(/Los últimos 30 días/)).toBeVisible();
    expect(within(panel).getByText(/Lo fiado no entra hasta que lo cobrás/)).toBeVisible();
  });

  it('el flujo de caja es una fila por semana con lo que entró, salió y dejó, escrito', async () => {
    await abrirInicio([crearAlmacenCargado(hoy())]);
    await cifrasAsync();

    const semanas = within(screen.getByRole('list', { name: 'Flujo de caja por semana' }));
    const filas = semanas.getAllByRole('listitem');

    expect(filas).toHaveLength(5);
    for (const fila of filas) {
      expect(within(fila).getByText('Entró')).toBeVisible();
      expect(within(fila).getByText('Salió')).toBeVisible();
      expect(within(fila).getByText('Dejó')).toBeVisible();
      expect(fila).toHaveTextContent(/\$\d/);
    }
    expect(screen.getByText('Te va quedando')).toBeVisible();
  });

  it('cada métrica explica para qué sirve y lleva a su detalle', async () => {
    const { user, router, sistema } = await abrirInicio([crearAlmacenCargado(hoy())]);
    await cifrasAsync();
    const tarjeta = (clave: string) => {
      const encontrada = document.querySelector<HTMLElement>(`[data-metrica="${clave}"]`);
      if (!encontrada) throw new Error(`Falta la tarjeta ${clave}`);
      return encontrada;
    };

    expect(within(tarjeta('teDeben')).getByText(/Lo que vendiste fiado/)).toBeVisible();
    await user.click(within(tarjeta('vendiste')).getByRole('button', { name: /Ver mis ventas/ }));
    await verifyPathAsync(router, `/sistemas/${sistema?.id}`);
  });

  it('«Ver mis objetivos» lleva a Objetivos', async () => {
    const { user, router } = await abrirInicio([crearAlmacenCargado(hoy())]);
    await cifrasAsync();

    const queda = document.querySelector<HTMLElement>('[data-metrica="queda"]');
    if (!queda) throw new Error('Falta la tarjeta de lo que te queda');
    await user.click(within(queda).getByRole('button', { name: /Ver mis objetivos/ }));

    await verifyPathAsync(router, '/objetivos');
  });

  it('si el primer cuaderno no tiene ventas, mira el que sí las tiene', async () => {
    await abrirInicio([makeTemplate('shop', {}, true), crearAlmacenCargado(hoy())]);

    await cifrasAsync();

    expect(screen.queryByText(/Todavía no cargaste ventas/)).toBeNull();
  });

  it('sin cuadernos no aparece: la home queda como siempre', async () => {
    await abrirInicio([]);

    expect(titulo()).toBeNull();
  });

  it('con un cuaderno sin ventas no inventa números y explica cómo cargarlas', async () => {
    const { user, router, sistema } = await abrirInicio([makeTemplate('shop', {}, true)]);

    expect(await screen.findByText(/Todavía no cargaste ventas/, undefined, ESPERA)).toBeVisible();
    expect(screen.queryByText('Vendiste')).toBeNull();
    expect(screen.queryByRole('img', { name: /Flujo de caja/ })).toBeNull();

    await clickButton(user, /Cargar mis ventas/);

    await verifyPathAsync(router, `/sistemas/${sistema?.id}`);
  });

  it('si el servidor falla avisa y permite reintentar', async () => {
    server.use(
      http.get(
        '*/api/negocio/panel',
        () => HttpResponse.json({ message: 'Error' }, { status: 500 }),
        {
          once: true,
        },
      ),
    );
    const { user } = await abrirInicio([crearAlmacenCargado(hoy())]);

    expect(await screen.findByRole('alert', undefined, ESPERA)).toHaveTextContent(
      /No pudimos traer tus números/,
    );
    await clickButton(user, 'Probar de nuevo');

    expect(await screen.findByText('Vendiste', undefined, ESPERA)).toBeVisible();
    expect(screen.queryByRole('alert')).toBeNull();
  });
});
