import { describe, expect, it } from 'vitest';
import { HttpResponse, http } from 'msw';
import { consultarPlanes, iniciarPruebaPremium } from '@/api/planes';
import { estado } from '@/mocks/db/estado';
import { planesDe } from '@/mocks/db/planes';
import { server } from '@/mocks/node';
import { setupServerResponse, setupUsuarioConSesion } from '@/test/testHelpers';

type Planes = Awaited<ReturnType<typeof consultarPlanes>>;
type Prueba = Awaited<ReturnType<typeof iniciarPruebaPremium>>;

const SIETE_DIAS_EN_MS = 7 * 24 * 60 * 60 * 1000;

let email = '';
let planes: Planes;
let prueba: Prueba;
let primeraPrueba: Prueba;
let rechazo: Promise<unknown>;
let intentos = 0;

describe('API de planes', () => {
  it('consulta el plan gratuito de la sesión', async () => {
    dadoUnUsuarioConSesion();

    await cuandoSeConsultanLosPlanes();

    entoncesElPlanActualEsGratuitoYSePuedeIniciarLaPrueba();
  });

  it('consulta el catálogo con el precio mensual aprobado', async () => {
    dadoUnUsuarioConSesion();

    await cuandoSeConsultanLosPlanes();

    entoncesElCatalogoTieneDosPlanesYElPremiumCuesta(14900);
  });

  it('consultar los planes no inicia una prueba', async () => {
    dadoUnUsuarioConSesion();

    await cuandoSeConsultanLosPlanes();

    entoncesNoSeIniciaNingunaPrueba();
  });

  it('activa siete días con diez páginas totales usando la sesión y CSRF', async () => {
    dadoUnUsuarioConSesion();

    await cuandoSeActivaLaPrueba();

    entoncesLaPruebaDuraSieteDiasConDiezPaginas();
    entoncesLosPlanesMuestranLaPruebaActiva();
  });

  it('conserva el código comercial y no repite un POST rechazado como si fuera CSRF', async () => {
    dadoQueElServidorRechazaLaPruebaPorYaUtilizada();

    cuandoSeIntentaActivarLaPrueba();

    await entoncesSeRechazaConElCodigo('PRUEBA_UTILIZADA');
    entoncesSeIntentoUnaSolaVez();
  });

  it('una segunda activación devuelve PRUEBA_UTILIZADA', async () => {
    await dadoUnaPruebaYaActivada();

    cuandoSeIntentaActivarLaPrueba();

    await entoncesSeRechazaConElCodigo('PRUEBA_UTILIZADA');
  });

  it('una segunda activación conserva las fechas de la primera', async () => {
    await dadoUnaPruebaYaActivada();

    cuandoSeIntentaActivarLaPrueba();
    await entoncesSeRechazaConElCodigo('PRUEBA_UTILIZADA');

    entoncesLaPruebaGuardadaEsLaPrimera();
  });

  it('una sesión diferente no recibe la prueba de la cuenta anterior', async () => {
    await dadoUnaPruebaActivadaPorOtraCuenta();

    await cuandoSeConsultanLosPlanes();

    entoncesNoHayPruebaYSePuedeIniciar();
  });

  it('rechaza un contrato inválido sin interpretar una cuota negativa como disponible', async () => {
    dadoUnServidorQueDevuelveUnaCuotaNegativa();

    cuandoSeIntentanConsultarLosPlanes();

    await entoncesSeRechazaElContrato();
  });
});

function dadoUnUsuarioConSesion() {
  email = setupUsuarioConSesion().email;
}

function dadoQueElServidorRechazaLaPruebaPorYaUtilizada() {
  dadoUnUsuarioConSesion();
  intentos = 0;
  server.use(
    http.post('/api/planes/prueba', () => {
      intentos++;
      return HttpResponse.json(
        {
          timestamp: new Date().toISOString(),
          status: 403,
          error: 'Forbidden',
          message: 'Esta cuenta ya utilizó su prueba premium.',
          path: '/api/planes/prueba',
          traceId: 'prueba',
          codigo: 'PRUEBA_UTILIZADA',
        },
        { status: 403 },
      );
    }),
  );
}

async function dadoUnaPruebaYaActivada() {
  dadoUnUsuarioConSesion();
  primeraPrueba = await iniciarPruebaPremium();
}

async function dadoUnaPruebaActivadaPorOtraCuenta() {
  setupUsuarioConSesion({ email: 'primera@example.test' });
  await iniciarPruebaPremium();
  setupUsuarioConSesion({ email: 'segunda@example.test' });
}

function dadoUnServidorQueDevuelveUnaCuotaNegativa() {
  const usuario = setupUsuarioConSesion();
  const datos = planesDe(usuario.email);
  setupServerResponse('get', '/planes', 200, { ...datos, uso: { ...datos.uso, paginasIa: -1 } });
}

async function cuandoSeConsultanLosPlanes() {
  planes = await consultarPlanes();
}

function cuandoSeIntentanConsultarLosPlanes() {
  rechazo = consultarPlanes();
}

async function cuandoSeActivaLaPrueba() {
  prueba = await iniciarPruebaPremium();
  planes = await consultarPlanes();
}

function cuandoSeIntentaActivarLaPrueba() {
  rechazo = iniciarPruebaPremium();
}

function entoncesElPlanActualEsGratuitoYSePuedeIniciarLaPrueba() {
  expect(planes.suscripcion.codigoPlanActual).toBe('GRATUITO');
  expect(planes.suscripcion.puedeIniciarPrueba).toBe(true);
}

function entoncesElCatalogoTieneDosPlanesYElPremiumCuesta(precio: number) {
  expect(planes.planes).toHaveLength(2);
  expect(planes.planes[1]?.precioMensual).toBe(precio);
}

function entoncesNoSeIniciaNingunaPrueba() {
  expect(estado.pruebasPremium.size).toBe(0);
}

function entoncesLaPruebaDuraSieteDiasConDiezPaginas() {
  expect(Date.parse(prueba.finEn) - Date.parse(prueba.inicioEn)).toBe(SIETE_DIAS_EN_MS);
  expect(prueba.limitePaginasIa).toBe(10);
  expect(prueba.paginasIaUtilizadas).toBe(0);
}

function entoncesLosPlanesMuestranLaPruebaActiva() {
  expect(planes.suscripcion.pruebaActiva).toBe(true);
  expect(planes.suscripcion.puedeIniciarPrueba).toBe(false);
  expect(planes.uso.periodoIa).toBe('PRUEBA');
}

async function entoncesSeRechazaConElCodigo(codigo: string) {
  await expect(rechazo).rejects.toMatchObject({ codigo });
}

function entoncesSeIntentoUnaSolaVez() {
  expect(intentos).toBe(1);
}

function entoncesLaPruebaGuardadaEsLaPrimera() {
  expect(estado.pruebasPremium.get(email)).toEqual(primeraPrueba);
}

function entoncesNoHayPruebaYSePuedeIniciar() {
  expect(planes.prueba).toBeNull();
  expect(planes.suscripcion.puedeIniciarPrueba).toBe(true);
}

async function entoncesSeRechazaElContrato() {
  await expect(rechazo).rejects.toThrow();
}
