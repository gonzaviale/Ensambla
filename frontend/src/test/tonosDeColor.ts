import { expect } from 'vitest';
import { CONCEPTOS_DE_COLOR, TONOS, tonoDe, tonoPorPosicion, type ConceptoDeColor } from '@/lib/sistema/tonos';

export const MEDIOS_DE_PAGO: ConceptoDeColor[] = ['efectivo', 'transferencia', 'tarjeta'];

export const PESTANAS_DE_CAJA: ConceptoDeColor[] = [
  'pestanaResumenDelDia',
  'pestanaCobros',
  'pestanaCajaAbierta',
  'pestanaCierresGuardados',
];

export const MODOS_DE_VENDER: ConceptoDeColor[] = ['venderEscanear', 'venderBuscar', 'venderFavoritos'];

let tonosLeidos: string[] = [];

export function cuandoSeLeenLosTonosDeTodosLosConceptos() {
  tonosLeidos = CONCEPTOS_DE_COLOR.map(tonoDe);
}

export function cuandoSeLeenLosTonosDe(conceptos: ConceptoDeColor[]) {
  tonosLeidos = conceptos.map(tonoDe);
}

export function cuandoSeLeenLosTonosDeLasPosiciones(desde: number, hasta: number) {
  tonosLeidos = posicionesEntre(desde, hasta).map(tonoPorPosicion);
}

export function entoncesTodosLosTonosSonDeLaMarca() {
  expect(tonosLeidos.every(esTonoDeLaMarca)).toBe(true);
}

export function entoncesNingunTonoSeRepite() {
  expect(new Set(tonosLeidos).size).toBe(tonosLeidos.length);
}

export function entoncesLosTonosSon(esperados: string[]) {
  expect(tonosLeidos).toEqual(esperados);
}

function esTonoDeLaMarca(tono: string) {
  return (TONOS as readonly string[]).includes(tono);
}

function posicionesEntre(desde: number, hasta: number) {
  return Array.from({ length: hasta - desde + 1 }, (_, paso) => desde + paso);
}
