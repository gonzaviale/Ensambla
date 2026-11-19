import { describe, expect, it } from 'vitest';
import { desbloquearAvisos } from '@/api/contenidoPlan';
import { obtenerZona, analizarZona } from '@/api/zona';
import { guardarSistema } from '@/api/sistemas';
import { http } from '@/api/client';
import { estado } from '@/mocks/db/estado';
import { setupUsuarioConSesion } from '@/test/testHelpers';
import { makeSystem } from '@/lib/sistema/core';
import { crearAlmacenDeEjemplo } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import { propioDe, productosPropios } from '@/lib/sistema/zona';
import { zonaSchema } from '@/types/zona';

type Acceso = Awaited<ReturnType<typeof desbloquearAvisos>>;
type Zona = Awaited<ReturnType<typeof obtenerZona>>;

let email = '';
let primero: Acceso;
let repetido: Acceso;
let acceso: Acceso;
let precios: Zona;
let cerca: Zona;
let otra: Awaited<ReturnType<typeof analizarZona>>;
let consultaInvalida: Promise<unknown>;
let primerSistema: ReturnType<typeof makeSystem>;
let segundoSistema: Promise<unknown>;

describe('cupos de contenido del plan', () => {
  it('permite seis avisos', async () => {
    dadoUnUsuarioConSesion();

    await cuandoSeDesbloqueanOchoAvisosYSeRepiteUnaConsulta();

    entoncesSoloSeAutorizanLosPrimerosSeis();
  });

  it('una consulta repetida no libera el séptimo aviso', async () => {
    dadoUnUsuarioConSesion();

    await cuandoSeDesbloqueanOchoAvisosYSeRepiteUnaConsulta();

    entoncesLaConsultaRepetidaSoloReconoceElPrimero();
  });

  it('la prueba Premium ofrece todos los avisos sin gastar el cupo gratuito', async () => {
    dadoUnaPruebaPremiumActiva();

    await cuandoSeDesbloqueanCuatroAvisos();

    entoncesSeAutorizanLosCuatroSinLimite();
    entoncesNoSeGastoElCupoGratuito();
  });

  it('Precios, Qué se vende cerca y Otra zona gastan cada uno sus seis productos mensuales', async () => {
    dadoUnUsuarioConSesion();

    await cuandoSeConsultanLasTresVistasDeZona();

    entoncesCadaVistaGastoSusSeisProductos();
    entoncesOtraZonaDejoProductosBloqueados();
  });

  it('Precios desbloquea primero lo que el negocio ya tiene', async () => {
    dadoUnAlmacenDeEjemplo();

    await cuandoSeConsultanPreciosYCerca();

    entoncesPreciosEmpiezaPorLoQueYaTengo();
  });

  it('Qué se vende cerca desbloquea primero lo que le falta al negocio', async () => {
    dadoUnAlmacenDeEjemplo();

    await cuandoSeConsultanPreciosYCerca();

    entoncesCercaEmpiezaPorLoQueMeFalta();
  });

  it('una vista desconocida se rechaza', async () => {
    dadoUnUsuarioConSesion();

    cuandoSePideUnaVistaDeZonaDesconocida();

    await entoncesSeRechazaConEstado(400);
  });

  it('el segundo sistema gratuito falla sin guardarse', async () => {
    await dadoUnPrimerSistemaGuardado();

    cuandoSeIntentaGuardarUnSegundoSistema();

    await entoncesSeRechazaPorElLimiteDelPlan();
  });

  it('el primer sistema sigue editable después de rechazar el segundo', async () => {
    await dadoUnPrimerSistemaGuardado();
    cuandoSeIntentaGuardarUnSegundoSistema();
    await entoncesSeRechazaPorElLimiteDelPlan();

    await cuandoSeCorrigeElNombreDelPrimerSistema();

    entoncesQuedaUnSoloSistemaLlamado('Corregido');
  });
});

function dadoUnUsuarioConSesion() {
  email = setupUsuarioConSesion().email;
}

function dadoUnaPruebaPremiumActiva() {
  dadoUnUsuarioConSesion();
  estado.pruebasPremium.set(email, {
    inicioEn: new Date(Date.now() - 1000).toISOString(),
    finEn: new Date(Date.now() + 86400000).toISOString(),
    paginasIaUtilizadas: 0,
    limitePaginasIa: 10,
  });
}

function dadoUnAlmacenDeEjemplo() {
  dadoUnUsuarioConSesion();
  estado.sistemas.set(email, [crearAlmacenDeEjemplo(hoy())]);
}

async function dadoUnPrimerSistemaGuardado() {
  dadoUnUsuarioConSesion();
  primerSistema = makeSystem('Primero');
  await guardarSistema(primerSistema);
}

async function cuandoSeDesbloqueanOchoAvisosYSeRepiteUnaConsulta() {
  primero = await desbloquearAvisos(['a', 'b', 'a', 'c', 'd', 'e', 'f', 'g']);
  repetido = await desbloquearAvisos(['a', 'g']);
}

async function cuandoSeDesbloqueanCuatroAvisos() {
  acceso = await desbloquearAvisos(['a', 'b', 'c', 'd']);
}

async function cuandoSeConsultanLasTresVistasDeZona() {
  precios = await obtenerZona();
  cerca = await obtenerZona('cerca');
  otra = await analizarZona({ lugar: 'Palermo' });
}

async function cuandoSeConsultanPreciosYCerca() {
  precios = await obtenerZona();
  cerca = await obtenerZona('cerca');
}

function cuandoSePideUnaVistaDeZonaDesconocida() {
  consultaInvalida = http.get('/zona/agregados?vista=otra', zonaSchema);
}

function cuandoSeIntentaGuardarUnSegundoSistema() {
  segundoSistema = guardarSistema(makeSystem('Segundo'));
}

async function cuandoSeCorrigeElNombreDelPrimerSistema() {
  await guardarSistema({ ...primerSistema, name: 'Corregido' });
}

function entoncesSoloSeAutorizanLosPrimerosSeis() {
  expect(primero.autorizadas).toEqual(['a', 'b', 'c', 'd', 'e', 'f']);
  expect(primero.bloqueadas).toBe(1);
}

function entoncesLaConsultaRepetidaSoloReconoceElPrimero() {
  expect(repetido.autorizadas).toEqual(['a']);
  expect(repetido.usadas).toBe(6);
}

function entoncesSeAutorizanLosCuatroSinLimite() {
  expect(acceso.autorizadas).toHaveLength(4);
  expect(acceso.limite).toBeNull();
}

function entoncesNoSeGastoElCupoGratuito() {
  expect(estado.contenidosDesbloqueados.size).toBe(0);
}

function entoncesCadaVistaGastoSusSeisProductos() {
  for (const vista of [precios, cerca, otra]) {
    expect(vista.productos).toHaveLength(6);
    expect(vista.acceso?.usadas).toBe(6);
  }
}

function entoncesOtraZonaDejoProductosBloqueados() {
  expect(otra.acceso?.bloqueadas).toBeGreaterThan(0);
}

const loTengo = (zona: Zona) => {
  const propios = productosPropios(estado.sistemas.get(email) ?? []);
  return zona.productos.map((producto) => Boolean(propioDe(producto, propios)));
};

function entoncesPreciosEmpiezaPorLoQueYaTengo() {
  expect(precios.productos).toHaveLength(6);
  expect(loTengo(precios)[0]).toBe(true);
  expect(loTengo(precios)).toEqual([...loTengo(precios)].sort((a, b) => Number(b) - Number(a)));
}

function entoncesCercaEmpiezaPorLoQueMeFalta() {
  expect(cerca.productos).toHaveLength(6);
  expect(loTengo(cerca)[0]).toBe(false);
  expect(loTengo(cerca)).toEqual([...loTengo(cerca)].sort((a, b) => Number(a) - Number(b)));
}

async function entoncesSeRechazaConEstado(estadoHttp: number) {
  await expect(consultaInvalida).rejects.toMatchObject({ status: estadoHttp });
}

async function entoncesSeRechazaPorElLimiteDelPlan() {
  await expect(segundoSistema).rejects.toMatchObject({ status: 403, codigo: 'LIMITE_PLAN' });
}

function entoncesQuedaUnSoloSistemaLlamado(nombre: string) {
  expect(estado.sistemas.get(email)).toHaveLength(1);
  expect(estado.sistemas.get(email)?.[0]?.name).toBe(nombre);
}
