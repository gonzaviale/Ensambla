import { expect } from 'vitest';
import { makeTemplate } from '@/lib/sistema/core';
import type { Sistema } from '@/lib/sistema/tipos';
import {
  seleccionarBloqueElegido,
  seleccionarSeccionActiva,
  seleccionarSistemaActivo,
  useSistemaStore,
} from '@/stores/sistemaStore';

const store = () => useSistemaStore.getState();

let sistema: Sistema;
let otroSistema: Sistema;
let original: Sistema | undefined;
let sistemasAntes: Sistema[] = [];
let resultado: unknown;
let deshizo: boolean;
let primerAviso = 0;

const nombreDelActivo = () => seleccionarSistemaActivo(store())?.name;

const renombrarElActivo = (nombre: string) =>
  store().commit((s) => {
    s.name = nombre;
  });

const unSistemaCreadoPorSami = (id: string) => ({ ...makeTemplate('business', {}, true), id });

export const dadoUnStoreVacio = () => {
  store().reiniciar();
};

export const dadoUnSistemaAbierto = () => {
  dadoUnStoreVacio();
  sistema = makeTemplate('shop', {}, true);
  store().hidratar('usuario-1', [sistema]);
  store().abrirSistema(sistema.id, 'build');
  original = store().sistemas[0];
};

export const dadoUnSistemaAbiertoYEditado = (nombre: string) => {
  dadoUnSistemaAbierto();
  renombrarElActivo(nombre);
};

export const dadoUnSistemaAbiertoQueSeElimino = () => {
  dadoUnSistemaAbierto();
  store().eliminarSistema(sistema.id);
};

export const dadoUnSistemaAbiertoYOtroQueSeElimino = () => {
  dadoUnSistemaAbierto();
  otroSistema = unSistemaCreadoPorSami('eliminado');
  store().agregarSistema(otroSistema);
  store().eliminarSistema(otroSistema.id);
};

export const dadoUnSistemaAbiertoConUnaPestanaNuevaElegida = () => {
  dadoUnSistemaAbierto();
  store().commit((s) => {
    s.sections.push({ id: 'otra', name: 'Otra', blocks: [] });
  });
  store().elegirSeccion('otra');
};

export const dadoUnSistemaAbiertoConUnBloqueElegidoYUnCambio = () => {
  dadoUnSistemaAbiertoYEditado('x');
  store().seleccionar('algo');
};

export const dadosDosSistemasSinGuardar = () => {
  dadoUnStoreVacio();
  sistema = makeTemplate('shop', {}, false);
  otroSistema = makeTemplate('warehouse', {}, false);
  store().hidratar('usuario-1', []);
  store().agregarSistema(sistema);
  store().agregarSistema(otroSistema);
};

export const dadoUnAviso = () => {
  dadoUnStoreVacio();
  store().avisar('Uno');
  primerAviso = store().aviso?.id ?? 0;
};

export const cuandoLaMismaPersonaVuelveACargar = () => {
  store().hidratar('usuario-1', [sistema]);
};

export const cuandoEntraOtraPersona = () => {
  store().hidratar('usuario-2', []);
};

export const cuandoSeIncorporaUnaVersionViejaYUnoNuevo = () => {
  store().incorporarSistemas([
    { ...sistema, name: 'Viejo' },
    unSistemaCreadoPorSami('creado-por-sami'),
  ]);
};

export const cuandoSeIncorporaElQueSeElimino = () => {
  store().incorporarSistemas([sistema]);
};

export const cuandoElServidorTraeOtroNombre = (nombre: string) => {
  store().actualizarDesdeServidor([{ ...sistema, name: nombre }]);
};

export const cuandoElServidorTraeUnoNuevoYElEliminado = () => {
  store().actualizarDesdeServidor([
    sistema,
    unSistemaCreadoPorSami('creado-por-sami'),
    otroSistema,
  ]);
};

export const cuandoElServidorTraeLoMismo = () => {
  sistemasAntes = store().sistemas;
  store().actualizarDesdeServidor([structuredClone(sistema)]);
};

export const cuandoSeVuelveAAbrirComoNuevo = () => {
  store().abrirSistema(sistema.id, 'use', true);
};

export const cuandoSeCambiaElNombre = (nombre: string) => {
  resultado = store().commit((s) => {
    s.name = nombre;
    return 42;
  });
};

export const cuandoSeDeshace = () => {
  deshizo = store().deshacer();
};

export const cuandoSeDeshaceDosVeces = () => {
  store().deshacer();
  cuandoSeDeshace();
};

export const cuandoSeCambiaElNombreCuarentaVeces = () => {
  Array.from({ length: 40 }, (_, indice) => renombrarElActivo(`Nombre ${indice}`));
};

export const cuandoSeEliminaElSistema = () => {
  store().eliminarSistema(sistema.id);
};

export const cuandoSeMarcaGuardadoElPrimero = () => {
  store().marcarGuardados([sistema.id], []);
};

export const cuandoSeEligeElPrimerBloque = () => {
  store().seleccionar(sistema.sections[0]?.blocks[0]?.id ?? null);
};

export const cuandoSeEligeLaPrimeraPestana = () => {
  cuandoSeEligeElPrimerBloque();
  store().elegirSeccion(sistema.sections[0]?.id ?? '');
};

export const cuandoSeAvisaOtraVez = () => {
  store().avisar('Uno');
};

export const cuandoSeCierraElAviso = () => {
  store().cerrarAviso();
};

export const entoncesElSistemaSeLlama = (nombre: string) => {
  expect(nombreDelActivo()).toBe(nombre);
};

export const entoncesQuedanElAbiertoYElCreadoPorSami = () => {
  expect(store().sistemas.map((item) => item.id)).toEqual([sistema.id, 'creado-por-sami']);
};

export const entoncesElPrimeroNoSeLlama = (nombre: string) => {
  expect(store().sistemas[0]?.name).not.toBe(nombre);
};

export const entoncesElPrimeroSeLlama = (nombre: string) => {
  expect(store().sistemas[0]?.name).toBe(nombre);
};

export const entoncesNoHayNadaPendiente = () => {
  expect(store().pendientes).toEqual([]);
};

export const entoncesQuedaPendienteDeGuardar = () => {
  expect(store().pendientes).toContain(sistema.id);
};

export const entoncesSoloQuedaPendienteElSegundo = () => {
  expect(store().pendientes).toEqual([otroSistema.id]);
};

export const entoncesNoHaySistemas = () => {
  expect(store().sistemas).toEqual([]);
};

export const entoncesElEstadoNoCambio = () => {
  expect(store().sistemas).toBe(sistemasAntes);
};

export const entoncesQuedaLaOtraPersonaSinNadaAbierto = () => {
  expect(store()).toMatchObject({ sistemas: [], activeId: null, usuarioId: 'usuario-2' });
};

export const entoncesQuedaListoParaUsarSinSeleccionNiHistorial = () => {
  expect(store()).toMatchObject({ modo: 'use', selectedId: null, historial: [], listo: true });
};

export const entoncesLaPestanaActivaEsLaPrimera = () => {
  expect(seleccionarSeccionActiva(store())?.id).toBe(sistema.sections[0]?.id);
};

export const entoncesLaPestanaElegidaEsLaPrimera = () => {
  expect(store().sectionId).toBe(sistema.sections[0]?.id);
};

export const entoncesDevuelveLoQueDevolvioElCambio = () => {
  expect(resultado).toBe(42);
};

export const entoncesElOriginalNoSeToco = () => {
  expect([sistema.name, original?.name]).toEqual(['Mi almacén', 'Mi almacén']);
};

export const entoncesSoloElSistemaQuedaPendiente = () => {
  expect(store().pendientes).toEqual([sistema.id]);
};

export const entoncesSePudoDeshacer = (pudo: boolean) => {
  expect(deshizo).toBe(pudo);
};

export const entoncesElHistorialTiene = (pasos: number) => {
  expect(store().historial).toHaveLength(pasos);
};

export const entoncesQuedaMarcadoParaBorrarYCerrado = () => {
  expect(store()).toMatchObject({
    sistemas: [],
    pendientes: [],
    eliminados: [sistema.id],
    activeId: null,
  });
};

export const entoncesElBloqueElegidoEsElPrimero = () => {
  expect(seleccionarBloqueElegido(store())?.id).toBe(sistema.sections[0]?.blocks[0]?.id);
};

export const entoncesNoHayBloqueElegido = () => {
  expect(store().selectedId).toBeNull();
};

export const entoncesElAvisoTieneOtroNumero = () => {
  expect(store().aviso?.id).toBeGreaterThan(primerAviso);
};

export const entoncesNoHayAviso = () => {
  expect(store().aviso).toBeNull();
};
