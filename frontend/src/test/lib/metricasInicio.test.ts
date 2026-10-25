import { crearAlmacenDeEjemplo } from '@/lib/sistema/demo';
import { describe, it, expect } from 'vitest';
import { makeBlock, makeSystem } from '@/lib/sistema/core';
import { metricasInicio, validarMeta } from '@/lib/sistema/metricasInicio';
import type { Bloque } from '@/lib/sistema/tipos';

const sistemaCon = (...bloques: Bloque[]) => {
  const sistema = makeSystem('Negocio');
  sistema.sections = [{ id: 'seccion', name: 'Datos', blocks: bloques }];
  return sistema;
};
describe('métricas de inicio', () => {
  it('excluye el almacén de demostración aunque sus filas no tengan marca de ejemplo', () => {
    const sistema = crearAlmacenDeEjemplo('2026-10-24');
    expect(metricasInicio(sistema).every((m) => m.valor === 0)).toBe(true);
    expect(sistema.sections.flatMap((s) => s.blocks).some((b) => b.rows.length > 0)).toBe(true);
  });
  it('separa ventas de pedidos y descarta ejemplos, sin sumar importes como ganancia', () => {
    const ventas = makeBlock('sales');
    const pedidos = makeBlock('orders');
    ventas.rows = [
      { id: 'v', quantity: 2, price: 500 },
      { id: 'e', quantity: 99, price: 999, __example: true },
    ];
    pedidos.rows = [{ id: 'p', total: 10000, status: 'Pendiente' }];
    const metricas = metricasInicio(sistemaCon(ventas, pedidos));
    expect(metricas.find((m) => m.clave === 'ventas')?.valor).toBe(1000);
    expect(metricas.find((m) => m.clave === 'pendientes')?.valor).toBe(1);
    expect(metricas.find((m) => m.clave === 'filas')?.valor).toBe(2);
    expect(metricas.find((m) => m.clave === 'ventas')?.detalle).toContain('No representa ganancia');
  });
  it('no presenta ventas incompletas como cero ni suma cantidades inválidas', () => {
    const ventas = makeBlock('sales');
    ventas.rows = [{ id: 'v', quantity: '', price: 200 }];
    expect(metricasInicio(sistemaCon(ventas))[0]?.valor).toBeNull();
    ventas.rows.push({ id: 'v2', quantity: 3, price: '100' });
    expect(metricasInicio(sistemaCon(ventas))[0]?.valor).toBe(300);
    expect(metricasInicio(sistemaCon(ventas))[0]?.detalle).toContain('1 filas');
  });
  it('no considera entregados ni cancelados como pendientes', () => {
    const pedidos = makeBlock('orders');
    pedidos.rows = [
      { id: 'a', status: ' Entregado ' },
      { id: 'b', status: 'Cancelado' },
      { id: 'c', status: 'Completado' },
      { id: 'd', status: 'En preparación' },
    ];
    expect(metricasInicio(sistemaCon(pedidos)).find((m) => m.clave === 'pendientes')?.valor).toBe(
      1,
    );
  });
  it('evalúa mínimos definidos y no suma existencias de distintas unidades', () => {
    const inventario = makeBlock('inventory');
    inventario.rows = [
      { id: 'a', quantity: 2, minimum: 2 },
      { id: 'b', quantity: 4, minimum: 3 },
      { id: 'c', quantity: 0 },
      { id: 'd', quantity: '', minimum: 1 },
      { id: 'e', quantity: 0, minimum: 0, __example: true },
    ];
    const metricas = metricasInicio(sistemaCon(inventario));
    expect(metricas.find((m) => m.clave === 'stock-bajo')?.valor).toBe(1);
    expect(metricas.find((m) => m.clave === 'stock-bajo')?.direccion).toBe('maximo');
  });
  it('muestra solo indicadores correspondientes a las tablas y excluye notas y resúmenes', () => {
    const clientes = makeBlock('clients');
    const nota = makeBlock('note');
    clientes.rows = [{ id: 'c', name: 'Ana' }];
    nota.rows = [{ id: 'n' }];
    expect(metricasInicio(sistemaCon(clientes, nota)).map((m) => [m.clave, m.valor])).toEqual([
      ['clientes', 1],
      ['filas', 1],
    ]);
  });
  it.each(['', '-1', 'Infinity', 'NaN', '1e309', '0.5'])(
    'rechaza meta de conteo inválida %s',
    (texto) => {
      expect(validarMeta(texto)).toBeNull();
    },
  );
  it('admite cero, conteos enteros y metas monetarias decimales', () => {
    expect(validarMeta('0')).toBe(0);
    expect(validarMeta('5')).toBe(5);
    expect(validarMeta('100.50', true)).toBe(100.5);
  });
});
