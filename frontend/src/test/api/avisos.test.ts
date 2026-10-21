import { describe, expect, it } from 'vitest';
import { ApiError } from '@/api/apiError';
import { aplicarAviso, descartarAviso, listarAvisosPendientes } from '@/api/avisos';
import { estado } from '@/mocks/db/estado';
import { setupUsuarioConSesion, setupUsuarioSinSesion } from '@/test/testHelpers';
import { avisoDelServidor } from '@/test/avisosData';

describe('API de avisos', () => {
  it('lista sólo los avisos pendientes de la cuenta', async () => {
    const usuario = setupUsuarioConSesion();
    estado.avisosDelServidor.set(usuario.email, [
      avisoDelServidor(),
      avisoDelServidor({ id: 'otro', estado: 'DESCARTADO', resueltoEn: '2026-10-19T13:00:00Z' }),
    ]);

    const avisos = await listarAvisosPendientes();

    expect(avisos.map((aviso) => aviso.id)).toEqual(['e0000000-0000-4000-8000-000000000001']);
  });

  it('aplicar cierra el aviso y devuelve su estado', async () => {
    const usuario = setupUsuarioConSesion();
    const aviso = avisoDelServidor();
    estado.avisosDelServidor.set(usuario.email, [aviso]);

    const resuelto = await aplicarAviso(aviso.id);

    expect(resuelto.estado).toBe('APLICADO');
    expect(resuelto.resueltoEn).not.toBeNull();
    expect(await listarAvisosPendientes()).toEqual([]);
  });

  it('descartar cierra el aviso y deja de listarse', async () => {
    const usuario = setupUsuarioConSesion();
    const aviso = avisoDelServidor();
    estado.avisosDelServidor.set(usuario.email, [aviso]);

    const resuelto = await descartarAviso(aviso.id);

    expect(resuelto.estado).toBe('DESCARTADO');
    expect(await listarAvisosPendientes()).toEqual([]);
  });

  it('un aviso de otra cuenta responde como si no existiera', async () => {
    estado.avisosDelServidor.set('otra@example.com', [avisoDelServidor()]);
    setupUsuarioConSesion({ email: 'lucia@example.com' });

    const error = await aplicarAviso('e0000000-0000-4000-8000-000000000001').catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).status).toBe(404);
  });

  it('no se puede resolver dos veces el mismo aviso', async () => {
    const usuario = setupUsuarioConSesion();
    const aviso = avisoDelServidor();
    estado.avisosDelServidor.set(usuario.email, [aviso]);
    await descartarAviso(aviso.id);

    const error = await descartarAviso(aviso.id).catch((e: unknown) => e);

    expect((error as ApiError).status).toBe(400);
    expect((error as ApiError).message).toBe('El aviso ya fue resuelto');
  });

  it('exige una sesión válida', async () => {
    setupUsuarioSinSesion();

    const error = await listarAvisosPendientes().catch((e: unknown) => e);

    expect((error as ApiError).status).toBe(401);
  });
});
