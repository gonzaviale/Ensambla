import { pesos } from '../alertas';
import { normalize } from '../core';
import {
  destinoDe,
  esDeLosUltimos30Dias,
  plural,
  productosDe,
  tiene,
  ventasDe,
  type ContextoDeChat,
  type ItemDeChat,
  type Venta,
} from './datos';

type Periodo = { desde: number; hasta: number; nombre: string };

const MAS_VENDIDOS_A_MOSTRAR = 3;

/** Los periodos que se reconocen en la pregunta, en orden: el primero que aparece es el que se usa. */
const PERIODOS: { palabras: string[]; periodo: Periodo }[] = [
  { palabras: ['ayer'], periodo: { desde: 1, hasta: 1, nombre: 'ayer' } },
  { palabras: ['hoy', 'del dia'], periodo: { desde: 0, hasta: 0, nombre: 'hoy' } },
  {
    palabras: ['semana', '7 dias'],
    periodo: { desde: 0, hasta: 6, nombre: 'en los últimos 7 días' },
  },
  {
    palabras: ['mes', '30 dias'],
    periodo: { desde: 0, hasta: 29, nombre: 'en los últimos 30 días' },
  },
];

const RESUMEN_SIN_PERIODO: Periodo[] = [
  { desde: 0, hasta: 0, nombre: 'Hoy' },
  { desde: 0, hasta: 6, nombre: 'Últimos 7 días' },
  { desde: 0, hasta: 29, nombre: 'Últimos 30 días' },
];

const periodoDe = (texto: string) =>
  PERIODOS.find(({ palabras }) => tiene(texto, palabras))?.periodo;

function lineaDelPeriodo(ventas: Venta[], { desde, hasta, nombre }: Periodo) {
  const dentro = ventas.filter((venta) => venta.dias >= desde && venta.dias <= hasta);
  const total = dentro.reduce((acumulado, venta) => acumulado + venta.total, 0);
  const unidades = dentro.reduce((acumulado, venta) => acumulado + venta.unidades, 0);
  return `${nombre}: ${pesos(total)} (${plural(unidades, 'unidad', 'unidades')} en ${plural(dentro.length, 'venta', 'ventas')})`;
}

const lineasDeVentas = (ventas: Venta[], pregunta: string) => {
  const periodo = periodoDe(normalize(pregunta));
  if (!periodo) return RESUMEN_SIN_PERIODO.map((resumen) => lineaDelPeriodo(ventas, resumen));
  return [lineaDelPeriodo(ventas, { ...periodo, nombre: `Vendiste ${periodo.nombre}` })];
};

export function ventasGenerales(pregunta: string, contexto: ContextoDeChat) {
  const ventas = ventasDe(contexto.sistema, contexto.hoy);
  if (!ventas.length) {
    return {
      texto: 'Todavía no hay ventas anotadas en esta sección.',
      sugerencias: ['¿Qué me falta?'],
    };
  }
  const mas = masVendidosDe(ventas, contexto);
  return {
    texto: `${lineasDeVentas(ventas, pregunta).join('\n')}${mas.length ? '\n\nLo que más salió:' : ''}`,
    items: mas,
    sugerencias: ['¿Qué avisos tengo?', '¿Qué me falta?'],
  };
}

function unidadesPorProducto(ventas: Venta[]) {
  const porProducto = new Map<string, number>();
  for (const venta of ventas.filter(esDeLosUltimos30Dias)) {
    porProducto.set(venta.productoId, (porProducto.get(venta.productoId) ?? 0) + venta.unidades);
  }
  return porProducto;
}

function masVendidosDe(ventas: Venta[], contexto: ContextoDeChat): ItemDeChat[] {
  const productos = productosDe(contexto.sistema);
  return [...unidadesPorProducto(ventas).entries()]
    .sort((a, b) => b[1] - a[1])
    .slice(0, MAS_VENDIDOS_A_MOSTRAR)
    .flatMap(([id, unidades]) => {
      const producto = productos.find((item) => item.fila.id === id);
      if (!producto) return [];
      return [
        {
          id,
          titulo: producto.nombre,
          detalle: `${plural(unidades, 'unidad', 'unidades')} en 30 días`,
          destino: destinoDe(producto, contexto.sistema),
        },
      ];
    });
}

export function masVendidos(contexto: ContextoDeChat) {
  const respuesta = ventasGenerales('mes', contexto);
  return {
    ...respuesta,
    texto: respuesta.items?.length ? 'Lo que más salió en los últimos 30 días:' : respuesta.texto,
  };
}
