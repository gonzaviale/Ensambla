import { HttpResponse, http as mswHttp } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import { z } from 'zod';
import { ApiError } from '@/api/apiError';
import { EVENTO_SESION_EXPIRADA, http, invalidarCsrf } from '@/api/client';
import { server } from '@/mocks/node';
import { setupServerError, setupServerResponse } from '@/test/testHelpers';

const schema = z.object({ nombre: z.string() });

describe('http', () => {
  beforeEach(() => {
    invalidarCsrf();
  });

  describe('respuestas', () => {
    it('devuelve el cuerpo validado con el schema', async () => {
      setupServerResponse('get', '/prueba', 200, { nombre: 'Ana' });

      const resultado = await http.get('/prueba', schema);

      expect(resultado).toEqual({ nombre: 'Ana' });
    });

    it('convierte el ErrorResponse del backend en ApiError', async () => {
      setupServerError('get', '/prueba', 404, 'Proyecto no encontrado');

      const error = await http.get('/prueba', schema).catch((e: unknown) => e);

      expect(error).toBeInstanceOf(ApiError);
      expect(error).toMatchObject({
        status: 404,
        message: 'Proyecto no encontrado',
        isNotFound: true,
      });
    });

    it('usa un mensaje genérico cuando el error no tiene el formato esperado', async () => {
      server.use(mswHttp.get('/api/prueba', () => new HttpResponse('boom', { status: 500 })));

      const error = await http.get('/prueba', schema).catch((e: unknown) => e);

      expect(error).toMatchObject({ status: 500, isClientError: false });
    });

    it('falla en el borde cuando el contrato no se cumple', async () => {
      setupServerResponse('get', '/prueba', 200, { nombre: 3 });

      await expect(http.get('/prueba', schema)).rejects.toThrow();
    });

    it('informa sin conexión con status 0 cuando la red falla', async () => {
      server.use(mswHttp.get('/api/prueba', () => HttpResponse.error()));

      const error = await http.get('/prueba', schema).catch((e: unknown) => e);

      expect(error).toMatchObject({ status: 0, message: expect.stringContaining('conectarnos') });
    });
  });

  describe('CSRF', () => {
    it('manda el token CSRF en las escrituras', async () => {
      let recibido: string | null = null;
      server.use(
        mswHttp.post('/api/prueba', ({ request }) => {
          recibido = request.headers.get('X-CSRF-TOKEN');
          return HttpResponse.json({ nombre: 'ok' });
        }),
      );

      await http.post('/prueba', { dato: 1 }, schema);

      expect(recibido).toBe('csrf-token-de-prueba');
    });

    it('reintenta una vez con un token nuevo cuando el backend responde 403', async () => {
      let intentos = 0;
      server.use(
        mswHttp.post('/api/prueba', () => {
          intentos += 1;
          return intentos === 1
            ? new HttpResponse(null, { status: 403 })
            : HttpResponse.json({ nombre: 'ok' });
        }),
      );

      const resultado = await http.post('/prueba', {}, schema);

      expect(resultado).toEqual({ nombre: 'ok' });
      expect(intentos).toBe(2);
    });
  });

  describe('sesión', () => {
    it('avisa que la sesión expiró cuando un endpoint privado responde 401', async () => {
      setupServerError('get', '/proyectos', 401, 'Se requiere una sesión válida');
      let avisos = 0;
      const alAvisar = () => (avisos += 1);
      window.addEventListener(EVENTO_SESION_EXPIRADA, alAvisar);

      await http.get('/proyectos', schema).catch(() => undefined);
      window.removeEventListener(EVENTO_SESION_EXPIRADA, alAvisar);

      expect(avisos).toBe(1);
    });

    it('no avisa expiración cuando el 401 viene de /auth', async () => {
      setupServerError('get', '/auth/me', 401, 'Se requiere una sesión válida');
      let avisos = 0;
      const alAvisar = () => (avisos += 1);
      window.addEventListener(EVENTO_SESION_EXPIRADA, alAvisar);

      await http.get('/auth/me', schema).catch(() => undefined);
      window.removeEventListener(EVENTO_SESION_EXPIRADA, alAvisar);

      expect(avisos).toBe(0);
    });
  });
});
