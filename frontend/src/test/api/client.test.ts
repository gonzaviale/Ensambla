import { HttpResponse, http as mswHttp } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import { z } from 'zod';
import { ApiError } from '@/api/apiError';
import { EVENTO_SESION_EXPIRADA, http, invalidarCsrf } from '@/api/client';
import { server } from '@/mocks/node';
import { setupServerError, setupServerResponse } from '@/test/testHelpers';

const schema = z.object({ nombre: z.string() });

let resultado: unknown;
let error: unknown;
let consulta: Promise<unknown>;
let tokenRecibido: string | null;
let intentos = 0;
let avisosDeSesionExpirada = 0;

const contarAviso = () => (avisosDeSesionExpirada += 1);

describe('http', () => {
  beforeEach(() => {
    invalidarCsrf();
    intentos = 0;
    tokenRecibido = null;
    avisosDeSesionExpirada = 0;
  });

  describe('respuestas', () => {
    it('devuelve el cuerpo validado con el schema', async () => {
      dadoQueElServidorResponde200({ nombre: 'Ana' });

      await cuandoSePideLaRuta('/prueba');

      entoncesElResultadoEs({ nombre: 'Ana' });
    });

    it('convierte el ErrorResponse del backend en ApiError', async () => {
      dadoQueElServidorFalla(404, 'Proyecto no encontrado');

      await cuandoSePideLaRutaYFalla('/prueba');

      entoncesElErrorEsUnApiErrorNoEncontrado('Proyecto no encontrado');
    });

    it('usa un mensaje genérico cuando el error no tiene el formato esperado', async () => {
      dadoQueElServidorResponde500ConTextoPlano();

      await cuandoSePideLaRutaYFalla('/prueba');

      entoncesElErrorTieneEstado(500, { isClientError: false });
    });

    it('falla en el borde cuando el contrato no se cumple', async () => {
      dadoQueElServidorResponde200({ nombre: 3 });

      cuandoSeIntentaPedirLaRuta('/prueba');

      await entoncesSeRechazaElContrato();
    });

    it('informa sin conexión con status 0 cuando la red falla', async () => {
      dadoQueLaRedFalla();

      await cuandoSePideLaRutaYFalla('/prueba');

      entoncesElErrorTieneEstado(0, { message: expect.stringContaining('conectarnos') });
    });
  });

  describe('CSRF', () => {
    it('manda el token CSRF en las escrituras', async () => {
      dadoUnServidorQueRecuerdaElTokenCsrf();

      await cuandoSeEscribe();

      entoncesElServidorRecibioElToken('csrf-token-de-prueba');
    });

    it('reintenta una vez con un token nuevo cuando el backend responde 403', async () => {
      dadoUnServidorQueRechazaElPrimerTokenConUn403();

      await cuandoSeEscribe();

      entoncesElResultadoEs({ nombre: 'ok' });
      entoncesSeIntentoDosVeces();
    });
  });

  describe('sesión', () => {
    it('avisa que la sesión expiró cuando un endpoint privado responde 401', async () => {
      dadoQueElServidorFalla(401, 'Se requiere una sesión válida', '/proyectos');

      await cuandoSePideLaRutaEscuchandoLaExpiracion('/proyectos');

      entoncesSeAvisoLaExpiracion(1);
    });

    it('no avisa expiración cuando el 401 viene de /auth', async () => {
      dadoQueElServidorFalla(401, 'Se requiere una sesión válida', '/auth/me');

      await cuandoSePideLaRutaEscuchandoLaExpiracion('/auth/me');

      entoncesSeAvisoLaExpiracion(0);
    });
  });
});

function dadoQueElServidorResponde200(cuerpo: unknown) {
  setupServerResponse('get', '/prueba', 200, cuerpo);
}

function dadoQueElServidorFalla(estado: number, mensaje: string, ruta = '/prueba') {
  setupServerError('get', ruta, estado, mensaje);
}

function dadoQueElServidorResponde500ConTextoPlano() {
  server.use(mswHttp.get('/api/prueba', () => new HttpResponse('boom', { status: 500 })));
}

function dadoQueLaRedFalla() {
  server.use(mswHttp.get('/api/prueba', () => HttpResponse.error()));
}

function dadoUnServidorQueRecuerdaElTokenCsrf() {
  server.use(
    mswHttp.post('/api/prueba', ({ request }) => {
      tokenRecibido = request.headers.get('X-CSRF-TOKEN');
      return HttpResponse.json({ nombre: 'ok' });
    }),
  );
}

function dadoUnServidorQueRechazaElPrimerTokenConUn403() {
  server.use(
    mswHttp.post('/api/prueba', () => {
      intentos += 1;
      return intentos === 1
        ? new HttpResponse(null, { status: 403 })
        : HttpResponse.json({ nombre: 'ok' });
    }),
  );
}

async function cuandoSePideLaRuta(ruta: string) {
  resultado = await http.get(ruta, schema);
}

function cuandoSeIntentaPedirLaRuta(ruta: string) {
  consulta = http.get(ruta, schema);
}

async function cuandoSePideLaRutaYFalla(ruta: string) {
  error = await http.get(ruta, schema).catch((e: unknown) => e);
}

async function cuandoSeEscribe() {
  resultado = await http.post('/prueba', { dato: 1 }, schema);
}

async function cuandoSePideLaRutaEscuchandoLaExpiracion(ruta: string) {
  window.addEventListener(EVENTO_SESION_EXPIRADA, contarAviso);
  await http.get(ruta, schema).catch(() => undefined);
  window.removeEventListener(EVENTO_SESION_EXPIRADA, contarAviso);
}

function entoncesElResultadoEs(esperado: unknown) {
  expect(resultado).toEqual(esperado);
}

function entoncesElErrorEsUnApiErrorNoEncontrado(mensaje: string) {
  expect(error).toBeInstanceOf(ApiError);
  expect(error).toMatchObject({ status: 404, message: mensaje, isNotFound: true });
}

function entoncesElErrorTieneEstado(estado: number, otros: Record<string, unknown>) {
  expect(error).toMatchObject({ status: estado, ...otros });
}

async function entoncesSeRechazaElContrato() {
  await expect(consulta).rejects.toThrow();
}

function entoncesElServidorRecibioElToken(token: string) {
  expect(tokenRecibido).toBe(token);
}

function entoncesSeIntentoDosVeces() {
  expect(intentos).toBe(2);
}

function entoncesSeAvisoLaExpiracion(veces: number) {
  expect(avisosDeSesionExpirada).toBe(veces);
}
