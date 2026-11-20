import { describe, expect, it } from 'vitest';
import { valorCorto } from '@/lib/metricas/graficos';

describe('valorCorto', () => {
  it('abrevia los miles y los millones para que muchas barras no se encimen', () => {
    expect(valorCorto(44000, 'MONEDA')).toBe('$\u00a044 mil');
    expect(valorCorto(8500, 'MONEDA')).toBe('$\u00a08,5 mil');
    expect(valorCorto(1200000, 'MONEDA')).toBe('$\u00a01,2 M');
  });

  it('deja como están los importes chicos y las cantidades', () => {
    expect(valorCorto(900, 'MONEDA')).toBe('$\u00a0900');
    expect(valorCorto(1500, 'NUMERO')).toBe('1.500');
  });
});
