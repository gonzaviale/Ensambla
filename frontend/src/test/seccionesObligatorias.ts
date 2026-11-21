import { expect } from 'vitest';
import { allBlocks, makeSystem } from '@/lib/sistema/core';
import { esColumnaFija, TIPOS_OBLIGATORIOS } from '@/lib/sistema/estructuraFija';
import { completarLasSeccionesObligatorias } from '@/lib/sistema/secciones';
import type { Sistema, TipoBloque } from '@/lib/sistema/tipos';

let sistema: Sistema;

const tiposDelSistema = () => allBlocks(sistema).map((bloque) => bloque.type);

const columnasFijasDe = (tipo: TipoBloque) =>
  allBlocks(sistema)
    .filter((bloque) => bloque.type === tipo)
    .flatMap((bloque) => bloque.fields.filter(esColumnaFija).map((campo) => campo.key));

export const dadoUnSistemaDeSamiSinPiezas = () => {
  sistema = makeSystem('Mi sección');
};

export const dadoUnSistemaDeSamiConSoloUnaTablaDeStock = () => {
  sistema = makeSystem('Mi sección', ['inventory']);
};

export const dadoUnSistemaDeSamiSinPestanas = () => {
  sistema = makeSystem('Mi sección');
  sistema.sections = [];
};

export const cuandoSeCompletanLasSeccionesObligatorias = () => {
  completarLasSeccionesObligatorias(sistema);
};

export const entoncesTieneUnaSeccionDeCadaTipoObligatorio = () => {
  expect(tiposDelSistema()).toEqual([...TIPOS_OBLIGATORIOS]);
};

export const entoncesLaTablaDeStockSigueSiendoUnaSola = () => {
  expect(tiposDelSistema().filter((tipo) => tipo === 'inventory')).toHaveLength(1);
  expect(tiposDelSistema()[0]).toBe('inventory');
};

export const entoncesCadaSeccionTraeSusColumnasFijas = () => {
  expect(columnasFijasDe('inventory')).toContain('quantity');
  expect(columnasFijasDe('sales')).toEqual(
    expect.arrayContaining(['product', 'quantity', 'price', 'date', 'customer', 'credit']),
  );
  expect(columnasFijasDe('clients')).toEqual(['name']);
  expect(columnasFijasDe('expenses')).toEqual(
    expect.arrayContaining(['name', 'date', 'total', 'category']),
  );
};

export const entoncesLasSeccionesSumadasNoTienenFilas = () => {
  expect(allBlocks(sistema).flatMap((bloque) => bloque.rows)).toEqual([]);
};

export const entoncesLasVentasSeConectanConElStockYConLosClientes = () => {
  const ventas = allBlocks(sistema).find((bloque) => bloque.type === 'sales');
  const stock = allBlocks(sistema).find((bloque) => bloque.type === 'inventory');
  const clientes = allBlocks(sistema).find((bloque) => bloque.type === 'clients');
  expect(ventas?.fields.find((campo) => campo.key === 'product')?.target).toBe(stock?.id);
  expect(ventas?.fields.find((campo) => campo.key === 'customer')?.target).toBe(clientes?.id);
};

export const entoncesNoSumoNingunaSeccion = () => {
  expect(allBlocks(sistema)).toEqual([]);
};
