import { describe, expect, it } from 'vitest';
import {
  comparacionConElPeriodoAnterior,
  cuandoFue,
  sistemasConDatos,
  diaCompleto,
  diasDelGrafico,
  etiquetaDelDia,
  textoUnidades,
} from '@/lib/sistema/metricasInicio';
import { makeSystem } from '@/lib/sistema/core';
import { formatoPesos } from '@/lib/sistema/formato';
import type { Sistema } from '@/lib/sistema/tipos';

const conFilas = (nombre: string, actualizadoEn: string, ejemplo: boolean): Sistema => {
  const sistema = makeSystem(nombre, ['inventory', 'sales'], false);
  const inventario = sistema.sections[0]?.blocks[0];
  if (inventario)
    inventario.rows = [{ id: 'p1', name: 'Yerba', ...(ejemplo ? { __example: true } : {}) }];
  return { ...sistema, updatedAt: actualizadoEn };
};

describe('métricas del inicio', () => {
  it('sólo cuenta las secciones con filas propias y deja primera la más reciente', () => {
    const viejo = conFilas('Viejo', '2026-10-01T00:00:00.000Z', false);
    const reciente = conFilas('Reciente', '2026-10-20T00:00:00.000Z', false);
    const deEjemplo = conFilas('De ejemplo', '2026-10-25T00:00:00.000Z', true);
    const vacio = makeSystem('Vacío', ['inventory', 'sales'], false);

    const resultado = sistemasConDatos([viejo, deEjemplo, vacio, reciente]);

    expect(resultado.map((sistema) => sistema.name)).toEqual(['Reciente', 'Viejo']);
  });

  it('compara con el mismo día de la semana pasada, con la semana pasada y con el mes pasado', () => {
    expect(comparacionConElPeriodoAnterior('hoy', 10200, 3400, '2026-10-19')).toBe(
      `${formatoPesos(6800)} más que el lunes pasado`,
    );
    expect(comparacionConElPeriodoAnterior('semana', 1000, 3400, '2026-10-19')).toBe(
      `${formatoPesos(2400)} menos que la semana pasada`,
    );
    expect(comparacionConElPeriodoAnterior('mes', 3400, 3400, '2026-10-19')).toBe(
      'Igual que el mes pasado',
    );
  });

  it('no compara cuando el período anterior no vendió nada', () => {
    expect(comparacionConElPeriodoAnterior('semana', 5000, 0, '2026-10-19')).toBeNull();
  });

  it('cuenta cuándo fue la última venta', () => {
    expect(cuandoFue('2026-10-18', '2026-10-19')).toBe('ayer');
    expect(cuandoFue('2026-10-12', '2026-10-19')).toBe('el lunes 12/10');
  });

  it('escribe las unidades en singular y plural', () => {
    expect(textoUnidades(1)).toBe('1 unidad');
    expect(textoUnidades(2.5)).toBe('2,5 unidades');
  });

  it('nombra los días del gráfico corto en el eje y completo en el detalle', () => {
    expect(etiquetaDelDia('2026-10-19')).toBe('lun 19');
    expect(diaCompleto('2026-11-01')).toBe('domingo 1/11');
  });

  it('dibuja treinta días para el mes y siete para hoy o la semana', () => {
    expect(diasDelGrafico('mes')).toBe(30);
    expect(diasDelGrafico('semana')).toBe(7);
    expect(diasDelGrafico('hoy')).toBe(7);
  });
});
