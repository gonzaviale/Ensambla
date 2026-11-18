import { expect } from 'vitest';
import { haceCuanto } from '@/components/sistema/avisos/presentacion';

export const dadoUnAvisoCreadoEl = (creadoEn: string | undefined) => creadoEn;

export const cuandoSeConsultaDesde = (creadoEn: string | undefined, ahora: string) =>
  haceCuanto(creadoEn, new Date(ahora));

export const entoncesDice = (texto: string | undefined, esperado: string | undefined) =>
  expect(texto).toBe(esperado);
