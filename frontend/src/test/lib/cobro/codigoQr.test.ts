import { describe, expect, it } from 'vitest';
import { matrizDelQr, trazoDelQr, type MatrizDelQr } from '@/lib/cobro/codigoQr';

const LADO_DE_LA_PRIMERA_VERSION = 21;
const PASO_ENTRE_VERSIONES = 4;
const LADO_DEL_PATRON_DE_POSICION = 7;

let matriz: MatrizDelQr;
let otraMatriz: MatrizDelQr;
let trazo: string;

describe('código QR del cobro', () => {
  it('es cuadrado y tiene el tamaño de una versión de QR', () => {
    cuandoSeArmaElQrDe('mi.alias.mp');

    entoncesEsCuadradoConTamanioDeVersionDeQr();
  });

  it('el mismo texto da siempre el mismo QR', () => {
    cuandoSeArmaElQrDeDosVecesElMismoTexto('mi.alias.mp');

    entoncesLosDosQrSonIguales();
  });

  it('un texto distinto da otro QR', () => {
    cuandoSeArmanLosQrDe('mi.alias.mp', 'otro.alias.mp');

    entoncesLosDosQrSonDistintos();
  });

  it('marca el patrón de posición en la esquina de arriba a la izquierda', () => {
    cuandoSeArmaElQrDe('mi.alias.mp');

    entoncesLaEsquinaTieneElPatronDePosicion();
  });

  it('un CVU de 22 dígitos entra en un QR', () => {
    cuandoSeArmaElQrDe('0000003100012345678901');

    entoncesEsCuadradoConTamanioDeVersionDeQr();
  });

  it('el trazo tiene un cuadrado por cada módulo oscuro', () => {
    cuandoSeArmaElTrazoDe('mi.alias.mp');

    entoncesElTrazoTieneUnCuadradoPorCadaModuloOscuro();
  });
});

function cuandoSeArmaElQrDe(texto: string) {
  matriz = matrizDelQr(texto);
}

function cuandoSeArmaElQrDeDosVecesElMismoTexto(texto: string) {
  matriz = matrizDelQr(texto);
  otraMatriz = matrizDelQr(texto);
}

function cuandoSeArmanLosQrDe(texto: string, otroTexto: string) {
  matriz = matrizDelQr(texto);
  otraMatriz = matrizDelQr(otroTexto);
}

function cuandoSeArmaElTrazoDe(texto: string) {
  matriz = matrizDelQr(texto);
  trazo = trazoDelQr(matriz);
}

function entoncesEsCuadradoConTamanioDeVersionDeQr() {
  expect(matriz.every((fila) => fila.length === matriz.length)).toBe(true);
  expect((matriz.length - LADO_DE_LA_PRIMERA_VERSION) % PASO_ENTRE_VERSIONES).toBe(0);
}

function entoncesLosDosQrSonIguales() {
  expect(otraMatriz).toEqual(matriz);
}

function entoncesLosDosQrSonDistintos() {
  expect(otraMatriz).not.toEqual(matriz);
}

function entoncesLaEsquinaTieneElPatronDePosicion() {
  const [primeraFila = []] = matriz;
  expect(primeraFila.slice(0, LADO_DEL_PATRON_DE_POSICION).every(Boolean)).toBe(true);
  expect(primeraFila[LADO_DEL_PATRON_DE_POSICION]).toBe(false);
}

function entoncesElTrazoTieneUnCuadradoPorCadaModuloOscuro() {
  const oscuros = matriz.flat().filter(Boolean).length;
  expect(trazo.split('M').length - 1).toBe(oscuros);
}
