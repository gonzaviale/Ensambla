import { render, screen, within } from '@testing-library/react';
import { expect } from 'vitest';
import { LoQueEntendi } from '@/components/sistema/metricas/LoQueEntendi';
import type {
  DefinicionDeMetrica,
  LecturaDelPedido,
  PedidoInterpretado,
  ResultadoDeMetrica,
} from '@/types/metricas';

// Pedidos ya interpretados, como los devuelve el servidor, para mirar el recuadro «Esto entendí» sin red.

const SIN_VENTAS: ResultadoDeMetrica = {
  formato: 'MONEDA',
  valor: null,
  filas: [],
  motivo: 'No hay ventas en este período.',
  deDondeSale: { fuente: 'VENTAS', filasUsadas: 0, filasSinDato: 0 },
};

const interpretado = (
  definicion: DefinicionDeMetrica,
  lectura: LecturaDelPedido,
): PedidoInterpretado => ({ definicion, resultado: SIN_VENTAS, lectura });

export const EL_FINDE = interpretado(
  {
    nombre: 'Cuanto vendi el finde',
    fuente: 'VENTAS',
    calculo: 'SUMA',
    agrupar: 'NINGUNA',
    periodo: 'MES',
    soloFiado: false,
  },
  {
    delPedido: ['FUENTE'],
    porDefecto: ['CALCULO', 'AGRUPACION', 'PERIODO'],
    noEntendidas: ['finde'],
  },
);

export const LOS_PRODUCTOS_QUE_TENGO = interpretado(
  {
    nombre: 'Cuantos productos tengo',
    fuente: 'INVENTARIO',
    calculo: 'CONTEO',
    agrupar: 'NINGUNA',
    periodo: 'TODO',
    soloFiado: false,
  },
  { delPedido: ['FUENTE', 'CALCULO'], porDefecto: ['AGRUPACION'], noEntendidas: [] },
);

export const LO_QUE_ME_DEBEN_POR_CLIENTE = interpretado(
  {
    nombre: 'Cuanto me deben por cliente',
    fuente: 'VENTAS',
    calculo: 'SUMA',
    agrupar: 'CLIENTE',
    periodo: 'TODO',
    soloFiado: true,
  },
  {
    delPedido: ['FUENTE', 'AGRUPACION', 'SOLO_FIADO'],
    porDefecto: ['CALCULO', 'PERIODO'],
    noEntendidas: [],
  },
);

export const dadoElPedidoInterpretado = (propuesta: PedidoInterpretado) =>
  render(<LoQueEntendi propuesta={propuesta} />);

const elRecuadro = () => screen.getByRole('region', { name: 'Esto entendí' });

const recuadro = () => within(elRecuadro());

const lineasDe = (lugar: HTMLElement) =>
  within(lugar)
    .getAllByRole('listitem')
    .map((linea) => linea.textContent);

const lineas = () => lineasDe(elRecuadro());

export const entoncesEntendio = (esperadas: string[]) => expect(lineas()).toEqual(esperadas);

export const entoncesNoEntendio = (texto: string) =>
  expect(recuadro().getByText(`No entendí: ${texto}`)).toBeInTheDocument();

/** Para las pantallas: espera a que el servidor responda y aparezca el recuadro. */
export const entoncesApareceQueEntendio = async (linea: string, noEntendidas: string) => {
  const enPantalla = await screen.findByRole('region', { name: 'Esto entendí' });
  expect(lineasDe(enPantalla)).toContain(linea);
  expect(within(enPantalla).getByText(`No entendí: ${noEntendidas}`)).toBeInTheDocument();
};

export const entoncesEntendioTodasLasPalabras = () =>
  expect(recuadro().queryByText(/No entendí/)).not.toBeInTheDocument();
