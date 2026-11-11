import { describe, expect, it } from 'vitest';
import { makeTemplate } from '@/lib/sistema/core';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import {
  bloqueDeVentas,
  pesos,
  pesosConSigno,
  porcentaje,
  sistemaDelPanel,
  tieneVentas,
} from '@/lib/sistema/panelDelNegocio';

const HOY = '2026-10-19';

describe('sistemaDelPanel', () => {
  it('sin cuadernos no hay panel', () => {
    expect(sistemaDelPanel([])).toBeNull();
  });

  it('elige el primer cuaderno con ventas aunque no sea el primero de la lista', () => {
    const vacio = makeTemplate('shop', {}, true);
    const cargado = crearAlmacenCargado(HOY);

    expect(sistemaDelPanel([vacio, cargado])).toBe(cargado);
  });

  it('si ninguno tiene ventas se queda con el primero, para explicar cómo cargarlas', () => {
    const primero = makeTemplate('shop', {}, true);
    const segundo = makeTemplate('shop', {}, true);

    expect(sistemaDelPanel([primero, segundo])).toBe(primero);
  });

  it('las filas de ejemplo no son ventas', () => {
    expect(tieneVentas(makeTemplate('shop', {}, true))).toBe(false);
    expect(tieneVentas(crearAlmacenCargado(HOY))).toBe(true);
  });
});

describe('bloqueDeVentas', () => {
  it('es la tabla de ventas del cuaderno, y nada si no tiene una', () => {
    expect(bloqueDeVentas(crearAlmacenCargado(HOY))?.type).toBe('sales');
    expect(bloqueDeVentas(makeTemplate('business', {}, true))).toBeUndefined();
  });
});

describe('formato del panel', () => {
  it('los pesos llevan el punto de los miles y el signo menos delante del peso', () => {
    expect(pesos(839750)).toBe('$839.750');
    expect(pesos(0)).toBe('$0');
    expect(pesos(-12000.4)).toBe('-$12.000');
  });

  it('lo que sobra lleva más, lo que falta lleva menos y el cero no lleva nada', () => {
    expect(pesosConSigno(110000)).toBe('+$110.000');
    expect(pesosConSigno(-2000)).toBe('-$2.000');
    expect(pesosConSigno(0)).toBe('$0');
  });

  it('el porcentaje usa la coma decimal de es-AR, con un solo decimal', () => {
    expect(porcentaje(25.9)).toBe('25,9 %');
    expect(porcentaje(20)).toBe('20 %');
    expect(porcentaje(20.64)).toBe('20,6 %');
  });
});
