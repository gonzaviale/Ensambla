import { describe, expect, it } from 'vitest';
import {
  allBlocks,
  clone,
  id,
  importRows,
  makeSystem,
  moveBlock,
  recommend,
  totals,
  upsertRow,
  validateRow,
} from '@/lib/sistema/core';
import type { Bloque, Seccion, Sistema, TipoBloque } from '@/lib/sistema/tipos';

function bloque(sistema: Sistema, tipo: TipoBloque): Bloque {
  const encontrado = allBlocks(sistema).find((item) => item.type === tipo);
  if (!encontrado) throw new Error(`Falta el bloque ${tipo}`);
  return encontrado;
}

const fila = <T>(items: readonly T[], indice = 0): T => {
  const item = items[indice];
  if (item === undefined) throw new Error(`Falta el elemento ${indice}`);
  return item;
};

describe('motor: recomendaciones', () => {
  it('distinguen un local de venta, la venta por pedido y el control de mercadería', () => {
    expect(recommend('Tengo un kiosco y vendo bebidas y golosinas').kind).toBe('shop');
    expect(recommend('Trabajo con pedidos y encargos para fiestas').kind).toBe('orders');
    expect(recommend('Necesito controlar la mercadería de mi depósito').kind).toBe('warehouse');
    expect(recommend('Vendo cosas', 'warehouse').kind).toBe('warehouse');
  });

  it('proponen las piezas de cada caso y suman gastos cuando se habla de costos', () => {
    expect(recommend('Tengo un almacén').types).toEqual(['inventory', 'sales', 'clients']);
    const conGastos = recommend('Tengo un almacén y pago alquiler y proveedores');
    for (const tipo of ['inventory', 'sales', 'clients', 'expenses']) {
      expect(conGastos.types).toContain(tipo);
    }
  });
});

describe('motor: creación y validación', () => {
  it('vincula pedidos y ventas con los clientes del mismo sistema', () => {
    const sistema = makeSystem('Mi negocio', ['orders', 'clients', 'inventory', 'sales'], true);
    const pedidos = bloque(sistema, 'orders');
    const clientes = bloque(sistema, 'clients');
    const ventas = bloque(sistema, 'sales');

    expect(pedidos.fields.find((campo) => campo.key === 'customer')?.target).toBe(clientes.id);
    expect(ventas.fields.find((campo) => campo.key === 'customer')?.target).toBe(clientes.id);
    expect(fila(pedidos.rows).customer).toBe(fila(clientes.rows).id);
    expect(validateRow(pedidos, fila(pedidos.rows), sistema)).toEqual({});
    const otro = makeSystem('Otro', ['clients']);
    expect(clientes.id).not.toBe(fila(allBlocks(otro)).id);
  });

  it('impide datos vacíos, números inválidos y vínculos rotos', () => {
    const sistema = makeSystem('Prueba', ['inventory', 'clients', 'orders']);
    const inventario = bloque(sistema, 'inventory');
    const clientes = bloque(sistema, 'clients');
    const pedidos = bloque(sistema, 'orders');

    expect(validateRow(inventario, { name: '  ', quantity: '2' }, sistema).name).toBeTruthy();
    expect(
      validateRow(inventario, { name: 'Yerba', quantity: 'cero' }, sistema).quantity,
    ).toBeTruthy();
    expect(validateRow(inventario, { name: 'Yerba', quantity: 0 }, sistema)).toEqual({});
    expect(validateRow(clientes, { name: 'Ana', email: 'incorrecto' }, sistema).email).toBeTruthy();
    expect(
      validateRow(pedidos, { name: 'Pedido', customer: 'no-existe' }, sistema).customer,
    ).toBeTruthy();
    expect(
      validateRow(pedidos, { name: 'Pedido', status: 'Inventado' }, sistema).status,
    ).toBeTruthy();
  });

  it('pide a quién se le fía cuando una venta es fiada', () => {
    const sistema = makeSystem('Local', ['inventory', 'sales', 'clients'], true);
    const ventas = bloque(sistema, 'sales');
    const producto = fila(bloque(sistema, 'inventory').rows);

    const errores = validateRow(
      ventas,
      { product: producto.id, quantity: 1, credit: true },
      sistema,
    );

    expect(errores.customer).toBe('Elegí a quién le fiás.');
  });

  it('un ejemplo editado se conserva y un precio vacío queda vacío', () => {
    const sistema = makeSystem('Stock', ['inventory'], true);
    const inventario = fila(allBlocks(sistema));
    const original = fila(inventario.rows);

    const resultado = upsertRow(
      inventario,
      { ...original, name: 'Mi producto' },
      original.id,
      sistema,
    );

    expect(resultado.row?.__example).toBeUndefined();
    expect(inventario.rows.filter((item) => !item.__example)).toHaveLength(1);
    expect(
      upsertRow(inventario, { name: 'Mi segundo producto', quantity: 0 }, null, sistema).row?.price,
    ).toBe('');
  });
});

describe('motor: bloques y datos', () => {
  it('mover bloques entre secciones conserva registros, claves y relaciones', () => {
    const sistema = makeSystem('Mi sistema', ['clients', 'orders'], true);
    const primera = fila(sistema.sections);
    const [clientes, pedidos] = primera.blocks as [Bloque, Bloque];
    const destino: Seccion = { id: id(), name: 'Ventas', blocks: [] };
    sistema.sections.push(destino);
    const instantanea = JSON.stringify(pedidos.rows);

    expect(moveBlock(sistema, pedidos.id, destino.id)).toBe(true);
    expect(primera.blocks).toHaveLength(1);
    expect(fila(destino.blocks).id).toBe(pedidos.id);
    expect(JSON.stringify(fila(destino.blocks).rows)).toBe(instantanea);
    expect(fila(destino.blocks).fields.find((campo) => campo.type === 'relation')?.target).toBe(
      clientes.id,
    );
    expect(moveBlock(sistema, pedidos.id, primera.id, clientes.id)).toBe(true);
    expect(fila(primera.blocks).id).toBe(pedidos.id);
    expect(moveBlock(sistema, pedidos.id, primera.id, pedidos.id)).toBe(false);
  });

  it('la carga revisada evita duplicados sin sobrescribir datos', () => {
    const sistema = makeSystem('Inventario', ['inventory']);
    const inventario = fila(allBlocks(sistema));
    upsertRow(inventario, { name: 'Yerba Playadito', quantity: 12 }, null, sistema);

    const resultado = importRows(
      inventario,
      [
        { name: ' yerba playadito ', quantity: 999 },
        { name: 'Aceite Natura', quantity: 7 },
      ],
      sistema,
      true,
    );

    expect(resultado).toEqual({ added: 1, skipped: 1, errors: [] });
    expect(fila(inventario.rows).quantity).toBe(12);
    expect(fila(inventario.rows, 1).__source).toBe('Lectura simulada');
  });

  it('los totales y la serialización respetan los datos del sistema', () => {
    const sistema = makeSystem('Prueba', ['inventory', 'orders', 'clients'], true);

    const resumen = totals(sistema);

    expect(resumen.stock).toBe(72);
    expect(resumen.pending).toBe(2);
    expect(resumen.clients).toBe(3);
    expect(resumen.income).toBe(48700);
    expect(clone(sistema)).toEqual(sistema);
  });
});
