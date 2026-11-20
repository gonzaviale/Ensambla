import { describe, expect, it } from 'vitest';
import {
  hechosDespuesDeElegir,
  pasosSinContestar,
  sePuedeIrAlPaso,
} from '@/lib/metricas/pasosDeLaReceta';

describe('Los pasos de «Armala vos»', () => {
  it('al elegir en un paso lo suma a los contestados', () => {
    expect(hechosDespuesDeElegir([0], 1, false)).toEqual([0, 1]);
  });

  it('si cambia lo que se mira, hay que volver a elegir lo demás', () => {
    expect(hechosDespuesDeElegir([0, 1, 2, 3], 0, true)).toEqual([0]);
  });

  it('elegir de nuevo en el mismo paso no lo repite', () => {
    expect(hechosDespuesDeElegir([0, 1], 1, false)).toEqual([0, 1]);
  });

  it('dice qué preguntas faltan contestar', () => {
    expect(pasosSinContestar([0, 2])).toEqual(['Qué saber', 'Cuándo']);
  });

  it('sin elegir qué mirar sólo se puede estar en el primer paso', () => {
    expect(sePuedeIrAlPaso([], 0)).toBe(true);
    expect(sePuedeIrAlPaso([], 1)).toBe(false);
  });

  it('los pasos van en orden: no se salta al tercero sin contestar el segundo', () => {
    expect(sePuedeIrAlPaso([0], 1)).toBe(true);
    expect(sePuedeIrAlPaso([0], 2)).toBe(false);
  });

  it('al nombre se llega cuando las cuatro preguntas están contestadas', () => {
    expect(sePuedeIrAlPaso([0, 1, 2], 4)).toBe(false);
    expect(sePuedeIrAlPaso([0, 1, 2, 3], 4)).toBe(true);
  });
});
