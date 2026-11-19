import { describe, expect, it } from 'vitest';
import { ApiError } from '@/api/apiError';
import {
  eliminarSistema,
  guardarPreferencias,
  guardarSistema,
  listarSistemas,
  obtenerPreferencias,
} from '@/api/sistemas';
import { makeTemplate } from '@/lib/sistema/core';
import { estado } from '@/mocks/db/estado';
import { setupUsuarioConSesion, setupUsuarioSinSesion } from '@/test/testHelpers';

type Sistema = ReturnType<typeof makeTemplate>;
type Preferencias = Awaited<ReturnType<typeof obtenerPreferencias>>;

const SIN_PREFERENCIAS = { descartadas: [], grupos: {}, bienvenidaVista: false };
const PREFERENCIAS_ELEGIDAS = {
  descartadas: ['stock:1'],
  grupos: { zona: false },
  bienvenidaVista: true,
};

let email = '';
let sistema: Sistema;
let segundo: Sistema;
let lista: Sistema[];
let error: unknown;
let rechazo: Promise<unknown>;
let preferencias: Preferencias;

describe('API de sistemas', () => {
  it('guarda un sistema nuevo y lo reemplaza sin duplicarlo', async () => {
    dadoUnUsuarioConSesion();

    await cuandoSeGuardaUnKioscoYSeLoRenombra();

    entoncesHayUnSoloSistemaLlamado('Mi kiosco');
  });

  it('conserva las secciones del sistema guardado', async () => {
    dadoUnUsuarioConSesion();

    await cuandoSeGuardaUnKioscoYSeLoRenombra();

    entoncesLaPrimeraSeccionTieneTresBloques();
  });

  it('cada cuenta ve solo sus sistemas', async () => {
    await dadoUnSistemaGuardadoPorLuciaYOtraSesionDistinta();

    await cuandoSeListanLosSistemas();

    entoncesLaOtraCuentaNoVeNingunSistema();
    entoncesLuciaConservaElSuyo();
  });

  it('Gratuito rechaza el segundo sistema', async () => {
    await dadoUnPrimerSistemaGuardado();

    cuandoSeIntentaGuardarUnSegundoSistema();

    await entoncesSeRechazaPorElLimiteDelPlan();
  });

  it('Gratuito conserva el primer sistema al rechazar el segundo', async () => {
    await dadoUnPrimerSistemaGuardado();
    cuandoSeIntentaGuardarUnSegundoSistema();
    await entoncesSeRechazaPorElLimiteDelPlan();

    await cuandoSeListanLosSistemas();

    entoncesLosSistemasSon(sistema.id);
  });

  it('devuelve los sistemas en el orden en que se crearon', async () => {
    await dadoDosSistemasGuardadosAlReves();

    await cuandoSeListanLosSistemas();

    entoncesLosSistemasSon(sistema.id, segundo.id);
  });

  it('rechaza un sistema inválido con el campo que falla', async () => {
    dadoUnUsuarioConSesion();

    await cuandoSeIntentaGuardarUnSistemaSinNombre();

    entoncesElErrorEsUnApiErrorDeEstado(400);
  });

  it('no guarda un sistema inválido', async () => {
    dadoUnUsuarioConSesion();
    await cuandoSeIntentaGuardarUnSistemaSinNombre();

    await cuandoSeListanLosSistemas();

    entoncesNoHaySistemas();
  });

  it('elimina un sistema', async () => {
    await dadoUnPrimerSistemaGuardado();

    await cuandoSeEliminaElSistema();
    await cuandoSeListanLosSistemas();

    entoncesNoHaySistemas();
  });

  it('avisa si el sistema eliminado ya no existe', async () => {
    await dadoUnPrimerSistemaGuardado();
    await cuandoSeEliminaElSistema();

    cuandoSeIntentaEliminarDeNuevoElSistema();

    await entoncesSeRechazaConEstado(404);
  });

  it('pide sesión', async () => {
    dadoUnUsuarioSinSesion();

    cuandoSeIntentanListarLosSistemas();

    await entoncesSeRechazaConEstado(401);
  });
});

describe('API de preferencias', () => {
  it('arranca con todo prendido', async () => {
    dadoUnUsuarioSinPreferencias();

    await cuandoSePidenLasPreferencias();

    entoncesLasPreferenciasSon(SIN_PREFERENCIAS);
  });

  it('guarda lo que la persona decide', async () => {
    dadoUnUsuarioSinPreferencias();

    await cuandoSeGuardanLasPreferenciasElegidas();

    entoncesLasPreferenciasSon(PREFERENCIAS_ELEGIDAS);
  });
});

function dadoUnUsuarioConSesion() {
  email = setupUsuarioConSesion().email;
}

function dadoUnUsuarioSinSesion() {
  setupUsuarioSinSesion();
}

function dadoUnUsuarioSinPreferencias() {
  setupUsuarioConSesion({}, { bienvenidaVista: false });
}

async function dadoUnPrimerSistemaGuardado() {
  dadoUnUsuarioConSesion();
  sistema = makeTemplate('shop', {}, false);
  await guardarSistema(sistema);
}

async function dadoUnSistemaGuardadoPorLuciaYOtraSesionDistinta() {
  email = setupUsuarioConSesion({ email: 'lucia@example.com' }).email;
  await guardarSistema(makeTemplate('shop', {}, false));
  estado.sesionUsuarioId = null;
  setupUsuarioConSesion({ email: 'otra@example.com' });
}

async function dadoDosSistemasGuardadosAlReves() {
  dadoUnUsuarioConSesion();
  estado.pruebasPremium.set(email, {
    inicioEn: new Date(Date.now() - 1000).toISOString(),
    finEn: new Date(Date.now() + 7 * 86400000).toISOString(),
    paginasIaUtilizadas: 0,
    limitePaginasIa: 10,
  });
  sistema = { ...makeTemplate('shop', {}, false), createdAt: '2026-10-01T10:00:00.000Z' };
  segundo = { ...makeTemplate('warehouse', {}, false), createdAt: '2026-10-02T10:00:00.000Z' };
  await guardarSistema(segundo);
  await guardarSistema(sistema);
}

async function cuandoSeGuardaUnKioscoYSeLoRenombra() {
  sistema = makeTemplate('shop', {}, true);
  await guardarSistema(sistema);
  await guardarSistema({ ...sistema, name: 'Mi kiosco' });
  lista = await listarSistemas();
}

function cuandoSeIntentaGuardarUnSegundoSistema() {
  rechazo = guardarSistema(makeTemplate('warehouse', {}, false));
}

async function cuandoSeIntentaGuardarUnSistemaSinNombre() {
  const invalido = { ...makeTemplate('shop', {}, false), name: '   ' };
  error = await guardarSistema(invalido).catch((e: unknown) => e);
}

async function cuandoSeListanLosSistemas() {
  lista = await listarSistemas();
}

function cuandoSeIntentanListarLosSistemas() {
  rechazo = listarSistemas();
}

async function cuandoSeEliminaElSistema() {
  await eliminarSistema(sistema.id);
}

function cuandoSeIntentaEliminarDeNuevoElSistema() {
  rechazo = eliminarSistema(sistema.id);
}

async function cuandoSePidenLasPreferencias() {
  preferencias = await obtenerPreferencias();
}

async function cuandoSeGuardanLasPreferenciasElegidas() {
  await guardarPreferencias(PREFERENCIAS_ELEGIDAS);
  preferencias = await obtenerPreferencias();
}

function entoncesHayUnSoloSistemaLlamado(nombre: string) {
  expect(lista).toHaveLength(1);
  expect(lista[0]?.name).toBe(nombre);
}

function entoncesLaPrimeraSeccionTieneTresBloques() {
  expect(lista[0]?.sections[0]?.blocks).toHaveLength(3);
}

function entoncesLaOtraCuentaNoVeNingunSistema() {
  expect(lista).toEqual([]);
}

function entoncesLuciaConservaElSuyo() {
  expect(estado.sistemas.get('lucia@example.com')).toHaveLength(1);
}

function entoncesNoHaySistemas() {
  expect(lista).toEqual([]);
}

function entoncesLosSistemasSon(...ids: string[]) {
  expect(lista.map((item) => item.id)).toEqual(ids);
}

function entoncesElErrorEsUnApiErrorDeEstado(estadoHttp: number) {
  expect(error).toBeInstanceOf(ApiError);
  expect((error as ApiError).status).toBe(estadoHttp);
}

async function entoncesSeRechazaPorElLimiteDelPlan() {
  await expect(rechazo).rejects.toMatchObject({ status: 403, codigo: 'LIMITE_PLAN' });
}

async function entoncesSeRechazaConEstado(estadoHttp: number) {
  await expect(rechazo).rejects.toMatchObject({ status: estadoHttp });
}

function entoncesLasPreferenciasSon(esperadas: unknown) {
  expect(preferencias).toEqual(esperadas);
}
