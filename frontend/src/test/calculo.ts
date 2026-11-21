import { expect } from 'vitest';

// Para las funciones puras: se calcula algo (cuando) y se mira el resultado (entonces).
let resultado: unknown;
let fallo: unknown;

export function cuandoSeCalcula<T>(calculo: () => T) {
  resultado = calculo();
}

export function cuandoSeIntentaCalcular(calculo: () => unknown) {
  try {
    calculo();
    fallo = undefined;
  } catch (error) {
    fallo = error;
  }
}

export function entoncesElResultadoEs(esperado: unknown) {
  expect(resultado).toEqual(esperado);
}

export function entoncesElResultadoEsIdenticoA(esperado: unknown) {
  expect(resultado).toBe(esperado);
}

export function entoncesElResultadoCoincideCon(esperado: object) {
  expect(resultado).toMatchObject(esperado);
}

export function entoncesElResultadoContiene(esperado: unknown) {
  expect(resultado).toContain(esperado);
}

export function entoncesElResultadoTieneLongitud(longitud: number) {
  expect(resultado).toHaveLength(longitud);
}

export function entoncesElResultadoExiste() {
  expect(resultado).toBeDefined();
}

export function entoncesElResultadoNoExiste() {
  expect(resultado == null).toBe(true);
}

export function entoncesElResultadoDe<T>(leer: (calculado: T) => unknown, esperado: unknown) {
  expect(leer(resultado as T)).toEqual(esperado);
}

export function entoncesElCalculoFalla() {
  expect(fallo).toBeInstanceOf(Error);
}
