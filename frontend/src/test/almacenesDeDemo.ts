import { expect } from 'vitest';
import { analizar, type Aviso, type TipoDeAviso } from '@/lib/sistema/alertas';
import { allBlocks } from '@/lib/sistema/core';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import type { Sistema, TipoBloque } from '@/lib/sistema/tipos';
import { zonaDeLaPersona } from '@/mocks/db/zona';

const HOY = '2026-10-31';

let sistema: Sistema;
let avisos: Aviso[] = [];

const filasDe = (tipo: TipoBloque) =>
  allBlocks(sistema).find((bloque) => bloque.type === tipo)?.rows ?? [];

export const cuandoSeArmaElAlmacenCargado = () => {
  sistema = crearAlmacenCargado(HOY);
};

export const cuandoSeAnalizaElAlmacenCargado = () => {
  cuandoSeArmaElAlmacenCargado();
  avisos = analizar({ sistemas: [sistema], zona: zonaDeLaPersona(), hoy: HOY });
};

export const entoncesTieneAlMenos = (tipo: TipoBloque, cantidad: number) => {
  expect(filasDe(tipo).length).toBeGreaterThanOrEqual(cantidad);
};

export const entoncesLasVentasApuntanAProductosQueExisten = () => {
  const productos = new Set(filasDe('inventory').map((fila) => fila.id));
  expect(filasDe('sales').every((venta) => productos.has(String(venta.product)))).toBe(true);
};

export const entoncesHayAlMenosAvisos = (cantidad: number) => {
  expect(avisos.length).toBeGreaterThanOrEqual(cantidad);
};

export const entoncesHayAvisosDeLosTipos = (tipos: TipoDeAviso[]) => {
  expect([...new Set(avisos.map((aviso) => aviso.tipo))]).toEqual(expect.arrayContaining(tipos));
};
