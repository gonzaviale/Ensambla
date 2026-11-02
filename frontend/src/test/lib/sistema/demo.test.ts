import { describe, expect, it } from 'vitest';
import { analizar } from '@/lib/sistema/alertas';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import { allBlocks } from '@/lib/sistema/core';
import { zonaDeLaPersona } from '@/mocks/db/zona';

const HOY = '2026-10-31';

describe('crearAlmacenCargado', () => {
  const sistema = crearAlmacenCargado(HOY);
  const bloque = (tipo: string) => allBlocks(sistema).find((item) => item.type === tipo);

  it('trae muchos productos, ventas y clientes', () => {
    expect(bloque('inventory')?.rows.length).toBeGreaterThanOrEqual(25);
    expect(bloque('sales')?.rows.length).toBeGreaterThan(500);
    expect(bloque('clients')?.rows.length).toBeGreaterThanOrEqual(10);
  });

  it('las ventas apuntan a productos que existen', () => {
    const productos = new Set(bloque('inventory')?.rows.map((fila) => fila.id));
    expect(bloque('sales')?.rows.every((venta) => productos.has(String(venta.product)))).toBe(true);
  });

  it('genera avisos de todos los tipos', () => {
    const avisos = analizar({ sistemas: [sistema], zona: zonaDeLaPersona(), hoy: HOY });
    const tipos = new Set(avisos.map((aviso) => aviso.tipo));

    expect(avisos.length).toBeGreaterThanOrEqual(12);
    for (const tipo of [
      'stock',
      'estancado',
      'precio_zona',
      'muy_vendido',
      'combo',
      'consumo_zona',
    ]) {
      expect(tipos).toContain(tipo);
    }
  });
});
