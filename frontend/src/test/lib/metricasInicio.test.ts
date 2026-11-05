import { describe, expect, it } from 'vitest';
import {
  comparacionConLaSemanaPasada,
  cuandoFue,
  cuadernosConDatos,
  textoUnidades,
} from '@/lib/sistema/metricasInicio';
import { makeSystem } from '@/lib/sistema/core';
import { formatoPesos } from '@/lib/sistema/formato';
import type { Sistema } from '@/lib/sistema/tipos';

const conFilas = (nombre: string, actualizadoEn: string, ejemplo: boolean): Sistema => {
  const sistema = makeSystem(nombre, ['inventory', 'sales'], false);
  const inventario = sistema.sections[0]?.blocks[0];
  if (inventario) inventario.rows = [{ id: 'p1', name: 'Yerba', ...(ejemplo ? { __example: true } : {}) }];
  return { ...sistema, updatedAt: actualizadoEn };
};

describe('métricas del inicio', () => {
  it('sólo cuenta los cuadernos con filas propias y deja primero el más reciente', () => {
    const viejo = conFilas('Viejo', '2026-10-01T00:00:00.000Z', false);
    const reciente = conFilas('Reciente', '2026-10-20T00:00:00.000Z', false);
    const deEjemplo = conFilas('De ejemplo', '2026-10-25T00:00:00.000Z', true);
    const vacio = makeSystem('Vacío', ['inventory', 'sales'], false);

    const resultado = cuadernosConDatos([viejo, deEjemplo, vacio, reciente]);

    expect(resultado.map((cuaderno) => cuaderno.name)).toEqual(['Reciente', 'Viejo']);
  });

  it('compara con el mismo día de la semana pasada', () => {
    expect(comparacionConLaSemanaPasada(10200, 3400, '2026-10-19')).toBe(
      `${formatoPesos(6800)} más que el lunes pasado`,
    );
    expect(comparacionConLaSemanaPasada(1000, 3400, '2026-10-19')).toBe(
      `${formatoPesos(2400)} menos que el lunes pasado`,
    );
    expect(comparacionConLaSemanaPasada(3400, 3400, '2026-10-19')).toBe('Igual que el lunes pasado');
  });

  it('no compara cuando la semana pasada no vendió nada', () => {
    expect(comparacionConLaSemanaPasada(5000, 0, '2026-10-19')).toBeNull();
  });

  it('cuenta cuándo fue la última venta', () => {
    expect(cuandoFue('2026-10-18', '2026-10-19')).toBe('ayer');
    expect(cuandoFue('2026-10-12', '2026-10-19')).toBe('el lunes 12/10');
  });

  it('escribe las unidades en singular y plural', () => {
    expect(textoUnidades(1)).toBe('1 unidad');
    expect(textoUnidades(2.5)).toBe('2,5 unidades');
  });
});
