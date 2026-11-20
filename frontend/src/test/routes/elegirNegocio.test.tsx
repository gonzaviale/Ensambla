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

const dadoNegocios = (...negocios: Sistema[]) => {
  setupUsuarioSinSesion(USUARIO);
  setupNegocios(USUARIO.email, ...negocios);
};

const cuandoIniciaSesion = async (ruta = '/login') => {
  const router = renderApp(ruta);
  const user = userEvent.setup();
  await screen.findByRole('heading', { name: 'Ingresá a tu cuenta' }, ESPERA);
  await typeLabel(user, 'Email', USUARIO.email);
  await typeLabel(user, 'Contraseña', USUARIO.contrasenia);
  await user.click(screen.getByRole('button', { name: 'Ingresar' }));
  return { router, user };
};

const popUp = () => screen.findByRole('dialog', { name: '¿Con qué negocio trabajás hoy?' }, ESPERA);

describe('elegir con qué negocio trabajar al iniciar sesión', () => {
  it('con un solo negocio entra directo, sin preguntar', async () => {
    dadoNegocios(ALMACEN);

    const { router } = await cuandoIniciaSesion();

    await verifyPathAsync(router, '/inicio');
    expect(screen.queryByRole('dialog', { name: '¿Con qué negocio trabajás hoy?' })).toBeNull();
  });

  it('con más de uno pregunta antes de entrar y recuerda el elegido', async () => {
    dadoNegocios(ALMACEN, KIOSCO);

    const { router, user } = await cuandoIniciaSesion();
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
    dadoNegocios(ALMACEN, KIOSCO);
    guardarNegocioActivo(KIOSCO.id);

    await cuandoIniciaSesion();
    const dialogo = await popUp();

    expect(
      await within(dialogo).findByRole('button', { name: 'Kiosco de la estación' }, ESPERA),
    ).toHaveAccessibleDescription(/El último que usaste/);
    expect(
      within(dialogo).getByRole('button', { name: 'Seguir con Kiosco de la estación' }),
    ).toBeVisible();
  });

  it('«Seguir con…» entra con el último que se usó, sin cambiar de negocio', async () => {
    dadoNegocios(ALMACEN, KIOSCO);

    const { router, user } = await cuandoIniciaSesion();
    const dialogo = await popUp();
    await user.click(within(dialogo).getByRole('button', { name: 'Seguir con Almacén San Justo' }));

    await verifyPathAsync(router, '/inicio');
    expect(
      await screen.findByRole('button', { name: /Negocio activo.*Almacén San Justo/ }, ESPERA),
    ).toBeVisible();
  });

  it('Esc no hace nada: hay que elegir un negocio', async () => {
    dadoNegocios(ALMACEN, KIOSCO);
    const { router, user } = await cuandoIniciaSesion();
    await popUp();

    await user.keyboard('{Escape}');

    expect(router.state.location.pathname).toBe('/elegir-negocio');
    expect(screen.getByRole('dialog')).toBeVisible();
  });

  it('después de elegir va a donde se quería ir antes de ingresar', async () => {
    dadoNegocios(ALMACEN, KIOSCO);

    const { router, user } = await cuandoIniciaSesion('/login?redirect=%2Fplanes');
    const dialogo = await popUp();
    await user.click(within(dialogo).getByRole('button', { name: 'Seguir con Almacén San Justo' }));

    await verifyPathAsync(router, '/planes');
  });

  it('si no llegan los cuadernos de la cuenta no pregunta nada', async () => {
    dadoNegocios(ALMACEN, KIOSCO);
    setupServerError('get', '/sistemas', 500, 'Error interno');

    const { router } = await cuandoIniciaSesion();

    await verifyPathAsync(router, '/inicio');
    expect(screen.queryByRole('dialog', { name: '¿Con qué negocio trabajás hoy?' })).toBeNull();
  });
});
