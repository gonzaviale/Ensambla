import { describe, expect, it } from 'vitest';
import { conNuevasPrimero, registrosNuevos } from '@/lib/sistema/tandas';
import type { Fila } from '@/lib/sistema/tipos';

const fila = (id: string, extra: Partial<Fila> = {}): Fila => ({ id, ...extra });

describe('conNuevasPrimero', () => {
  it('pone los registros nuevos al principio y deja el resto en su orden', () => {
    const filas = [fila('a'), fila('b'), fila('c'), fila('d')];

    const ordenadas = conNuevasPrimero(filas, new Set(['c', 'd']));

    expect(ordenadas.map((item) => item.id)).toEqual(['c', 'd', 'a', 'b']);
  });

  it('sin registros nuevos devuelve las mismas filas', () => {
    const filas = [fila('a'), fila('b')];

    expect(conNuevasPrimero(filas, new Set())).toBe(filas);
  });
});

describe('registrosNuevos', () => {
  it('son los reales que no estaban guardados: ni los de ejemplo ni los que ya existían', () => {
    const bloques = [
      { rows: [fila('guardada'), fila('ejemplo', { __example: true }), fila('nueva')] },
      { rows: [fila('otra-nueva')] },
    ];

    const nuevas = registrosNuevos(bloques, new Set(['guardada']));

    expect([...nuevas]).toEqual(['nueva', 'otra-nueva']);
  });

  it('al armar un cuaderno nuevo todo lo real es nuevo', () => {
    const nuevas = registrosNuevos([{ rows: [fila('a'), fila('b')] }], new Set());

    expect(nuevas.size).toBe(2);
  });
});
