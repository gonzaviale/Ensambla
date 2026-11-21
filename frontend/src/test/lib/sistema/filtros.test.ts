import { describe, it } from 'vitest';
import { claseDeFiltro, filtrarFilas, hayFiltros } from '@/lib/sistema/filtros';
import { cuandoSeCalcula, entoncesElResultadoEs } from '@/test/calculo';
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

type Filtros = Parameters<typeof filtrarFilas>[3];

const ids = (filas: Fila[]) => filas.map((fila) => fila.id);

const cuandoSeFiltra = (consulta: string, filtros: Filtros) =>
  cuandoSeCalcula(() => ids(filtrarFilas(sistema, bloque, consulta, filtros)));

const entoncesQuedan = (...esperadas: string[]) => entoncesElResultadoEs(esperadas);

const campoDeTipo = (type: Bloque['fields'][number]['type']) => ({
  key: 'k',
  label: 'K',
  type,
  required: false,
});

describe('claseDeFiltro', () => {
  it.each([
    ['select', 'opcion'],
    ['checkbox', 'si-no'],
    ['date', 'fecha'],
    ['number', 'numero'],
    ['currency', 'numero'],
    ['text', 'texto'],
    ['relation', 'texto'],
  ] as const)('elige el control de %s según el tipo de campo: %s', (tipo, clase) => {
    cuandoSeCalcula(() => claseDeFiltro(campoDeTipo(tipo)));

    entoncesElResultadoEs(clase);
  });
});

describe('filtrarFilas', () => {
  it('sin búsqueda ni filtros devuelve todas las filas', () => {
    cuandoSeFiltra('', {});

    entoncesQuedan('1', '2', '3', '4');
  });

  it('los filtros vacíos no filtran nada', () => {
    cuandoSeFiltra('', { name: {}, status: { opcion: '' } });

    entoncesQuedan('1', '2', '3', '4');
  });

  it.each([
    ['CÉRA', ['3']],
    ['entregado', ['1']],
  ])(
    'la búsqueda «%s» mira todas las columnas y no distingue tildes ni mayúsculas',
    (consulta, filas) => {
      cuandoSeFiltra(consulta, {});

      entoncesQuedan(...filas);
    },
  );

  it.each([
    ['de ', ['1', '2', '3']],
    ['regalo', ['2']],
  ])('un filtro de texto «%s» busca dentro de esa columna', (texto, filas) => {
    cuandoSeFiltra('', { name: { texto } });

    entoncesQuedan(...filas);
  });

  it('un filtro de lista deja solo la opción elegida', () => {
    cuandoSeFiltra('', { status: { opcion: 'Pendiente' } });

    entoncesQuedan('2', '3');
  });

  it.each([
    ['si', ['1', '3']],
    ['no', ['2', '4']],
  ])('un filtro de sí/no «%s» mira el valor de la casilla', (opcion, filas) => {
    cuandoSeFiltra('', { paid: { opcion } });

    entoncesQuedan(...filas);
  });

  it.each([
    [{ desde: '2026-10-15' }, ['2', '3']],
    [{ hasta: '2026-10-15' }, ['1', '2']],
    [{ desde: '2026-10-15', hasta: '2026-10-15' }, ['2']],
  ])(
    'un rango de fechas %j incluye los bordes y deja afuera las filas sin fecha',
    (rango, filas) => {
      cuandoSeFiltra('', { date: rango });

      entoncesQuedan(...filas);
    },
  );

  it.each([
    [{ desde: '10000' }, ['1', '3']],
    [{ hasta: '13000' }, ['1', '2']],
    [{ desde: '8200', hasta: '13000' }, ['1', '2']],
  ])('un rango de números %j incluye los bordes y deja afuera las filas vacías', (rango, filas) => {
    cuandoSeFiltra('', { total: rango });

    entoncesQuedan(...filas);
  });

  it('la búsqueda y un filtro se combinan: hay que cumplir los dos', () => {
    cuandoSeFiltra('cera', { status: { opcion: 'Pendiente' } });

    entoncesQuedan('3');
  });

  it('si la búsqueda y el filtro no coinciden no queda nada', () => {
    cuandoSeFiltra('cera', { status: { opcion: 'Entregado' } });

    entoncesQuedan();
  });

  it('dos filtros se combinan: hay que cumplir los dos', () => {
    cuandoSeFiltra('', { status: { opcion: 'Pendiente' }, paid: { opcion: 'si' } });

    entoncesQuedan('3');
  });

  it('en una columna de vínculo busca por el nombre de lo vinculado', () => {
    cuandoSeFiltraUnaColumnaDeVinculoPorNombre('carla');

    entoncesQuedan('a');
  });
});

describe('hayFiltros', () => {
  it.each([
    ['', {}, false],
    ['  ', { name: {}, date: { desde: '', hasta: '' } }, false],
    ['cera', {}, true],
    ['', { total: { hasta: '5' } }, true],
  ] as [string, Filtros, boolean][])(
    'con la búsqueda «%s» y los filtros %j dice %s',
    (consulta, filtros, hay) => {
      cuandoSeCalcula(() => hayFiltros(consulta, filtros));

      entoncesElResultadoEs(hay);
    },
  );
});

function cuandoSeFiltraUnaColumnaDeVinculoPorNombre(texto: string) {
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
      { key: 'customer', label: 'Cliente', type: 'relation', required: false, target: 'clientas' },
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
  cuandoSeCalcula(() => ids(filtrarFilas(doble, conVinculo, '', { customer: { texto } })));
}
