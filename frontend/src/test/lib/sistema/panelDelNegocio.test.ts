import { describe, expect, it } from 'vitest';
import { makeTemplate } from '@/lib/sistema/core';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import { sistemaDelPanel, tieneVentas } from '@/lib/sistema/panelDelNegocio';

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
