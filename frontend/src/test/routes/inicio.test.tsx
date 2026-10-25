import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { makeTemplate } from '@/lib/sistema/core';
import { estado } from '@/mocks/db/estado';
import { useSistemaStore } from '@/stores/sistemaStore';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  verifyToastAsync,
  clickButton,
  setupUsuarioConSesion,
  verifyHeadingPresentAsync,
  verifyPathAsync,
} from '@/test/testHelpers';

const abrirInicio = async (sistemas = false) => {
  const usuario = setupUsuarioConSesion({ nombre: 'Ana Pérez' });
  if (sistemas) estado.sistemas.set(usuario.email, [makeTemplate('shop', {}, true)]);
  const router = renderApp('/inicio');
  await verifyHeadingPresentAsync(/En qué puedo/);
  return { usuario, router, user: userEvent.setup() };
};

describe('inicio', () => {
  it('saluda con el nombre de la cuenta y muestra las tres maneras de empezar', async () => {
    await abrirInicio();

    expect(screen.getByText('Hola, Ana Pérez.')).toBeVisible();
    expect(screen.getByRole('button', { name: /Crear con Sami/ })).toBeVisible();
    expect(screen.getByRole('button', { name: /Elegir una plantilla/ })).toBeVisible();
    expect(screen.getByRole('button', { name: /Empezar de cero/ })).toBeVisible();
    expect(screen.queryByRole('heading', { name: 'Seguí donde lo dejaste' })).toBeNull();
  });

  // La sección «Así se ve un sistema» está comentada en InicioPage: se retoma si vuelve.
  it.skip('cambia la distribución del ejemplo entre mosaico y una columna', async () => {
    const { user } = await abrirInicio();
    const bloques = () => document.querySelectorAll('.hero-system .preview-block');

    expect([...bloques()].some((bloque) => bloque.getAttribute('data-width') === 'half')).toBe(
      true,
    );
    await clickButton(user, /Una columna/);

    expect([...bloques()].every((bloque) => bloque.getAttribute('data-width') === 'full')).toBe(
      true,
    );
  });

  it('lista los sistemas guardados y abre uno', async () => {
    const { user, router } = await abrirInicio(true);

    await user.click(await screen.findByRole('button', { name: /Mi almacén/ }, ESPERA));

    await verifyPathAsync(router, `/sistemas/${useSistemaStore.getState().activeId}`);
    expect(useSistemaStore.getState().modo).toBe('use');
  });

  it('«Mis cuadernos» sin sistemas explica por dónde empezar', async () => {
    const { user } = await abrirInicio();

    await clickButton(user, 'Mis cuadernos');

    await verifyToastAsync(/tres caminos/);
  });

  it('muestra la zona sin mapa y lleva a «Mi zona»', async () => {
    const { user, router } = await abrirInicio();

    expect(document.querySelector('.leaflet-container')).toBeNull();
    await clickButton(user, /Ver mi zona/);

    await verifyPathAsync(router, '/zona');
  });
});

describe('plantillas', () => {
  it('abre la vista previa, elige opciones y crea el sistema con ejemplos', async () => {
    const { user, router } = await abrirInicio();
    const tarjeta = document.querySelector<HTMLElement>('[data-template="business"]');
    if (!tarjeta) throw new Error('Falta la tarjeta de la plantilla');

    await user.click(within(tarjeta).getByRole('button', { name: /Ver el sistema/ }));
    const modal = await screen.findByRole('dialog');
    expect(
      within(modal).getByRole('heading', { name: /Pedidos y clientes, a tu manera/ }),
    ).toBeVisible();
    await user.click(within(modal).getByLabelText(/Entradas y salidas/));
    await user.click(within(modal).getByRole('button', { name: /Empezar con este conjunto/ }));

    await waitFor(() => expect(useSistemaStore.getState().sistemas).toHaveLength(1), ESPERA);
    const [sistema] = useSistemaStore.getState().sistemas;
    expect(sistema?.sections[0]?.blocks.map((bloque) => bloque.type)).toContain('entries');
    expect(useSistemaStore.getState().listo).toBe(true);
    expect(screen.queryByRole('dialog')).toBeNull();
    await verifyPathAsync(router, `/sistemas/${sistema?.id}`);
  });

  it('filtra por tipo y busca por texto', async () => {
    const { user } = await abrirInicio();
    await clickButton(user, 'Plantillas');
    await verifyHeadingPresentAsync(/Elegí una base/);
    const visibles = () => document.querySelectorAll('[data-template]:not([hidden])').length;
    expect(visibles()).toBe(3);

    await clickButton(user, 'Stock y movimientos');
    expect(visibles()).toBe(1);

    await clickButton(user, 'Todas');
    await user.type(screen.getByLabelText('Buscar una plantilla'), 'kiosco');
    expect(visibles()).toBe(1);

    await user.clear(screen.getByLabelText('Buscar una plantilla'));
    await user.type(screen.getByLabelText('Buscar una plantilla'), 'turnos');
    expect(visibles()).toBe(0);
    expect(screen.getByText(/Probá con Almacén/)).toBeVisible();
  });
});

describe('modales', () => {
  it('se cierran con Esc y devuelven el foco', async () => {
    const { user } = await abrirInicio();
    const boton = screen.getByRole('button', { name: /¿Necesitás una mano\?/ });

    await user.click(boton);
    expect(await screen.findByRole('dialog')).toBeVisible();
    expect(document.body.style.overflow).toBe('hidden');
    await user.keyboard('{Escape}');

    expect(screen.queryByRole('dialog')).toBeNull();
    expect(document.body.style.overflow).toBe('');
    expect(boton).toHaveFocus();
  });

  it('el lienzo en blanco crea un sistema vacío en modo Construir', async () => {
    const { user } = await abrirInicio();

    await clickButton(user, /Empezar de cero/);
    await user.click(await screen.findByRole('button', { name: /Seguir desde cero/ }));

    await waitFor(() => expect(useSistemaStore.getState().sistemas).toHaveLength(1), ESPERA);
    expect(useSistemaStore.getState().modo).toBe('build');
  });

  it('el avatar lleva a la cuenta, donde se cambia el nombre que se muestra y se guarda la preferencia', async () => {
    const { user, usuario, router } = await abrirInicio();

    await clickButton(user, /Tu cuenta y configuración/);
    await verifyPathAsync(router, '/cuenta');
    const campo = await screen.findByLabelText('¿Cómo querés que te llame Sami?');
    await user.clear(campo);
    await user.type(campo, 'Marta');
    await clickButton(user, 'Guardar nombre');

    await verifyToastAsync('Guardamos tu nombre.');
    expect(await screen.findByText('Marta')).toBeVisible();
    await waitFor(
      () => expect(estado.preferencias.get(usuario.email)?.nombre).toBe('Marta'),
      ESPERA,
    );
  });

  it('desde la cuenta se puede cerrar la sesión', async () => {
    const { user, router } = await abrirInicio();

    await clickButton(user, /Tu cuenta y configuración/);
    await user.click(await screen.findByRole('link', { name: 'Sesión' }));
    await user.click(await screen.findByRole('button', { name: 'Cerrar sesión' }));

    await verifyPathAsync(router, '/login');
  });
});
