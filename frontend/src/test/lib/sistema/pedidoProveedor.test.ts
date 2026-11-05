import { describe, expect, it } from 'vitest';
import {
  NOMBRE_SIN_PROVEEDOR,
  cantidadEscrita,
  decisionDe,
  decisionesIniciales,
  errorDelPedido,
  lineasDelPedido,
  nombreDelGrupo,
  textoDelPedido,
  type DecisionesDelPedido,
} from '@/lib/sistema/pedidoProveedor';
import type { GrupoDeReposicion } from '@/types/reposicion';

const GRUPO: GrupoDeReposicion = {
  proveedor: 'Molinos',
  productos: [
    { id: 'a', nombre: 'Yerba', cantidadActual: 0, cantidadAPedir: 20, situacion: 'Agotado' },
    { id: 'b', nombre: 'Fideos', cantidadActual: 3, cantidadAPedir: 17, situacion: 'Quedan 3 (mínimo 10)' },
  ],
};

const SIN_PROVEEDOR: GrupoDeReposicion = { proveedor: null, productos: [] };

const iniciales = () => decisionesIniciales([GRUPO]);

describe('decisionesIniciales', () => {
  it('pide todos los productos con la cantidad que sugiere el servidor', () => {
    expect(iniciales()).toEqual({
      a: { pedir: true, cantidad: '20' },
      b: { pedir: true, cantidad: '17' },
    });
  });

  it('sin grupos no hay decisiones', () => {
    expect(decisionesIniciales([])).toEqual({});
  });
});

describe('decisionDe', () => {
  it('devuelve la decisión tomada', () => {
    expect(decisionDe(iniciales(), 'a')).toEqual({ pedir: true, cantidad: '20' });
  });

  it('un producto sin decisión no se pide', () => {
    expect(decisionDe({}, 'zzz')).toEqual({ pedir: false, cantidad: '' });
  });
});

describe('nombreDelGrupo', () => {
  it('usa el proveedor o «Sin proveedor»', () => {
    expect(nombreDelGrupo(GRUPO)).toBe('Molinos');
    expect(nombreDelGrupo(SIN_PROVEEDOR)).toBe(NOMBRE_SIN_PROVEEDOR);
  });
});

describe('cantidadEscrita', () => {
  it('acepta enteros, decimales y coma decimal', () => {
    expect(cantidadEscrita('12')).toBe(12);
    expect(cantidadEscrita('2.5')).toBe(2.5);
    expect(cantidadEscrita('2,5')).toBe(2.5);
  });

  it('rechaza vacío, cero, negativos y lo que no es un número', () => {
    expect(cantidadEscrita('')).toBeNull();
    expect(cantidadEscrita('  ')).toBeNull();
    expect(cantidadEscrita('0')).toBeNull();
    expect(cantidadEscrita('-3')).toBeNull();
    expect(cantidadEscrita('diez')).toBeNull();
  });
});

describe('errorDelPedido', () => {
  it('está listo cuando hay productos elegidos con cantidades válidas', () => {
    expect(errorDelPedido(GRUPO, iniciales())).toBeNull();
  });

  it('pide elegir al menos un producto', () => {
    const decisiones: DecisionesDelPedido = {
      a: { pedir: false, cantidad: '20' },
      b: { pedir: false, cantidad: '17' },
    };

    expect(errorDelPedido(GRUPO, decisiones)).toBe('Elegí al menos un producto para el pedido.');
  });

  it('pide revisar una cantidad inválida de un producto elegido', () => {
    const decisiones = { ...iniciales(), a: { pedir: true, cantidad: '0' } };

    expect(errorDelPedido(GRUPO, decisiones)).toBe(
      'Revisá las cantidades: tienen que ser un número mayor a cero.',
    );
  });

  it('una cantidad inválida de un producto que no se pide no molesta', () => {
    const decisiones = { ...iniciales(), a: { pedir: false, cantidad: '' } };

    expect(errorDelPedido(GRUPO, decisiones)).toBeNull();
  });
});

describe('lineasDelPedido', () => {
  it('lleva solo los productos elegidos con su cantidad como número', () => {
    const decisiones = { ...iniciales(), a: { pedir: true, cantidad: '25,5' }, b: { pedir: false, cantidad: '17' } };

    expect(lineasDelPedido(GRUPO, decisiones)).toEqual([{ productoId: 'a', cantidad: 25.5 }]);
  });

  it('deja afuera un producto elegido con una cantidad inválida', () => {
    const decisiones = { ...iniciales(), a: { pedir: true, cantidad: 'x' } };

    expect(lineasDelPedido(GRUPO, decisiones)).toEqual([{ productoId: 'b', cantidad: 17 }]);
  });
});

describe('textoDelPedido', () => {
  it('escribe un renglón por producto elegido', () => {
    const decisiones = { ...iniciales(), b: { pedir: false, cantidad: '17' } };

    expect(textoDelPedido(GRUPO, decisiones)).toBe('Hola, necesito reponer lo siguiente:\n- Yerba: 20');
  });

  it('ignora un producto que ya no está en el grupo', () => {
    const decisiones = { ...iniciales(), zzz: { pedir: true, cantidad: '5' } };

    expect(textoDelPedido(GRUPO, decisiones)).toBe(
      'Hola, necesito reponer lo siguiente:\n- Yerba: 20\n- Fideos: 17',
    );
  });
});
