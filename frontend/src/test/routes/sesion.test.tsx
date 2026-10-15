import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { renderApp } from '@/test/renderApp';
import {
  clickButton,
  setupServerError,
  setupUsuarioConSesion,
  setupUsuarioSinSesion,
  typeLabel,
  verifyAlertPresentAsync,
  verifyHeadingPresentAsync,
  verifyPathAsync,
  verifyTextPresentAsync,
} from '@/test/testHelpers';
import { estado } from '@/mocks/db/estado';
import { usuarioSchema } from '@/types/auth';

const EMAIL = 'brian-sesion@example.com';

async function completarRegistro() {
  const user = userEvent.setup();
  await verifyHeadingPresentAsync('Creá tu cuenta');
  await typeLabel(user, 'Tu nombre', 'Brian');
  await typeLabel(user, 'Nombre del negocio', 'Mi negocio');
  await typeLabel(user, 'Email', EMAIL);
  await typeLabel(user, 'Contraseña', 'ClaveSegura123!');
  await clickButton(user, 'Crear cuenta');
}

describe('registro y sesión del cliente', () => {
  it('registra usuario y negocio, inicia sesión y muestra el espacio privado', async () => {
    const router = renderApp('/registro');

    await completarRegistro();

    await verifyPathAsync(router, '/bienvenida');
    expect(estado.usuarios.get(EMAIL)?.nombre).toBe('Brian');
    expect(localStorage.length).toBe(0);
  });

  it('rechaza un email repetido con el mensaje del servidor', async () => {
    setupUsuarioSinSesion({ email: EMAIL });
    const router = renderApp('/registro');

    await completarRegistro();

    await verifyAlertPresentAsync('Ya existe una cuenta con ese email');
    expect(router.state.location.pathname).toBe('/registro');
  });

  it('valida campos obligatorios antes de enviar el registro', async () => {
    const user = userEvent.setup();
    renderApp('/registro');
    await verifyHeadingPresentAsync('Creá tu cuenta');

    await clickButton(user, 'Crear cuenta');

    await verifyTextPresentAsync('Ingresá tu nombre');
    await verifyTextPresentAsync('Ingresá el nombre de tu negocio');
    await verifyTextPresentAsync('Ingresá tu email');
    await verifyTextPresentAsync('Usá al menos 8 caracteres');
    expect(estado.usuarios.size).toBe(0);
  });

  it('muestra los errores del servidor junto al campo del negocio', async () => {
    setupServerError('post', '/auth/register', 400, 'Revisá los datos enviados', {
      nombreNegocio: 'El nombre del negocio no es válido',
    });
    renderApp('/registro');

    await completarRegistro();

    await verifyTextPresentAsync('El nombre del negocio no es válido');
    await verifyAlertPresentAsync('Revisá los datos enviados');
    expect(screen.getByLabelText('Nombre del negocio')).toHaveAttribute('aria-invalid', 'true');
  });

  it('logout invalida la sesión y vuelve a login; una URL privada vuelve a rechazar el acceso', async () => {
    const user = userEvent.setup();
    setupUsuarioConSesion();
    const router = renderApp('/proyectos');
    await verifyHeadingPresentAsync('¿Por dónde querés empezar?');

    await clickButton(user, 'Cerrar sesión');

    await verifyPathAsync(router, '/login');
    await verifyHeadingPresentAsync('Ingresá a tu cuenta');
    expect(estado.sesionUsuarioId).toBeNull();
    await router.navigate({
      to: '/proyectos/$proyectoId/construir',
      params: { proyectoId: 'abc' },
    });
    await verifyPathAsync(router, '/login');
  });

  it('si falla el logout informa el error y mantiene visible la sesión activa', async () => {
    const user = userEvent.setup();
    setupUsuarioConSesion();
    setupServerError('post', '/auth/logout', 500, 'No se pudo cerrar la sesión');
    const router = renderApp('/proyectos');
    await verifyHeadingPresentAsync('¿Por dónde querés empezar?');

    await clickButton(user, 'Cerrar sesión');

    await verifyAlertPresentAsync('No pudimos cerrar sesión. Volvé a intentarlo.');
    expect(router.state.location.pathname).toBe('/proyectos');
    expect(estado.sesionUsuarioId).not.toBeNull();
  });

  it('verifica la sesión en el servidor aunque el cliente hubiera cacheado un usuario', async () => {
    setupUsuarioConSesion();
    const router = renderApp('/proyectos');
    await verifyHeadingPresentAsync('¿Por dónde querés empezar?');
    estado.sesionUsuarioId = null;

    await router.navigate({ to: '/proyectos/$proyectoId/usar', params: { proyectoId: 'abc' } });

    await verifyPathAsync(router, '/login');
    await verifyHeadingPresentAsync('Ingresá a tu cuenta');
  });

  it('el contrato de usuario solo contiene datos públicos', () => {
    const response = usuarioSchema.parse({
      id: 'id',
      nombre: 'Brian',
      email: EMAIL,
      hashContrasenia: 'no-se-expone',
    });

    expect(response).toEqual({ id: 'id', nombre: 'Brian', email: EMAIL });
  });
});
