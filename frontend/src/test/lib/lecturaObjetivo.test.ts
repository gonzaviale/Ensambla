import { describe, expect, it } from 'vitest';
import { alcanceDeLaMeta, leerObjetivo } from '@/lib/lecturaObjetivo';

const NEGOCIO = { hayVentas: true, vendiste: 1_000_000, queda: 250_000 };

describe('leerObjetivo', () => {
  it.each([
    ['Quiero ganar $400.000 por mes sin subir demasiado los precios', 400_000],
    ['Quiero ganar 400 mil', 400_000],
    ['quiero ganar 400k', 400_000],
    ['Quiero ganar 1,5 millones', 1_500_000],
    ['quiero ganar un palo por mes', 1_000_000],
    ['quiero ganar medio millón', 500_000],
    ['quiero ganar 300 lucas', 300_000],
    ['Quiero ganar 400000', 400_000],
    ['Quiero poder retirar $150.000 por mes sin afectar el negocio', 150_000],
    ['Quiero ganar $100.000 por semana', 429_000],
    ['ganar 20 mil por día', 600_000],
    ['ganar 6 millones por año', 500_000],
    ['Para 2026 quiero ganar 400 mil', 400_000],
    ['Quiero subir la yerba a $4.000 y ganar $500.000', 500_000],
    ['Quiero ganar $500.000 sin subir la yerba a más de $4.000', 500_000],
    ['Vender 30 unidades por día y ganar $250K', 250_000],
    ['Subir 15 % y llegar a 300 mil antes del día 20', 300_000],
    ['Quiero que el negocio me deje $300.000', 300_000],
    ['ganar 400 mil por mes vendiendo 30 unidades por día', 400_000],
  ])('«%s» → meta %d', (texto, meta) => {
    expect(leerObjetivo(texto).meta).toBe(meta);
  });

  it('una meta de ventas la pasa a ganancia con lo que queda hoy de cada venta', () => {
    const lectura = leerObjetivo('Quiero vender $2.000.000 por mes', NEGOCIO);
    expect(lectura.tipo).toBe('VENDER');
    expect(lectura.meta).toBe(500_000);
    expect(lectura.explicacion).toMatch(/25 %/);
  });

  it('«en ventas» después del monto también es vender', () => {
    expect(leerObjetivo('Quiero llegar a 1 millón en ventas', NEGOCIO).meta).toBe(250_000);
  });

  it('una meta de ventas sin datos no inventa la ganancia', () => {
    const lectura = leerObjetivo('Quiero vender $2.000.000 por mes');
    expect(lectura.meta).toBeNull();
    expect(lectura.avisos).toContain('Escribí abajo cuánto querés que te quede por mes, en pesos.');
  });

  it('crecer un porcentaje se calcula sobre lo que queda hoy', () => {
    expect(leerObjetivo('Quiero ganar un 20 % más', NEGOCIO).meta).toBe(300_000);
    expect(leerObjetivo('Quiero duplicar lo que gano', NEGOCIO).meta).toBe(500_000);
    expect(leerObjetivo('Quiero ganar un 20 % más').meta).toBeNull();
  });

  it('sin monto no inventa una meta', () => {
    for (const texto of [
      'Quiero recuperar mis gastos antes del día 20',
      'Quiero crecer',
      'Quiero $5',
    ]) {
      expect(leerObjetivo(texto).meta).toBeNull();
    }
    expect(leerObjetivo('Quiero recuperar mis gastos').tipo).toBe('CUBRIR_GASTOS');
  });

  it('avisa si había más de un monto y cuál tomó', () => {
    const lectura = leerObjetivo('Quiero ganar $400.000 o $500.000');
    expect(lectura.meta).toBe(400_000);
    expect(lectura.avisos[0]).toMatch(/También escribiste \$500\.000/);
  });

  it('avisa si la meta ya se cumple hoy', () => {
    expect(leerObjetivo('Quiero ganar $100.000', NEGOCIO).avisos[0]).toMatch(/ya la cumplís/);
  });

  it('explica retirar y cubrir gastos con sus palabras', () => {
    expect(leerObjetivo('Quiero retirar $150.000').explicacion).toMatch(
      /^Retirar \$150\.000 por mes/,
    );
    expect(leerObjetivo('Quiero cubrir $300.000 de alquiler').explicacion).toMatch(
      /para tus gastos/,
    );
    expect(leerObjetivo('Quiero ganar $100.000 por semana').explicacion).toMatch(
      /unos \$429\.000 por mes/,
    );
  });
});

describe('alcanceDeLaMeta', () => {
  const negocio = { hayVentas: true, vendiste: 83_000, queda: 28_000 };

  it('una meta mayor a lo que se vende no se alcanza y sugiere un primer escalón', () => {
    const alcance = alcanceDeLaMeta(1_000_000, negocio);
    expect(alcance?.nivel).toBe('imposible');
    expect(alcance?.sugerida).toBe(36_000);
    expect(alcance?.mensaje).toMatch(/Hoy vendés unos \$83\.000 por mes/);
  });

  it('más del doble de lo que queda hoy está lejos pero se puede', () => {
    expect(alcanceDeLaMeta(70_000, negocio)?.nivel).toBe('lejos');
  });

  it('una meta cercana no avisa nada', () => {
    expect(alcanceDeLaMeta(40_000, negocio)).toBeNull();
  });

  it('sin ventas o si hoy no queda nada no inventa un escalón', () => {
    expect(alcanceDeLaMeta(1_000_000, { hayVentas: false, vendiste: 0, queda: 0 })).toBeNull();
    const sinQueda = alcanceDeLaMeta(1_000_000, {
      hayVentas: true,
      vendiste: 83_000,
      queda: -5_000,
    });
    expect(sinQueda?.nivel).toBe('imposible');
    expect(sinQueda?.sugerida).toBeNull();
  });
});
