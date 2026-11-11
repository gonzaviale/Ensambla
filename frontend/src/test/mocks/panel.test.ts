import { describe, expect, it } from 'vitest';
import { makeTemplate } from '@/lib/sistema/core';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import { panelDe } from '@/mocks/db/panel';

const HOY = '2026-10-19';

describe('panel de la home simulado', () => {
  it('arma las semanas desde el primer día del período, la última puede ser más corta', () => {
    const panel = panelDe(crearAlmacenCargado(HOY), HOY, 30);

    expect(panel.dias).toBe(30);
    expect(panel.desde).toBe('2026-09-20');
    expect(panel.hasta).toBe(HOY);
    expect(panel.flujoDeCaja.map((semana) => semana.desde)).toEqual([
      '2026-09-20',
      '2026-09-27',
      '2026-10-04',
      '2026-10-11',
      '2026-10-18',
    ]);
    expect(panel.flujoDeCaja.at(-1)?.hasta).toBe(HOY);
  });

  it('lo fiado no es una entrada: se informa aparte y no suma al saldo', () => {
    const panel = panelDe(crearAlmacenCargado(HOY), HOY, 30);
    const metricas = panel.metricas;
    if (!metricas) throw new Error('Debería haber métricas');
    const entradas = panel.flujoDeCaja.reduce((total, semana) => total + semana.entradas, 0);

    expect(metricas.teDeben).toBeGreaterThan(0);
    expect(entradas).toBeCloseTo(metricas.vendiste - metricas.teDeben, 2);
  });

  it('lo que queda es el saldo con el que cierra el flujo de caja', () => {
    const panel = panelDe(crearAlmacenCargado(HOY), HOY, 30);

    expect(panel.metricas?.queda).toBeCloseTo(panel.flujoDeCaja.at(-1)?.saldoAcumulado ?? NaN, 2);
  });

  it('con menos días mira un período más corto', () => {
    const panel = panelDe(crearAlmacenCargado(HOY), HOY, 7);

    expect(panel.desde).toBe('2026-10-13');
    expect(panel.flujoDeCaja).toHaveLength(1);
  });

  it('sin ventas en el período no hay métricas ni semanas, y las filas de ejemplo no cuentan', () => {
    const panel = panelDe(makeTemplate('shop', {}, true), HOY, 30);

    expect(panel.hayDatos).toBe(false);
    expect(panel.metricas).toBeNull();
    expect(panel.flujoDeCaja).toEqual([]);
  });
});
