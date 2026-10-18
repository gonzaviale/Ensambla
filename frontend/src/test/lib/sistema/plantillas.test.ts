import { describe, expect, it } from 'vitest';
import {
  allBlocks,
  applyLayout,
  clearExamples,
  createGuided,
  makeTemplate,
  refineDraft,
  removeBlock,
  removeRow,
  splitBlocks,
  totals,
  upsertRow,
  validateRow,
} from '@/lib/sistema/core';
import type { Bloque, Fila, Sistema, TipoBloque } from '@/lib/sistema/tipos';

function bloque(sistema: Sistema, tipo: TipoBloque): Bloque {
  const encontrado = allBlocks(sistema).find((item) => item.type === tipo);
  if (!encontrado) throw new Error(`Falta el bloque ${tipo}`);
  return encontrado;
}

const tipos = (sistema: Sistema) => allBlocks(sistema).map((item) => item.type);

function filaNueva(
  bloqueDestino: Bloque,
  valores: Record<string, string | number | boolean>,
  sistema: Sistema,
  filaId: string | null = null,
): Fila {
  const resultado = upsertRow(bloqueDestino, valores, filaId, sistema);
  if (!resultado.row) throw new Error(`No se pudo guardar: ${JSON.stringify(resultado.errors)}`);
  return resultado.row;
}

describe('plantillas', () => {
  it('son conjuntos de piezas vinculadas con anchos iniciales útiles', () => {
    const almacen = makeTemplate('shop', {}, true);
    const deposito = makeTemplate('warehouse', {}, false);
    const negocio = makeTemplate('business', {}, true);

    expect(tipos(almacen)).toEqual(['inventory', 'sales', 'clients']);
    expect(tipos(deposito)).toEqual(['inventory', 'entries', 'exits']);
    expect(tipos(negocio)).toEqual(['orders', 'clients', 'inventory']);
    const clientes = bloque(negocio, 'clients');
    expect(clientes.width).toBe('half');
    expect(bloque(negocio, 'orders').width).toBe('half');
    expect(bloque(negocio, 'inventory').width).toBe('full');
    expect(bloque(negocio, 'orders').fields.find((campo) => campo.key === 'customer')?.target).toBe(
      clientes.id,
    );
    for (const movimiento of allBlocks(deposito).filter((item) => item.type !== 'inventory')) {
      expect(movimiento.fields.find((campo) => campo.key === 'product')?.target).toBe(
        bloque(deposito, 'inventory').id,
      );
    }
    const ventas = bloque(almacen, 'sales');
    expect(ventas.fields.find((campo) => campo.key === 'product')?.target).toBe(
      bloque(almacen, 'inventory').id,
    );
    expect(ventas.fields.find((campo) => campo.key === 'customer')?.target).toBe(
      bloque(almacen, 'clients').id,
    );
    expect(ventas.stockDirection).toBe(-1);
  });

  it('las entradas y los gastos son opciones independientes de la base', () => {
    expect(tipos(makeTemplate('shop', { movements: true }))).toEqual([
      'inventory',
      'sales',
      'clients',
      'entries',
    ]);
    expect(tipos(makeTemplate('business', { movements: true, expenses: true }))).toEqual([
      'orders',
      'clients',
      'inventory',
      'entries',
      'exits',
      'expenses',
    ]);
    expect(makeTemplate('warehouse', { expenses: true }).options).toEqual({
      movements: false,
      expenses: true,
    });
  });
});

describe('ventas', () => {
  it('elegir un producto sugiere su precio, descuenta stock y permite un precio manual', () => {
    const sistema = makeTemplate('shop', {}, true);
    const inventario = bloque(sistema, 'inventory');
    const ventas = bloque(sistema, 'sales');
    const clientes = bloque(sistema, 'clients');
    const producto = inventario.rows[0] as Fila;
    const cliente = clientes.rows[0] as Fila;
    const stockInicial = Number(producto.quantity);

    const venta = filaNueva(
      ventas,
      { product: producto.id, quantity: 3, price: '', customer: cliente.id, credit: true },
      sistema,
    );

    expect(venta.price).toBe(3600);
    expect(producto.quantity).toBe(stockInicial - 3);
    expect(cliente.__example).toBeUndefined();
    const manual = filaNueva(ventas, { product: producto.id, quantity: 1, price: 3900 }, sistema);
    expect(manual.price).toBe(3900);
    expect(totals(sistema).income).toBe(3 * 3600 + 3900);
  });

  it('no deja vender más de lo que hay y editar una venta revierte la anterior', () => {
    const sistema = makeTemplate('shop', {}, true);
    const inventario = bloque(sistema, 'inventory');
    const ventas = bloque(sistema, 'sales');
    const producto = filaNueva(inventario, { name: 'Alfajor', quantity: 10, price: 900 }, sistema);

    const venta = filaNueva(ventas, { product: producto.id, quantity: 4 }, sistema);
    expect(producto.quantity).toBe(6);
    expect(
      upsertRow(ventas, { product: producto.id, quantity: 20 }, null, sistema).errors?.quantity,
    ).toBeTruthy();
    expect(producto.quantity).toBe(6);

    filaNueva(ventas, { product: producto.id, quantity: 2 }, sistema, venta.id);
    expect(producto.quantity).toBe(8);
    expect(removeRow(ventas, venta.id, sistema).removed).toBe(true);
    expect(producto.quantity).toBe(10);
  });
});

describe('entradas y salidas', () => {
  it('actualizan el stock, las ediciones revierten el efecto anterior y se evita el stock negativo', () => {
    const sistema = makeTemplate('warehouse', {}, false);
    const [inventario, entradas, salidas] = allBlocks(sistema) as [Bloque, Bloque, Bloque];
    const producto = filaNueva(inventario, { name: 'Azúcar', quantity: 10, price: 1150 }, sistema);

    const entrada = filaNueva(
      entradas,
      { product: producto.id, quantity: 5, date: '2026-10-16' },
      sistema,
    );
    expect(producto.quantity).toBe(15);
    const salida = filaNueva(
      salidas,
      { product: producto.id, quantity: 4, date: '2026-10-16' },
      sistema,
    );
    expect(producto.quantity).toBe(11);
    expect(
      upsertRow(salidas, { product: producto.id, quantity: 20 }, null, sistema).errors?.quantity,
    ).toBeTruthy();
    expect(producto.quantity).toBe(11);
    expect(salidas.rows).toHaveLength(1);
    filaNueva(
      salidas,
      { product: producto.id, quantity: 2, date: '2026-10-16' },
      sistema,
      salida.id,
    );
    expect(producto.quantity).toBe(13);
    removeRow(salidas, salida.id, sistema);
    expect(producto.quantity).toBe(15);
    removeRow(entradas, entrada.id, sistema);
    expect(producto.quantity).toBe(10);
  });

  it('editar un movimiento puede cambiar de producto sin perder el efecto anterior', () => {
    const sistema = makeTemplate('warehouse', {}, false);
    const [inventario, entradas, salidas] = allBlocks(sistema) as [Bloque, Bloque, Bloque];
    const a = filaNueva(inventario, { name: 'A', quantity: 5 }, sistema);
    const b = filaNueva(inventario, { name: 'B', quantity: 3 }, sistema);

    const entrada = filaNueva(entradas, { product: a.id, quantity: 7 }, sistema);
    expect(a.quantity).toBe(12);
    filaNueva(entradas, { product: b.id, quantity: 4 }, sistema, entrada.id);
    expect(a.quantity).toBe(5);
    expect(b.quantity).toBe(7);
    filaNueva(salidas, { product: b.id, quantity: 6 }, sistema);

    expect(removeRow(entradas, entrada.id, sistema).errors?.quantity).toBeTruthy();
    expect(b.quantity).toBe(1);
  });

  it('quitar ejemplos o bloques de movimientos revierte el stock sin perder consumos reales', () => {
    const sistema = makeTemplate('warehouse', {}, false);
    const [stock, entradas, salidas] = allBlocks(sistema) as [Bloque, Bloque, Bloque];
    const producto = filaNueva(stock, { name: 'Harina', quantity: 10 }, sistema);
    const entrada = filaNueva(entradas, { product: producto.id, quantity: 5 }, sistema);
    entrada.__example = true;

    const limpieza = clearExamples(sistema);

    expect(limpieza.removed).toHaveLength(1);
    expect(producto.quantity).toBe(10);
    expect(entradas.rows).toHaveLength(0);
    filaNueva(entradas, { product: producto.id, quantity: 5 }, sistema);
    filaNueva(salidas, { product: producto.id, quantity: 12 }, sistema);
    expect(producto.quantity).toBe(3);
    expect(removeBlock(sistema, entradas.id).errors?.quantity).toBeTruthy();
    expect(producto.quantity).toBe(3);
    expect(entradas.rows).toHaveLength(1);
    removeBlock(sistema, salidas.id);
    expect(producto.quantity).toBe(15);
    expect(removeBlock(sistema, entradas.id).removed).toBe(true);
    expect(producto.quantity).toBe(10);
  });
});

describe('distribución y propuesta guiada', () => {
  it('separar una fila en dos preserva los registros y evita que el par quede partido', () => {
    const sistema = makeTemplate('business', {}, true);
    const [pedidos, clientes, stock] = allBlocks(sistema) as [Bloque, Bloque, Bloque];
    const instantanea = JSON.stringify(pedidos.rows);

    expect(splitBlocks(sistema, stock.id, clientes.id, 'right')).toBe(true);
    expect(stock.width).toBe('half');
    expect(clientes.width).toBe('half');
    expect(clientes.startRow).toBe(true);
    expect(sistema.sections[0]?.blocks[2]?.id).toBe(stock.id);
    expect(JSON.stringify(pedidos.rows)).toBe(instantanea);
    applyLayout(sistema, 'mosaic');
    expect(stock.width).toBe('full');
    expect(clientes.startRow).toBeUndefined();
  });

  it('la propuesta guiada se puede refinar con bloques, distribución y secciones', () => {
    const { system: sistema } = createGuided(
      'Tengo un kiosco y vendo bebidas y golosinas',
      'Lo de Rosa',
    );

    expect(sistema.name).toBe('Lo de Rosa');
    for (const tipo of ['inventory', 'sales', 'clients'] as const) {
      expect(tipos(sistema)).toContain(tipo);
    }
    refineDraft(sistema, 'Me falta controlar gastos');
    expect(tipos(sistema)).toContain('expenses');
    refineDraft(sistema, 'Prefiero dos columnas y el stock de todo el ancho');
    expect(bloque(sistema, 'inventory').width).toBe('full');
    refineDraft(sistema, 'Separar clientes en otra sección');
    expect(sistema.sections).toHaveLength(2);
    expect(sistema.sections[1]?.blocks[0]?.type).toBe('clients');
    expect(bloque(sistema, 'sales').fields.find((campo) => campo.key === 'customer')?.target).toBe(
      bloque(sistema, 'clients').id,
    );
  });

  it('elige la base según lo que cuenta la persona y le pone nombre', () => {
    expect(createGuided('Tengo un kiosco').system.name).toBe('Mi kiosco');
    expect(createGuided('Tengo un minimercado').system.name).toBe('Mi minimercado');
    expect(tipos(createGuided('Vendo por encargo y pedidos').system)).toEqual([
      'orders',
      'clients',
      'inventory',
    ]);
    expect(tipos(createGuided('Necesito llevar entradas y salidas de mi depósito').system)).toEqual(
      ['inventory', 'entries', 'exits'],
    );
  });

  it('refinar quita y suma bloques con mensajes claros, y avisa cuando no entiende', () => {
    const { system: sistema } = createGuided('Tengo un almacén');

    expect(refineDraft(sistema, 'No quiero clientes')).toContain('Quité clientes.');
    expect(tipos(sistema)).not.toContain('clients');
    expect(refineDraft(sistema, 'Sumá pedidos')).toContain('Sumé pedidos.');
    expect(refineDraft(sistema, 'asdf')).toContain('¿Qué te serviría?');
  });

  it('valida un pedido con el cliente de la plantilla', () => {
    const sistema = makeTemplate('business', {}, true);
    const pedidos = bloque(sistema, 'orders');

    expect(validateRow(pedidos, pedidos.rows[0] as Fila, sistema)).toEqual({});
  });
});
