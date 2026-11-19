import type {
  AgrupacionDeMetrica,
  CalculoDeMetrica,
  DefinicionDeMetrica,
  GraficoDeMetrica,
  ResultadoDeMetrica,
} from '@/types/metricas';

// Los mismos que GraficoMetrica del servidor: el número sirve siempre, las barras y la dona necesitan un reparto y la
// línea, algo que va en orden (los días de la semana).
const GRAFICOS_POSIBLES: Record<AgrupacionDeMetrica, GraficoDeMetrica[]> = {
  NINGUNA: ['NUMERO'],
  PRODUCTO: ['NUMERO', 'BARRAS', 'DONA'],
  CLIENTE: ['NUMERO', 'BARRAS', 'DONA'],
  CATEGORIA: ['NUMERO', 'BARRAS', 'DONA'],
  DIA_DE_LA_SEMANA: ['NUMERO', 'BARRAS', 'DONA', 'LINEA'],
};

export const graficosPosiblesPara = (agrupar: AgrupacionDeMetrica) => GRAFICOS_POSIBLES[agrupar];

/** Sin elegir: una métrica sin repartir es un número; repartida, barras. */
export const graficoDeSiempre = (agrupar: AgrupacionDeMetrica): GraficoDeMetrica =>
  graficosPosiblesPara(agrupar).length > 1 ? 'BARRAS' : 'NUMERO';

export const ETIQUETA_DEL_GRAFICO: Record<GraficoDeMetrica, string> = {
  NUMERO: 'Número',
  BARRAS: 'Barras',
  DONA: 'Torta',
  LINEA: 'Línea',
};

/** Cuántas partes se ven de entrada; el resto se suma en «Otros» o se ve con «Ver todo». */
export const PARTES_A_LA_VISTA = 5;

// En una suma las partes se pueden juntar en «Otros»; un promedio de varios no es la suma de sus promedios.
const SE_PUEDE_JUNTAR: Record<CalculoDeMetrica, boolean> = {
  SUMA: true,
  CONTEO: true,
  UNIDADES: true,
  PROMEDIO: false,
};

// Los días de la semana se muestran siempre los siete y en su orden: recortarlos rompería la semana.
const VA_EN_ORDEN: Record<AgrupacionDeMetrica, boolean> = {
  NINGUNA: false,
  PRODUCTO: false,
  CLIENTE: false,
  CATEGORIA: false,
  DIA_DE_LA_SEMANA: true,
};

export type Parte = { etiqueta: string; valor: number; otros?: boolean };

/** Lo que recibe cada dibujo: la receta, lo que dio y las partes que se ven. */
export type PropsDelGrafico = {
  definicion: DefinicionDeMetrica;
  resultado: ResultadoDeMetrica;
  partes: Parte[];
};

/** Las partes que se muestran: las cinco más grandes y, si suman, el resto junto en «Otros»; o todas, si se pide. */
export function partesParaMostrar(
  definicion: DefinicionDeMetrica,
  resultado: ResultadoDeMetrica,
  todas: boolean,
): Parte[] {
  if (todas || !hayMasPartes(definicion, resultado)) return resultado.filas;
  const primeras = resultado.filas.slice(0, PARTES_A_LA_VISTA);
  if (!SE_PUEDE_JUNTAR[definicion.calculo]) return primeras;
  return [...primeras, otrosDe(primeras, resultado.valor ?? 0)];
}

const otrosDe = (primeras: Parte[], total: number): Parte => ({
  etiqueta: 'Otros',
  valor: Math.max(0, total - primeras.reduce((suma, parte) => suma + parte.valor, 0)),
  otros: true,
});

/** {@code true} si hay más partes que las que se ven de entrada (los días de la semana se ven siempre todos). */
export const hayMasPartes = (definicion: DefinicionDeMetrica, resultado: ResultadoDeMetrica) =>
  !VA_EN_ORDEN[definicion.agrupar] && resultado.filas.length > PARTES_A_LA_VISTA;
