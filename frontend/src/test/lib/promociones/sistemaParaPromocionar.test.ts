import { describe, expect, it } from 'vitest';
import {
  promocionListaDePrecios,
  sistemaParaPromocionar,
} from '@/lib/promociones/sistemaParaPromocionar';
import { allBlocks, makeSystem } from '@/lib/sistema/core';
import type { Fila, Sistema } from '@/lib/sistema/tipos';

const conInventario = (nombre: string, filas: Fila[] = []): Sistema => {
  const sistema = makeSystem(nombre, ['inventory']);
  const [inventario] = allBlocks(sistema);
  if (inventario) inventario.rows = filas;
  return sistema;
};

const sinInventario = (nombre: string) => makeSystem(nombre, ['clients']);

describe('sistemaParaPromocionar', () => {
  it('usa el cuaderno activo si tiene inventario', () => {
    const uno = conInventario('Uno');
    const dos = conInventario('Dos');

    expect(sistemaParaPromocionar([uno, dos], dos.id)).toBe(dos);
  });

  it('si el activo no tiene inventario usa el primero que lo tenga', () => {
    const clientes = sinInventario('Clientes');
    const almacen = conInventario('Almacén');

    expect(sistemaParaPromocionar([clientes, almacen], clientes.id)).toBe(almacen);
  });

  it('sin cuaderno activo usa el primero con inventario', () => {
    const almacen = conInventario('Almacén');

    expect(sistemaParaPromocionar([sinInventario('Clientes'), almacen], null)).toBe(almacen);
  });

  it('si ningún cuaderno tiene inventario no hay con qué promocionar', () => {
    expect(sistemaParaPromocionar([sinInventario('Clientes')], null)).toBeUndefined();
    expect(sistemaParaPromocionar([], null)).toBeUndefined();
  });
});

describe('promocionListaDePrecios', () => {
  it('arma la lista con los productos que tienen nombre y precio', () => {
    const sistema = conInventario('Almacén', [
      { id: 'a', name: ' Yerba ', price: 3600, quantity: 10 },
      { id: 'b', name: 'Fideos', price: 1100, quantity: 0 },
    ]);

    expect(promocionListaDePrecios(sistema)).toEqual({
      clave: 'lista',
      titulo: 'Lista de precios',
      gancho: 'Precios al día',
      productos: [
        { nombre: 'Yerba', precio: 3600 },
        { nombre: 'Fideos', precio: 1100 },
      ],
    });
  });

  it('deja afuera las filas de ejemplo, las sin nombre y las sin precio', () => {
    const sistema = conInventario('Almacén', [
      { id: 'a', name: 'Ejemplo', price: 100, __example: true },
      { id: 'b', name: '  ', price: 100 },
      { id: 'c', name: 'Sin precio' },
      { id: 'd', name: 'Gratis', price: 0 },
      { id: 'e', name: 'Sal', price: 500 },
    ]);

    expect(promocionListaDePrecios(sistema)?.productos).toEqual([{ nombre: 'Sal', precio: 500 }]);
  });

  it('lleva como máximo doce productos', () => {
    const filas = Array.from({ length: 15 }, (_, posicion) => ({
      id: `f${posicion}`,
      name: `Producto ${posicion + 1}`,
      price: 100,
    }));

    expect(promocionListaDePrecios(conInventario('Almacén', filas))?.productos).toHaveLength(12);
  });

  it('sin productos con precio no hay lista', () => {
    expect(promocionListaDePrecios(conInventario('Almacén'))).toBeNull();
    expect(promocionListaDePrecios(sinInventario('Clientes'))).toBeNull();
  });
});
