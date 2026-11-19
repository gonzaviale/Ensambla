import { expect } from 'vitest';
import {
  aplicarAcciones,
  mismoSistema,
  sumarPendientes,
  type AccionDeSami,
  type ResultadoDeAcciones,
} from '@/lib/sistema/acciones';
import { allBlocks, clone, makeSystem, makeTemplate } from '@/lib/sistema/core';
import type { Bloque, Sistema } from '@/lib/sistema/tipos';

type Argumentos = Record<string, unknown>;
type Filas = Record<string, unknown>[];

export const taller: { sistema: Sistema; antes: Sistema; resultado: ResultadoDeAcciones } = {
  sistema: makeSystem('Mi sistema', [], false),
  antes: makeSystem('Mi sistema', [], false),
  resultado: { aplicadas: [], omitidas: [], pendientes: [], sinStock: [] },
};

export const pedido = (herramienta: string, argumentos: Argumentos = {}): AccionDeSami => ({
  herramienta,
  argumentos,
});

export const agregarPieza = (tipo: string, otros: Argumentos = {}) =>
  pedido('agregar_pieza', { tipo, ...otros });
export const quitarPieza = (pieza: string) => pedido('quitar_pieza', { pieza });
export const renombrarPieza = (pieza: string, nombre: string) =>
  pedido('renombrar_pieza', { pieza, nombre });
export const renombrarSistema = (nombre: string) => pedido('renombrar_sistema', { nombre });
export const separarEnSeccion = (pieza: string, seccion: string) =>
  pedido('separar_en_seccion', { pieza, seccion });
export const cambiarDistribucion = (layout: string) => pedido('cambiar_distribucion', { layout });
export const agregarCampo = (
  pieza: string,
  etiqueta: string,
  tipo: string,
  otros: Argumentos = {},
) => pedido('agregar_campo', { pieza, etiqueta, tipo, ...otros });
export const quitarCampo = (pieza: string, campo: string) =>
  pedido('quitar_campo', { pieza, campo });
export const editarCampo = (pieza: string, campo: string, cambios: Argumentos = {}) =>
  pedido('editar_campo', { pieza, campo, ...cambios });
export const cargarEjemplos = (pieza: string, filas: Filas) =>
  pedido('cargar_ejemplos', { pieza, filas });
export const cargarRegistros = (pieza: string, filas: Filas) =>
  pedido('cargar_registros', { pieza, filas });
export const editarRegistros = (pieza: string, registros: unknown[]) =>
  pedido('editar_registros', { pieza, registros });
export const borrarRegistros = (pieza: string, criterio: Argumentos = {}) =>
  pedido('borrar_registros', { pieza, ...criterio });
export const conectar = (desde: string, hacia: string, otros: Argumentos = {}) =>
  pedido('conectar', { desde, hacia, ...otros });

export const piezaDelTipo = (tipo: string, en: Sistema = taller.sistema): Bloque => {
  const encontrada = allBlocks(en).find((bloque) => bloque.type === tipo);
  if (!encontrada) throw new Error(`Falta la pieza ${tipo}`);
  return encontrada;
};

export const piezaLlamada = (nombre: string, en: Sistema = taller.sistema): Bloque => {
  const encontrada = allBlocks(en).find((bloque) => bloque.name === nombre);
  if (!encontrada) throw new Error(`Falta la pieza ${nombre}`);
  return encontrada;
};

export const idDeLaPieza = (tipo: string) => piezaDelTipo(tipo).id;

export const dadoUnAlmacenDeEjemplo = () => {
  taller.sistema = makeTemplate('shop', {}, true);
};

export const dadoUnAlmacenSinEjemplos = () => {
  taller.sistema = makeTemplate('shop', {}, false);
};

export const dadoUnSistemaVacio = () => {
  taller.sistema = makeSystem('Mi sistema', [], false);
};

export const dadoQueSamiYaPidio = (...acciones: AccionDeSami[]) =>
  aplicarAcciones(taller.sistema, acciones);

/** Aplica las acciones y confirma los clientes y productos que no estaban, como al tocar «Sumarlos». */
export const dadoQueSamiYaCargoYSeSumaronLosPendientes = (...acciones: AccionDeSami[]) => {
  const resultado = aplicarAcciones(taller.sistema, acciones);
  sumarPendientes(taller.sistema, resultado.pendientes);
  return resultado;
};

export const cuandoSamiPide = (...acciones: AccionDeSami[]) => {
  taller.antes = clone(taller.sistema);
  taller.resultado = aplicarAcciones(taller.sistema, acciones);
};

export const entoncesSeAplico = (...descripciones: string[]) => {
  expect(taller.resultado.aplicadas).toEqual(descripciones);
};

export const entoncesSeAplicaron = (cantidad: number) => {
  expect(taller.resultado.aplicadas).toHaveLength(cantidad);
};

export const entoncesSeOmitio = (...motivos: string[]) => {
  expect(taller.resultado.omitidas).toEqual(motivos);
};

export const entoncesSoloSeOmitio = (...motivos: string[]) => {
  expect(taller.resultado.aplicadas).toEqual([]);
  expect(taller.resultado.omitidas).toEqual(motivos);
};

export const entoncesSeOmitieron = (cantidad: number) => {
  expect(taller.resultado.aplicadas).toEqual([]);
  expect(taller.resultado.omitidas).toHaveLength(cantidad);
};

export const entoncesSoloSeHizo = (descripcion: string) => {
  expect(taller.resultado).toEqual({
    aplicadas: [descripcion],
    omitidas: [],
    pendientes: [],
    sinStock: [],
  });
};

export const entoncesNoCambioNada = () => {
  expect(mismoSistema(taller.sistema, taller.antes)).toBe(true);
};
