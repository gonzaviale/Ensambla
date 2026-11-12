import { describe, expect, it } from 'vitest';
import { allBlocks, clone, makeTemplate } from '@/lib/sistema/core';
import { codigoGtin, identidadGtin } from '@/lib/escaneo/codigo';
import {
  buscarProductoPorCodigo,
  guardarProductoEscaneado,
  prepararProducto,
  sumarStockProductoEscaneado,
} from '@/lib/escaneo/inventario';

function requerido<T>(dato: T | undefined): T {
  if (dato === undefined) throw new Error('Falta un dato del escenario');
  return dato;
}

function escenario() {
  const sistema = makeTemplate('shop', { movements: true }, false);
  const inventario = allBlocks(sistema).find((bloque) => bloque.type === 'inventory');
  if (!inventario) throw new Error('Falta el inventario');
  inventario.rows = [
    {
      id: 'alfajor',
      name: 'Guaymallén',
      barcode: '77903754',
      quantity: 10,
      price: 500.25,
      cost: 300,
      minimum: 2,
    },
  ];
  return { sistema, inventario };
}

describe('alta por código en el inventario existente', () => {
  it('valida GTIN y conserva ceros y equivalencias', () => {
    expect(codigoGtin(' 77903754 ')).toBe('77903754');
    expect(identidadGtin('77903754')).toBe(identidadGtin('0000077903754'));
    for (const codigo of ['77903755', '00000000', '7790a754'])
      expect(() => codigoGtin(codigo)).toThrow();
  });
  it('guarda costos, precios y campos propios sin crear ventas ni movimientos ni modificar el original', () => {
    const { sistema, inventario } = escenario();
    inventario.fields.push({
      key: 'categoria',
      label: 'Categoría',
      type: 'select',
      required: true,
      options: ['Kiosco'],
    });
    const borrador = prepararProducto(inventario, '3017620422003');
    borrador.valores = {
      ...borrador.valores,
      name: 'Nuevo producto',
      quantity: '12',
      price: '850.25',
      cost: '500.10',
      minimum: '3',
      categoria: 'Kiosco',
    };
    const resultado = guardarProductoEscaneado(sistema, inventario.id, borrador);
    expect(resultado.errores).toBeUndefined();
    expect(resultado.producto).toMatchObject({
      name: 'Nuevo producto',
      barcode: '3017620422003',
      quantity: 12,
      price: 850.25,
      cost: 500.1,
      minimum: 3,
      categoria: 'Kiosco',
    });
    expect(inventario.rows).toHaveLength(1);
    expect(inventario.fields.some((campo) => campo.key === 'barcode')).toBe(false);
    expect(
      allBlocks(requerido(resultado.sistema))
        .filter((bloque) => ['sales', 'entries'].includes(bloque.type))
        .every((bloque) => bloque.rows.length === 0),
    ).toBe(true);
  });
  it('acepta un conteo cero explícito y precios opcionales; no inventa stock', () => {
    const { sistema, inventario } = escenario();
    const borrador = prepararProducto(inventario, '3017620422003');
    borrador.valores.name = 'Nuevo';
    expect(
      guardarProductoEscaneado(sistema, inventario.id, borrador).errores?.quantity,
    ).toBeTruthy();
    borrador.valores.quantity = '0';
    expect(guardarProductoEscaneado(sistema, inventario.id, borrador).producto).toMatchObject({
      quantity: 0,
      price: '',
      cost: '',
    });
  });
  it('rechaza duplicados equivalentes y altas ocurridas mientras se revisaba', () => {
    const { sistema, inventario } = escenario();
    const borrador = prepararProducto(inventario, '0000077903754');
    expect(guardarProductoEscaneado(sistema, inventario.id, borrador).errores?._form).toMatch(
      /ya está cargado/,
    );
    expect(buscarProductoPorCodigo(inventario, '0000077903754')?.id).toBe('alfajor');
    expect(inventario.rows).toHaveLength(1);
  });
  it('vincula un producto sin código y conserva su cantidad actual si otro flujo la modificó', () => {
    const { sistema, inventario } = escenario();
    const producto = requerido(inventario.rows[0]);
    delete producto.barcode;
    const borrador = prepararProducto(inventario, '77903754', producto);
    borrador.valores.cost = '350';
    producto.quantity = 6;
    const guardado = guardarProductoEscaneado(sistema, inventario.id, borrador);
    expect(guardado.producto).toMatchObject({
      id: 'alfajor',
      barcode: '77903754',
      quantity: 6,
      cost: 350,
      price: 500.25,
    });
    expect(
      allBlocks(requerido(guardado.sistema)).find((bloque) => bloque.id === inventario.id)?.rows,
    ).toHaveLength(1);
  });
  it('un cambio concurrente en un dato editado requiere revisar y no aplica cambios parciales', () => {
    const { sistema, inventario } = escenario();
    const producto = requerido(inventario.rows[0]);
    const borrador = prepararProducto(inventario, '77903754', producto);
    borrador.valores.quantity = '15';
    borrador.valores.price = '900';
    producto.quantity = 8;
    const antes = clone(sistema);
    expect(guardarProductoEscaneado(sistema, inventario.id, borrador).errores?.quantity).toMatch(
      /cambió/,
    );
    expect(sistema).toEqual(antes);
  });
  it('respeta campos obligatorios y rechaza cantidades, costos y precios negativos', () => {
    const { sistema, inventario } = escenario();
    inventario.fields.push({ key: 'rubro', label: 'Rubro', type: 'text', required: true });
    const borrador = prepararProducto(inventario, '3017620422003');
    borrador.valores = { name: 'Nuevo', quantity: '2', price: '100', cost: '50' };
    expect(guardarProductoEscaneado(sistema, inventario.id, borrador).errores?.rubro).toBeTruthy();
    borrador.valores.rubro = 'Alimentos';
    for (const clave of ['quantity', 'cost', 'price', 'minimum']) {
      const caso = { ...borrador, valores: { ...borrador.valores, [clave]: '-1' } };
      expect(guardarProductoEscaneado(sistema, inventario.id, caso).errores?.[clave]).toBeTruthy();
    }
    expect(inventario.rows).toHaveLength(1);
  });
  it('no reemplaza otro código ni usa una tabla que no sea inventario', () => {
    const { sistema, inventario } = escenario();
    const borrador = prepararProducto(inventario, '3017620422003', inventario.rows[0]);
    expect(guardarProductoEscaneado(sistema, inventario.id, borrador).errores?._form).toMatch(
      /otro código/,
    );
    expect(guardarProductoEscaneado(sistema, 'ausente', borrador).errores?._form).toMatch(
      /no está disponible/,
    );
    inventario.fields.push({ key: 'barcode', label: 'Código', type: 'number', required: false });
    const nuevo = prepararProducto(inventario, '3017620422003');
    expect(guardarProductoEscaneado(sistema, inventario.id, nuevo).errores?._form).toMatch(
      /de texto/,
    );
  });
});

describe('reposición de un producto reconocido', () => {
  it('suma las unidades al stock vigente sin duplicar el producto ni modificar otros datos', () => {
    const { sistema, inventario } = escenario();
    const producto = requerido(inventario.rows[0]);
    producto.barcode = '0000077903754';
    producto.quantity = 6;
    producto.cost = 350;
    const antes = clone(sistema);
    const resultado = sumarStockProductoEscaneado(
      sistema,
      inventario.id,
      producto.id,
      '77903754',
      '3',
    );
    expect(resultado.errores).toBeUndefined();
    expect(resultado.producto).toEqual({ ...producto, quantity: 9 });
    expect(
      allBlocks(requerido(resultado.sistema)).find((bloque) => bloque.id === inventario.id)?.rows,
    ).toHaveLength(1);
    expect(sistema).toEqual(antes);
  });

  it('admite cantidades decimales positivas sin reemplazar el stock actual', () => {
    const { sistema, inventario } = escenario();
    expect(
      sumarStockProductoEscaneado(sistema, inventario.id, 'alfajor', '77903754', '0.5').producto
        ?.quantity,
    ).toBe(10.5);
  });

  it.each([
    '',
    ' ',
    '0',
    '-2',
    'NaN',
    'Infinity',
    '1e309',
    String(Number.MAX_SAFE_INTEGER),
    String(Number.MIN_VALUE),
  ])('rechaza una cantidad inválida %j sin alterar el inventario', (cantidad) => {
    const { sistema, inventario } = escenario();
    const antes = clone(sistema);
    expect(
      sumarStockProductoEscaneado(sistema, inventario.id, 'alfajor', '77903754', cantidad).errores
        ?.quantity,
    ).toBeTruthy();
    expect(sistema).toEqual(antes);
  });

  it('impide reponer un producto eliminado o cuyo código cambió', () => {
    const { sistema, inventario } = escenario();
    expect(
      sumarStockProductoEscaneado(sistema, inventario.id, 'eliminado', '77903754', '2').errores
        ?._form,
    ).toMatch(/eliminado/);
    expect(
      sumarStockProductoEscaneado(sistema, inventario.id, 'alfajor', '3017620422003', '2').errores
        ?._form,
    ).toMatch(/código.*cambió/);
    expect(inventario.rows[0]?.quantity).toBe(10);
  });
});
