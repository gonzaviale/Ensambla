import { describe, expect, it } from 'vitest';
import { iniciarSesion, obtenerUsuarioActual, registrarUsuario } from '@/api/auth';
import { ApiError } from '@/api/apiError';
import { setupServerError, setupUsuarioConSesion } from '@/test/testHelpers';

describe('auth', () => {
  it('devuelve null cuando no hay sesión (401 es un estado, no un error)', async () => {
    const usuario = await obtenerUsuarioActual();

    expect(usuario).toBeNull();
  });

  it('devuelve el usuario de la sesión abierta', async () => {
    setupUsuarioConSesion({ nombre: 'Lucía Gómez' });

    const usuario = await obtenerUsuarioActual();

    expect(usuario).toMatchObject({ nombre: 'Lucía Gómez' });
  });

  it('propaga los errores que no son de sesión', async () => {
    setupServerError('get', '/auth/me', 500, 'Ocurrió un error interno en el servidor');

    await expect(obtenerUsuarioActual()).rejects.toBeInstanceOf(ApiError);
  });

  it('registra una cuenta y después permite iniciar sesión', async () => {
    const datos = {
      nombre: 'Lucía Gómez',
      nombreNegocio: 'Velas Lumbre',
      email: 'lucia@example.com',
      contrasenia: 'una-contrasenia-segura',
    };

    await registrarUsuario(datos);
    const usuario = await iniciarSesion({ email: datos.email, contrasenia: datos.contrasenia });

    expect(usuario.email).toBe('lucia@example.com');
  });

  it('rechaza un email ya registrado con 409', async () => {
    setupUsuarioConSesion({ email: 'lucia@example.com' });

    const error = await registrarUsuario({
      nombre: 'Otra',
      nombreNegocio: 'Otro negocio',
      email: 'lucia@example.com',
      contrasenia: 'una-contrasenia-segura',
    }).catch((e: unknown) => e);

    expect(error).toMatchObject({ status: 409 });
  });

  it('rechaza credenciales incorrectas con 401', async () => {
    setupUsuarioConSesion({ email: 'lucia@example.com' });

    const error = await iniciarSesion({
      email: 'lucia@example.com',
      contrasenia: 'incorrecta',
    }).catch((e: unknown) => e);

    expect(error).toMatchObject({ status: 401 });
  });
});
