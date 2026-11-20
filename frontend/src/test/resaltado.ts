import { afterEach, expect, vi } from 'vitest';
import { resaltar } from '@/lib/sistema/resaltar';

const MARCO = '.resaltado-marco';

let elemento: HTMLElement;

afterEach(() => {
  vi.useRealTimers();
  document.body.innerHTML = '';
});

export const dadoQueElTiempoLoManejaLaPrueba = () => {
  vi.useFakeTimers();
};

export const dadoUnElementoEnPantalla = () => {
  elemento = document.createElement('div');
  document.body.append(elemento);
};

export const cuandoSeResaltaElElemento = (opciones?: { duracion: number }) => {
  resaltar(elemento, opciones);
};

export const cuandoSeResaltaDosVecesElMismoElemento = () => {
  resaltar(elemento);
  resaltar(elemento);
};

export const cuandoSeResaltaUnElementoInexistente = () => {
  resaltar(null);
};

export const cuandoPasanLosMilisegundos = (milisegundos: number) => {
  vi.advanceTimersByTime(milisegundos);
};

export const entoncesHayUnSoloMarcoSobreElElemento = () => {
  expect(document.querySelectorAll(MARCO)).toHaveLength(1);
};

export const entoncesElMarcoEstaSaliendo = () => {
  expect(document.querySelector(`${MARCO}.saliendo`)).not.toBeNull();
};

export const entoncesNoQuedaNingunMarco = () => {
  expect(document.querySelector(MARCO)).toBeNull();
};
