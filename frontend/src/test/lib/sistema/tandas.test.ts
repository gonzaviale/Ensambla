import { describe, expect, it } from 'vitest';
import { conNuevasPrimero, registrosNuevos } from '@/lib/sistema/tandas';
import type { Fila } from '@/lib/sistema/tipos';

const fila = (id: string, extra: Partial<Fila> = {}): Fila => ({ id, ...extra });

let filas: Fila[];
let ordenadas: Fila[];
let nuevas: Set<string>;

describe('conNuevasPrimero', () => {
  it('pone los registros nuevos al principio y deja el resto en su orden', () => {
    dadoCuatroFilas();

    cuandoSeOrdenanConNuevasPrimero(['c', 'd']);

    entoncesElOrdenEs(['c', 'd', 'a', 'b']);
  });

  it('sin registros nuevos devuelve las mismas filas', () => {
    dadoDosFilas();

    cuandoSeOrdenanConNuevasPrimero([]);

    entoncesSeDevuelvenLasMismasFilas();
  });
});

describe('registrosNuevos', () => {
  it('son los reales que no estaban guardados: ni los de ejemplo ni los que ya existían', () => {
    cuandoSeBuscanLosNuevosEnDosBloques();

    entoncesLosNuevosSon(['nueva', 'otra-nueva']);
  });

  it('al armar una sección nueva todo lo real es nuevo', () => {
    cuandoSeBuscanLosNuevosDeUnBloqueSinNadaGuardado();

    entoncesHayNuevos(2);
  });
});

function dadoCuatroFilas() {
  filas = [fila('a'), fila('b'), fila('c'), fila('d')];
}

function dadoDosFilas() {
  filas = [fila('a'), fila('b')];
}

function cuandoSeOrdenanConNuevasPrimero(ids: string[]) {
  ordenadas = conNuevasPrimero(filas, new Set(ids));
}

function cuandoSeBuscanLosNuevosEnDosBloques() {
  const bloques = [
    { rows: [fila('guardada'), fila('ejemplo', { __example: true }), fila('nueva')] },
    { rows: [fila('otra-nueva')] },
  ];
  nuevas = registrosNuevos(bloques, new Set(['guardada']));
}

function cuandoSeBuscanLosNuevosDeUnBloqueSinNadaGuardado() {
  nuevas = registrosNuevos([{ rows: [fila('a'), fila('b')] }], new Set());
}

function entoncesElOrdenEs(ids: string[]) {
  expect(ordenadas.map((item) => item.id)).toEqual(ids);
}

function entoncesSeDevuelvenLasMismasFilas() {
  expect(ordenadas).toBe(filas);
}

function entoncesLosNuevosSon(ids: string[]) {
  expect([...nuevas]).toEqual(ids);
}

function entoncesHayNuevos(cantidad: number) {
  expect(nuevas.size).toBe(cantidad);
}
