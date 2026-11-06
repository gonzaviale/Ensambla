import { describe, expect, it } from 'vitest';
import {
  compararCostos,
  confirmarCompra,
  mensajeDelPedido,
  resumenDelMes,
  sugerirPedido,
  totalCompra,
  validarRevision,
} from '@/lib/proveedores/calculos';
import { crearDemoProveedores, crearRevisionSimulada } from '@/lib/proveedores/demo';
import { demoProveedoresSchema } from '@/types/proveedores';
import type { DemoProveedores, RevisionCompra } from '@/types/proveedores';

function preparar() {
  const datos = crearDemoProveedores('2026-11-05');
  const revision = {
    ...crearRevisionSimulada(datos, 'distribuidora', 'ticket.pdf'),
    fecha: '2026-11-05',
  };
  return { datos, revision };
}

describe('pedidos a proveedores', () => {
  it('sugiere agotados incluso con mínimo cero, y bajo mínimo; excluye el stock suficiente', () => {
    const { datos } = preparar();
    const base = datos.productos[0];
    if (!base) throw new Error('Falta semilla');
    expect(
      sugerirPedido([
        { ...base, id: 'agotado', stock: 0, minimo: 0 },
        { ...base, id: 'bajo', stock: 3, minimo: 10 },
        { ...base, id: 'suficiente', stock: 10, minimo: 10 },
      ]),
    ).toEqual([
      { productoId: 'agotado', cantidad: 1 },
      { productoId: 'bajo', cantidad: 17 },
    ]);
  });

  it('el mensaje respeta cantidades editadas, exclusiones y el proveedor sin mover stock', () => {
    const { datos } = preparar();
    const proveedor = datos.proveedores[0];
    const producto = datos.productos[0];
    if (!proveedor || !producto) throw new Error('Falta semilla');
    const stock = producto.stock;
    const editado = { ...datos, pedido: [{ productoId: producto.id, cantidad: 25 }] };
    expect(mensajeDelPedido(editado, proveedor)).toContain(`25 × ${producto.nombre}`);
    expect(
      mensajeDelPedido(
        { ...editado, pedido: [{ productoId: producto.id, cantidad: 0 }] },
        proveedor,
      ),
    ).toBe('');
    expect(datos.productos[0]?.stock).toBe(stock);
    expect(mensajeDelPedido(editado, { ...proveedor, id: 'otro' })).toBe('');
  });
});

describe('recepción de compras revisadas', () => {
  it('registra una compra, un gasto vinculado y un ingreso de stock de forma idempotente', () => {
    const { datos, revision } = preparar();
    const precio = datos.productos[0]?.precio;
    const guardados = confirmarCompra(datos, revision);
    expect(guardados.productos[0]?.stock).toBe((datos.productos[0]?.stock ?? 0) + 12);
    expect(guardados.productos[0]?.costo).toBe(revision.lineas[0]?.costo);
    expect(guardados.productos[0]?.precio).toBe(precio);
    expect(guardados.compras).toHaveLength(datos.compras.length + 1);
    expect(guardados.gastos.filter((g) => g.compraId === revision.id)).toHaveLength(1);
    expect(confirmarCompra(guardados, revision)).toBe(guardados);
    expect(datos.productos[0]?.stock).toBe(4);
  });

  it('rechaza el mismo número de comprobante normalizado aunque llegue con otro id', () => {
    const { datos, revision } = preparar();
    const guardados = confirmarCompra(datos, revision);
    expect(
      validarRevision(guardados, {
        ...revision,
        id: 'otro',
        numero: ` ${revision.numero.toLowerCase()} `,
      }),
    ).toMatch(/ya está registrado/);
  });

  it('reconcilia centavos, cargos y descuento antes de permitir confirmar', () => {
    const { datos, revision } = preparar();
    const linea = revision.lineas[0];
    if (!linea) throw new Error('Falta línea');
    const conCargos: RevisionCompra = {
      ...revision,
      lineas: [{ ...linea, cantidad: 3, costo: 10.1 }],
      extras: 5.2,
      descuento: 1,
      totalDeclarado: 34.5,
    };
    expect(totalCompra(conCargos)).toBe(34.5);
    expect(validarRevision(datos, conCargos)).toBeNull();
    expect(validarRevision(datos, { ...conCargos, totalDeclarado: 34.49 })).toMatch(
      /total no coincide/,
    );
  });

  it.each([
    ['fecha inválida', { fecha: '2026-99-99' }, /fecha/],
    ['día inexistente', { fecha: '2026-02-31' }, /fecha/],
    ['proveedor inexistente', { proveedorId: 'otro' }, /proveedor/],
    ['comprobante vacío', { numero: ' ' }, /número/],
    ['sin renglones', { lineas: [] }, /al menos/],
    ['cargo inválido', { extras: Number.NaN }, /importes/],
  ])('no confirma %s', (_caso, cambio, mensaje) => {
    const { datos, revision } = preparar();
    expect(validarRevision(datos, { ...revision, ...cambio })).toMatch(mensaje);
  });

  it('impide productos sin vincular, cantidades fraccionarias y renglones duplicados', () => {
    const { datos, revision } = preparar();
    const linea = revision.lineas[0];
    if (!linea) throw new Error('Falta línea');
    expect(validarRevision(datos, { ...revision, lineas: [{ ...linea, productoId: '' }] })).toMatch(
      /Vinculá/,
    );
    expect(validarRevision(datos, { ...revision, lineas: [{ ...linea, cantidad: 1.5 }] })).toMatch(
      /cantidades/,
    );
    expect(validarRevision(datos, { ...revision, lineas: [linea, linea] })).toMatch(/repetidos/);
  });

  it('recibir un comprobante antiguo no reemplaza el costo de una compra posterior', () => {
    const { datos, revision } = preparar();
    const guardados = confirmarCompra(datos, revision);
    const antigua = {
      ...revision,
      id: 'antigua',
      numero: 'ANTIGUA',
      fecha: '2026-10-01',
      lineas: revision.lineas.map((l) => ({ ...l, costo: 1 })),
    };
    const resultado = confirmarCompra(guardados, {
      ...antigua,
      totalDeclarado: totalCompra(antigua),
    });
    expect(resultado.productos[0]?.costo).toBe(guardados.productos[0]?.costo);
    expect(resultado.productos[0]?.stock).toBe((guardados.productos[0]?.stock ?? 0) + 12);
  });
});

describe('costos y resultado del negocio', () => {
  it('compara el mismo producto y proveedor por fecha, sin mezclar otros proveedores ni tocar la venta', () => {
    const { datos, revision } = preparar();
    const guardados = confirmarCompra(datos, revision);
    const ajena = {
      ...revision,
      id: 'ajena',
      numero: 'AJENA',
      proveedorId: 'limpieza',
      lineas: revision.lineas.map((l) => ({ ...l, costo: 1 })),
    };
    const conOtroProveedor = confirmarCompra(guardados, {
      ...ajena,
      totalDeclarado: totalCompra(ajena),
    });
    const comparacion = compararCostos(conOtroProveedor, 'distribuidora')[0];
    expect(comparacion?.anterior).toBe(2700);
    expect(comparacion?.linea.costo).toBe(3024);
    expect(comparacion?.variacion).toBeCloseTo(12);
    expect(comparacion?.precioSugerido).toBe(4032);
    expect(comparacion?.producto.precio).toBe(3600);
    expect(compararCostos(datos, 'distribuidora')[0]?.variacion).toBeNull();
  });

  it('resta costo vendido y gastos generales una sola vez, sin descontar de nuevo las compras de stock', () => {
    const { datos, revision } = preparar();
    const base: DemoProveedores = {
      ...datos,
      ventas: [{ fecha: '2026-11-05', cantidad: 2, precio: 100, costo: 40 }],
      gastos: [
        { id: 'alquiler', name: 'Alquiler', date: '2026-11-01', total: 30, category: 'Alquiler' },
        {
          id: 'fuera',
          name: 'Otro período',
          date: '2026-10-01',
          total: 900,
          category: 'Servicios',
        },
      ],
    };
    const antes = resumenDelMes(base, '2026-11');
    const despues = resumenDelMes(confirmarCompra(base, revision), '2026-11');
    expect(antes).toMatchObject({
      ingresos: 200,
      costoVendido: 80,
      margen: 120,
      operativos: 30,
      resultado: 90,
    });
    expect(despues.resultado).toBe(90);
    expect(despues.compras).toBe(totalCompra(revision));
    expect(resumenDelMes(base, '2026-12').resultado).toBe(0);
  });

  it('valida el almacenamiento local y rechaza importes no finitos', () => {
    const { datos } = preparar();
    expect(demoProveedoresSchema.safeParse(datos).success).toBe(true);
    expect(demoProveedoresSchema.safeParse({ ...datos, version: 2 }).success).toBe(false);
    expect(
      demoProveedoresSchema.safeParse({
        ...datos,
        gastos: [
          { id: 'malo', name: 'NaN', date: '2026-11-05', category: 'Otros', total: Number.NaN },
        ],
      }).success,
    ).toBe(false);
  });
});
