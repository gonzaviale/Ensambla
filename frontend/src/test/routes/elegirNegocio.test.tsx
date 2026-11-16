import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { guardarNegocioActivo, leerNegocioGuardado } from '@/lib/negocios/lista';
import type { Sistema } from '@/lib/sistema/tipos';
import { renderApp } from '@/test/renderApp';
import {
  ESPERA,
  negocioDePrueba,
  setupNegocios,
  setupServerError,
  setupUsuarioSinSesion,
  typeLabel,
  verifyPathAsync,
} from '@/test/testHelpers';

const USUARIO = { email: 'dos-negocios@example.com', contrasenia: 'una-contrasenia-segura' };

const ALMACEN = negocioDePrueba('10000000-0000-4000-8000-000000000001', 'Almacén San Justo');
const KIOSCO = negocioDePrueba('20000000-0000-4000-8000-000000000002', 'Kiosco de la estación');

const givenNegocios = (...negocios: Sistema[]) => {
  setupUsuarioSinSesion(USUARIO);
  setupNegocios(USUARIO.email, ...negocios);
};

const whenIniciaSesion = async (ruta = '/login') => {
  const router = renderApp(ruta);
  const user = userEvent.setup();
  await screen.findByRole('heading', { name: 'Ingresá a tu cuenta' }, ESPERA);
  await typeLabel(user, 'Email', USUARIO.email);
  await typeLabel(user, 'Contraseña', USUARIO.contrasenia);
  await user.click(screen.getByRole('button', { name: 'Ingresar' }));
  return { router, user };
};

/** Cada ícono del pop-up apunta a un dibujo que está en la página: si no, se ve el recuadro vacío. */
const thenLosIconosSeDibujan = (dialogo: HTMLElement) => {
  const usados = [...dialogo.querySelectorAll('use')].map((uso) => uso.getAttribute('href'));
  expect(usados.length).toBeGreaterThan(0);
  usados.forEach((href) => expect(document.querySelector(`symbol${href}`)).not.toBeNull());
};

const popUp = () => screen.findByRole('dialog', { name: '¿Con qué negocio trabajás hoy?' }, ESPERA);

describe('elegir con qué negocio trabajar al iniciar sesión', () => {
  it('con un solo negocio entra directo, sin preguntar', async () => {
    givenNegocios(ALMACEN);

    const { router } = await whenIniciaSesion();

    await verifyPathAsync(router, '/inicio');
    expect(screen.queryByRole('dialog', { name: '¿Con qué negocio trabajás hoy?' })).toBeNull();
  });

  it('con más de uno pregunta antes de entrar y recuerda el elegido', async () => {
    givenNegocios(ALMACEN, KIOSCO);

    const { router, user } = await whenIniciaSesion();
    const dialogo = await popUp();
    expect(dialogo).toHaveTextContent('Tenés 2 negocios');
    expect(router.state.location.pathname).toBe('/elegir-negocio');
    await user.click(
      await within(dialogo).findByRole('button', { name: 'Kiosco de la estación' }, ESPERA),
    );

    await verifyPathAsync(router, '/inicio');
    expect(leerNegocioGuardado()).toBe(KIOSCO.id);
    expect(
      await screen.findByRole('button', { name: /Negocio activo.*Kiosco de la estación/ }, ESPERA),
    ).toBeVisible();
  });

  it('señala el último negocio que se usó en este navegador', async () => {
    givenNegocios(ALMACEN, KIOSCO);
    guardarNegocioActivo(KIOSCO.id);

    await whenIniciaSesion();
    const dialogo = await popUp();

    expect(
      await within(dialogo).findByRole('button', { name: 'Kiosco de la estación' }, ESPERA),
    ).toHaveAccessibleDescription(/El último que usaste/);
    thenLosIconosSeDibujan(dialogo);
    expect(within(dialogo).queryByRole('button', { name: /Seguir con/ })).toBeNull();
  });

  it('tocar el último que se usó entra sin cambiar de negocio', async () => {
    givenNegocios(ALMACEN, KIOSCO);

    const { router, user } = await whenIniciaSesion();
    const dialogo = await popUp();
    await user.click(
      await within(dialogo).findByRole('button', { name: 'Almacén San Justo' }, ESPERA),
    );

    await verifyPathAsync(router, '/inicio');
    expect(
      await screen.findByRole('button', { name: /Negocio activo.*Almacén San Justo/ }, ESPERA),
    ).toBeVisible();
  });

  it('Esc no hace nada: hay que elegir un negocio', async () => {
    givenNegocios(ALMACEN, KIOSCO);
    const { router, user } = await whenIniciaSesion();
    await popUp();

    await user.keyboard('{Escape}');

    expect(router.state.location.pathname).toBe('/elegir-negocio');
    expect(screen.getByRole('dialog')).toBeVisible();
  });

  it('después de elegir va a donde se quería ir antes de ingresar', async () => {
    givenNegocios(ALMACEN, KIOSCO);

    const { router, user } = await whenIniciaSesion('/login?redirect=%2Fplanes');
    const dialogo = await popUp();
    await user.click(
      await within(dialogo).findByRole('button', { name: 'Almacén San Justo' }, ESPERA),
    );

    await verifyPathAsync(router, '/planes');
  });

  it('si no llegan los cuadernos de la cuenta no pregunta nada', async () => {
    givenNegocios(ALMACEN, KIOSCO);
    setupServerError('get', '/sistemas', 500, 'Error interno');

    const { router } = await whenIniciaSesion();

    await verifyPathAsync(router, '/inicio');
    expect(screen.queryByRole('dialog', { name: '¿Con qué negocio trabajás hoy?' })).toBeNull();
  });
});
