import { describe, expect, it } from 'vitest';
import {
  agrupacionesDe,
  ajustarALaFuente,
  calculosDe,
  definicionInicial,
  etiquetaDeCalculo,
  formatoDelValor,
  periodosDe,
  permiteFiado,
  resumenDeLaReceta,
  textoDeDondeSale,
} from '@/lib/metricas/metricasAMedida';
import type { ResultadoDeMetrica } from '@/types/metricas';

const resultado = (deDondeSale: Partial<ResultadoDeMetrica['deDondeSale']> = {}): ResultadoDeMetrica => ({
  formato: 'MONEDA',
  valor: 100,
  filas: [],
  motivo: null,
  deDondeSale: { fuente: 'VENTAS', filasUsadas: 3, filasSinDato: 0, desde: '2026-11-07', hasta: '2026-11-13', ...deDondeSale },
});

describe('metricasAMedida', () => {
  it('ofrece sólo lo que cada tabla puede calcular', () => {
    expect(calculosDe('GASTOS')).not.toContain('UNIDADES');
    expect(calculosDe('VENTAS')).toContain('UNIDADES');
    expect(calculosDe('VENTAS')).toEqual(expect.arrayContaining(['GANANCIA', 'COSTO']));
    expect(calculosDe('GASTOS')).not.toContain('GANANCIA');
    expect(calculosDe('INVENTARIO')).not.toContain('COSTO');
    expect(agrupacionesDe('GASTOS')).toContain('CATEGORIA');
    expect(agrupacionesDe('VENTAS')).not.toContain('CATEGORIA');
    expect(agrupacionesDe('INVENTARIO')).toEqual(['NINGUNA', 'PRODUCTO']);
    expect(periodosDe('INVENTARIO')).toEqual(['TODO']);
    expect(periodosDe('VENTAS')).toHaveLength(4);
    expect(permiteFiado('VENTAS')).toBe(true);
    expect(permiteFiado('GASTOS')).toBe(false);
  });

  it('al cambiar de tabla conserva lo que sigue valiendo y corrige lo que no', () => {
    const ventas = { ...definicionInicial(), calculo: 'UNIDADES', agrupar: 'CLIENTE', soloFiado: true } as const;
    const gastos = ajustarALaFuente(ventas, 'GASTOS');
    expect(gastos).toMatchObject({ fuente: 'GASTOS', calculo: 'SUMA', agrupar: 'NINGUNA', soloFiado: false, periodo: 'MES' });
    const inventario = ajustarALaFuente(ventas, 'INVENTARIO');
    expect(inventario.periodo).toBe('TODO');
    const mismaTabla = ajustarALaFuente({ ...ventas, agrupar: 'PRODUCTO' }, 'VENTAS');
    expect(mismaTabla).toMatchObject({ calculo: 'UNIDADES', agrupar: 'PRODUCTO', soloFiado: true });
  });

  it('escribe los cálculos con las palabras de cada tabla', () => {
    expect(etiquetaDeCalculo('VENTAS', 'SUMA')).toBe('Total de ventas');
    expect(etiquetaDeCalculo('INVENTARIO', 'SUMA')).toBe('Valor del stock (a costo)');
    expect(etiquetaDeCalculo('GASTOS', 'PROMEDIO')).toBe('Promedio por gasto');
    expect(etiquetaDeCalculo('VENTAS', 'CONTEO')).toBe('Cuántas ventas hay');
    expect(etiquetaDeCalculo('VENTAS', 'UNIDADES')).toBe('Unidades vendidas');
    expect(etiquetaDeCalculo('INVENTARIO', 'UNIDADES')).toBe('Unidades en stock');
    expect(etiquetaDeCalculo('VENTAS', 'GANANCIA')).toBe('Ganancia');
    expect(etiquetaDeCalculo('VENTAS', 'COSTO')).toBe('Costo de lo que vendiste');
  });

  it('resume la receta en una frase', () => {
    const base = { ...definicionInicial(), agrupar: 'PRODUCTO', periodo: 'SEMANA' } as const;
    expect(resumenDeLaReceta(base)).toBe('Total de ventas · Por producto · Esta semana');
    expect(resumenDeLaReceta({ ...base, soloFiado: true })).toContain('(fiado)');
  });

  it('formatea el número según sea plata o cantidad', () => {
    expect(formatoDelValor(1500, 'MONEDA')).toContain('1.500');
    expect(formatoDelValor(1500, 'NUMERO')).toBe('1.500');
  });

  it('cuenta de dónde sale el número', () => {
    expect(textoDeDondeSale(resultado())).toBe('Sale de tus ventas, del 7/11 al 13/11. Se usaron 3 ventas.');
    expect(textoDeDondeSale(resultado({ desde: '2026-11-13', filasUsadas: 1 }))).toBe(
      'Sale de tus ventas de hoy (13/11). Se usó 1 venta.',
    );
    expect(textoDeDondeSale(resultado({ desde: null, hasta: null, filasSinDato: 1 }))).toContain(
      '1 venta quedó afuera porque le falta un dato.',
    );
    expect(textoDeDondeSale(resultado({ filasUsadas: 0, filasSinDato: 2 }))).toContain(
      '2 ventas quedaron afuera porque les falta un dato.',
    );
  });
});
