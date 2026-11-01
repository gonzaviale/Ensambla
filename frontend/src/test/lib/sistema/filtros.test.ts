import { describe, expect, it } from 'vitest';
import { claseDeFiltro, filtrarFilas, hayFiltros } from '@/lib/sistema/filtros';
import type { Bloque, Fila, Sistema } from '@/lib/sistema/tipos';

const bloque: Bloque = {
  id: 'pedidos',
  type: 'orders',
  name: 'Pedidos',
  description: '',
  color: 'coral',
  width: 'wide',
  content: '',
  fields: [
    { key: 'name', label: 'Pedido', type: 'text', required: true },
    { key: 'date', label: 'Fecha', type: 'date', required: false },
    { key: 'total', label: 'Total', type: 'currency', required: false },
    {
      key: 'status',
      label: 'Estado',
      type: 'select',
      required: false,
      options: ['Pendiente', 'Entregado'],
    },
    { key: 'paid', label: 'Pagado', type: 'checkbox', required: false },
  ],
  rows: [
    {
      id: '1',
      name: 'Pedido de lavandas',
      date: '2026-10-02',
      total: 13000,
      status: 'Entregado',
      paid: true,
    },
    {
      id: '2',
      name: 'Regalo de cumple',
      date: '2026-10-15',
      total: 8200,
      status: 'Pendiente',
      paid: false,
    },
    {
      id: '3',
      name: 'Cera de abeja',
      date: '2026-10-23',
      total: 19600,
      status: 'Pendiente',
      paid: true,
    },
    { id: '4', name: 'Sin datos', date: '', total: '', status: '', paid: false },
  ],
};

const sistema: Sistema = {
  id: 's',
  name: 'Mis velas',
  createdAt: '',
  updatedAt: '',
  sections: [{ id: 'sec', name: 'Vista general', blocks: [bloque] }],
};

const ids = (filas: Fila[]) => filas.map((fila) => fila.id);
const filtrar = (consulta: string, filtros: Parameters<typeof filtrarFilas>[3]) =>
  ids(filtrarFilas(sistema, bloque, consulta, filtros));

describe('claseDeFiltro', () => {
  it('elige el control según el tipo de campo', () => {
    const clase = (type: Bloque['fields'][number]['type']) =>
      claseDeFiltro({ key: 'k', label: 'K', type, required: false });

    expect(clase('select')).toBe('opcion');
    expect(clase('checkbox')).toBe('si-no');
    expect(clase('date')).toBe('fecha');
    expect(clase('number')).toBe('numero');
    expect(clase('currency')).toBe('numero');
    expect(clase('text')).toBe('texto');
    expect(clase('relation')).toBe('texto');
  });
});

describe('filtrarFilas', () => {
  it('sin búsqueda ni filtros devuelve todas las filas', () => {
    expect(filtrar('', {})).toEqual(['1', '2', '3', '4']);
    expect(filtrar('', { name: {}, status: { opcion: '' } })).toEqual(['1', '2', '3', '4']);
  });

  it('la búsqueda mira todas las columnas y no distingue tildes ni mayúsculas', () => {
    expect(filtrar('CÉRA', {})).toEqual(['3']);
    expect(filtrar('entregado', {})).toEqual(['1']);
  });

  it('un filtro de texto busca dentro de esa columna', () => {
    expect(filtrar('', { name: { texto: 'de ' } })).toEqual(['1', '2', '3']);
    expect(filtrar('', { name: { texto: 'regalo' } })).toEqual(['2']);
  });

  it('un filtro de lista deja solo la opción elegida', () => {
    expect(filtrar('', { status: { opcion: 'Pendiente' } })).toEqual(['2', '3']);
  });

  it('un filtro de sí/no mira el valor de la casilla', () => {
    expect(filtrar('', { paid: { opcion: 'si' } })).toEqual(['1', '3']);
    expect(filtrar('', { paid: { opcion: 'no' } })).toEqual(['2', '4']);
  });

  it('un rango de fechas incluye los bordes y deja afuera las filas sin fecha', () => {
    expect(filtrar('', { date: { desde: '2026-10-15' } })).toEqual(['2', '3']);
    expect(filtrar('', { date: { hasta: '2026-10-15' } })).toEqual(['1', '2']);
    expect(filtrar('', { date: { desde: '2026-10-15', hasta: '2026-10-15' } })).toEqual(['2']);
  });

  it('un rango de números incluye los bordes y deja afuera las filas vacías', () => {
    expect(filtrar('', { total: { desde: '10000' } })).toEqual(['1', '3']);
    expect(filtrar('', { total: { hasta: '13000' } })).toEqual(['1', '2']);
    expect(filtrar('', { total: { desde: '8200', hasta: '13000' } })).toEqual(['1', '2']);
  });

  it('la búsqueda y los filtros se combinan: hay que cumplir todo', () => {
    expect(filtrar('cera', { status: { opcion: 'Pendiente' } })).toEqual(['3']);
    expect(filtrar('cera', { status: { opcion: 'Entregado' } })).toEqual([]);
    expect(filtrar('', { status: { opcion: 'Pendiente' }, paid: { opcion: 'si' } })).toEqual(['3']);
  });

  it('en una columna de vínculo busca por el nombre de lo vinculado', () => {
    const clientas: Bloque = {
      ...bloque,
      id: 'clientas',
      type: 'clients',
      name: 'Clientas',
      fields: [{ key: 'name', label: 'Nombre', type: 'text', required: true }],
      rows: [{ id: 'c1', name: 'Carla Gómez' }],
    };
    const conVinculo: Bloque = {
      ...bloque,
      fields: [
        {
          key: 'customer',
          label: 'Cliente',
          type: 'relation',
          required: false,
          target: 'clientas',
        },
      ],
      rows: [
        { id: 'a', customer: 'c1' },
        { id: 'b', customer: '' },
      ],
    };
    const doble: Sistema = {
      ...sistema,
      sections: [{ id: 'sec', name: 'V', blocks: [conVinculo, clientas] }],
    };

    expect(ids(filtrarFilas(doble, conVinculo, '', { customer: { texto: 'carla' } }))).toEqual([
      'a',
    ]);
  });
});

describe('hayFiltros', () => {
  it('es verdadero con algo escrito en la búsqueda o en cualquier filtro', () => {
    expect(hayFiltros('', {})).toBe(false);
    expect(hayFiltros('  ', { name: {}, date: { desde: '', hasta: '' } })).toBe(false);
    expect(hayFiltros('cera', {})).toBe(true);
    expect(hayFiltros('', { total: { hasta: '5' } })).toBe(true);
  });
});
