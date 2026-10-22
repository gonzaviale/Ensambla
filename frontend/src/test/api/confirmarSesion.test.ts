import { describe, expect, it } from 'vitest';
import { sesionExpiradaConfirmada } from '@/api/auth';
import { estado } from '@/mocks/db/estado';
import { setupServerError, setupUsuarioConSesion } from '@/test/testHelpers';

describe('confirmación del vencimiento de sesión', () => {
  it('una respuesta 401 atrasada no expulsa a quien todavía tiene sesión', async () => {
    setupUsuarioConSesion();

    expect(await sesionExpiradaConfirmada()).toBe(false);
  });

  it('confirma el cierre cuando el servidor ya no reconoce la sesión', async () => {
    setupUsuarioConSesion();
    estado.sesionUsuarioId = null;

    expect(await sesionExpiradaConfirmada()).toBe(true);
  });

  it('un error temporal del servidor no se transforma en una sesión vencida', async () => {
    setupUsuarioConSesion();
    setupServerError('get', '/auth/me', 503, 'Temporalmente no disponible');

    await expect(sesionExpiradaConfirmada()).rejects.toMatchObject({ status: 503 });
  });
});
