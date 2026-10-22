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

    await verifyPathAsync(router, '/inicio');
    await verifyHeadingPresentAsync(/En qué puedo/);
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

  // El cierre de sesión vive en el perfil del encabezado: sus pruebas se suman cuando esa pantalla esté.

  it('verifica la sesión en el servidor aunque el cliente hubiera cacheado un usuario', async () => {
    setupUsuarioConSesion();
    const router = renderApp('/inicio');
    await verifyHeadingPresentAsync(/En qué puedo/);
    estado.sesionUsuarioId = null;

    await router.navigate({ to: '/inicio' });

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
