import { describe, expect, it } from 'vitest';
import { conSigno, diapositivasDelAviso, hayVentasParaComparar } from '@/lib/sistema/rentabilidad';
import type { ResumenInicio } from '@/types/resumenInicio';
import type { AvisoEconomico } from '@/types/avisoEconomico';

const aviso = (datos: Partial<AvisoEconomico> = {}): AvisoEconomico => ({
  prioridad: 'ALTA',
  tendencia: 'EMPEORA',
  categoria: 'Rentabilidad',
  titulo: 'Las ventas subieron 12 %, pero la ganancia bajó 4 %',
  explicacion: 'Vendiste más, pero te quedó menos ganancia.',
  variacionVentas: 12,
  variacionResultadoBruto: -4,
  productos: ['Aceite', 'Leche'],
  cambios: [
    { nombre: 'Aceite', monto: -4200 },
    { nombre: 'Leche', monto: -2600 },
  ],
  confianza: 'MEDIA',
  ...datos,
});

describe('conSigno', () => {
  it('pone el signo y el porcentaje', () => {
    expect(conSigno(12)).toBe('+12 %');
    expect(conSigno(-4)).toBe('−4 %');
    expect(conSigno(0)).toBe('0 %');
  });
});

describe('diapositivasDelAviso', () => {
  it('separa ventas, ganancia y productos', () => {
    const diapositivas = diapositivasDelAviso(aviso(), 'n1');

    expect(diapositivas.map((item) => item.tipo)).toEqual(['ventas', 'ganancia', 'productos']);
    expect(diapositivas[0]).toMatchObject({ cambio: 12 });
    expect(diapositivas[1]).toMatchObject({ cambio: -4 });
  });

  it('sin costos avisa que faltan y sin productos no arma esa tarjeta', () => {
    const diapositivas = diapositivasDelAviso(
      aviso({ variacionResultadoBruto: null, productos: [], cambios: [] }),
      'n1',
    );

    expect(diapositivas.map((item) => item.tipo)).toEqual(['ventas', 'costos']);
  });

  it('la clave cambia con el negocio y con el aviso', () => {
    const uno = diapositivasDelAviso(aviso(), 'n1')[0]?.clave;
    expect(diapositivasDelAviso(aviso(), 'n2')[0]?.clave).not.toBe(uno);
    expect(diapositivasDelAviso(aviso({ titulo: 'Otro' }), 'n1')[0]?.clave).not.toBe(uno);
  });
});

const periodo = (ingresos: number, ingresosAnteriores: number) => ({
  ingresos,
  unidades: 0,
  cantidad: 0,
  sinPrecio: 0,
  ingresosAnteriores,
  fiado: 0,
  ventasFiadas: 0,
});

const dadoResumen = (semana: [number, number], diasConVentas: number): ResumenInicio => ({
  sistema: 'x',
  hoy: '2026-11-16',
  hayVentas: true,
  hayProductos: true,
  ventas: { hoy: periodo(0, 0), semana: periodo(...semana), mes: periodo(...semana) },
  ventasPorDia: Array.from({ length: 14 }, (_, dia) => ({
    fecha: `2026-11-${String(dia + 1).padStart(2, '0')}`,
    ingresos: dia < diasConVentas ? 1000 : 0,
  })),
  ultimoDiaConVentas: null,
  masVendido: null,
  faltantes: { total: 0, primeros: [] },
  parados: [],
});

describe('hayVentasParaComparar', () => {
  it('con ventas esta semana y la anterior, en varios días, hay con qué comparar', () => {
    expect(hayVentasParaComparar(dadoResumen([5000, 4000], 6))).toBe(true);
  });

  it('con una sola venta en un solo día no hay con qué comparar', () => {
    expect(hayVentasParaComparar(dadoResumen([1500, 0], 1))).toBe(false);
  });

  it('sin ventas la semana anterior no hay con qué comparar', () => {
    expect(hayVentasParaComparar(dadoResumen([5000, 0], 4))).toBe(false);
  });

  it('con ventas de las dos semanas pero un solo día con ventas no alcanza', () => {
    expect(hayVentasParaComparar(dadoResumen([5000, 4000], 1))).toBe(false);
  });
});
