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

  it('el flujo de caja es una fila por semana con lo que entró, salió y el saldo, escrito', async () => {
    await abrirInicio([crearAlmacenCargado(hoy())]);
    await cifrasAsync();

    const semanas = within(screen.getByRole('list', { name: 'Flujo de caja por semana' }));
    const filas = semanas.getAllByRole('listitem');

    expect(filas).toHaveLength(5);
    for (const fila of filas) {
      expect(within(fila).getByText('Entró')).toBeVisible();
      expect(within(fila).getByText('Salió')).toBeVisible();
      expect(within(fila).getByText('Saldo')).toBeVisible();
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

  it('el margen usa la coma decimal y cada acceso a un detalle tiene su propio nombre', async () => {
    await abrirInicio([crearAlmacenCargado(hoy())]);
    await cifrasAsync();

    expect(
      document.querySelector('[data-metrica="margen"] .panel-tarjeta-valor'),
    ).toHaveTextContent(/^\d+(,\d)? %$/);
    const tarjetas = document.querySelector<HTMLElement>('.panel-tarjetas');
    if (!tarjetas) throw new Error('Faltan las tarjetas');
    const accesos = within(tarjetas)
      .getAllByRole('button')
      .map((boton) => boton.textContent);
    expect(new Set(accesos).size).toBe(accesos.length);
  });

  it('con panel el saludo sigue siendo el h1 y el título del panel es un h2', async () => {
    await abrirInicio([crearAlmacenCargado(hoy())]);
    await cifrasAsync();

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(/En qué puedo/);
    expect(screen.getByRole('heading', { level: 2, name: 'Cómo viene tu negocio' })).toBeVisible();
  });

  it('quien ya tiene un cuaderno ve una bienvenida compacta con las tres formas de empezar', async () => {
    await abrirInicio([crearAlmacenCargado(hoy())]);
    await cifrasAsync();

    const bienvenida = screen.getByRole('region', { name: 'Empezar algo nuevo' });
    expect(within(bienvenida).getByText('Hola, Ana Pérez.')).toBeVisible();
    expect(within(bienvenida).getByRole('button', { name: /Crear con Sami/ })).toBeVisible();
    expect(within(bienvenida).getByRole('button', { name: /Elegir una plantilla/ })).toBeVisible();
    expect(within(bienvenida).getByRole('button', { name: /Empezar de cero/ })).toBeVisible();
    expect(within(bienvenida).getByRole('button', { name: /Primera vez/ })).toBeVisible();
    expect(screen.queryByText(/Tocá una ficha para arrancar/)).toBeNull();
  });

  it('desde la bienvenida compacta se sigue pudiendo crear: «Elegir una plantilla» lleva a Plantillas', async () => {
    const { user, router } = await abrirInicio([crearAlmacenCargado(hoy())]);
    await cifrasAsync();

    await user.click(
      within(screen.getByRole('region', { name: 'Empezar algo nuevo' })).getByRole('button', {
        name: /Elegir una plantilla/,
      }),
    );

    await verifyPathAsync(router, '/plantillas');
  });

  it('«Cargar desde una foto» y «Ver mi zona» van en la franja de bienvenida, una sola vez', async () => {
    await abrirInicio([crearAlmacenCargado(hoy())]);
    await cifrasAsync();

    const bienvenida = within(screen.getByRole('region', { name: 'Empezar algo nuevo' }));
    expect(bienvenida.getByRole('button', { name: /Cargar desde una foto/ })).toBeVisible();
    expect(bienvenida.getByRole('button', { name: /Ver mi zona/ })).toBeVisible();
    expect(screen.getAllByRole('button', { name: /Cargar desde una foto/ })).toHaveLength(1);
    expect(screen.getAllByRole('button', { name: /Ver mi zona/ })).toHaveLength(1);
    expect(screen.getAllByRole('region', { name: 'Más para tu negocio' })).toHaveLength(1);
    expect(document.querySelector('.inicio-atajos.compactos')).not.toBeNull();
  });

  it('desde la franja, «Ver mi zona» lleva a Mi zona', async () => {
    const { user, router } = await abrirInicio([crearAlmacenCargado(hoy())]);
    await cifrasAsync();

    await user.click(
      within(screen.getByRole('region', { name: 'Empezar algo nuevo' })).getByRole('button', {
        name: /Ver mi zona/,
      }),
    );

    await verifyPathAsync(router, '/zona');
  });

  it('quien recién empieza sigue viendo los dos atajos con su explicación', async () => {
    await abrirInicio([]);

    const atajos = within(screen.getByRole('region', { name: 'Más para tu negocio' }));
    expect(atajos.getByText(/Sacale una foto a tu cuaderno/)).toBeVisible();
    expect(atajos.getByRole('button', { name: /Ver mi zona/ })).toBeVisible();
    expect(document.querySelector('.inicio-atajos.compactos')).toBeNull();
  });

  it('quien recién empieza ve la bienvenida grande, sin franja compacta ni panel', async () => {
    await abrirInicio([]);

    expect(screen.getByText(/Tocá una ficha para arrancar/)).toBeVisible();
    expect(screen.queryByRole('region', { name: 'Empezar algo nuevo' })).toBeNull();
    expect(titulo()).toBeNull();
  });

  it('sin cuadernos el saludo sigue siendo el h1', async () => {
    await abrirInicio([]);

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(/En qué puedo/);
  });

  it('mientras llegan los números avisa que está haciendo las cuentas', async () => {
    await abrirInicio([crearAlmacenCargado(hoy())]);

    const panel = screen.getByRole('region', { name: 'Cómo viene tu negocio' });
    expect(within(panel).getByRole('status')).toHaveTextContent('Estamos haciendo tus cuentas');
    await cifrasAsync();
    expect(screen.queryByText('Estamos haciendo tus cuentas…')).toBeNull();
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
