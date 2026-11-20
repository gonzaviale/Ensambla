import { formatoDelValor } from '@/lib/metricas/metricasAMedida';
import type {
  AgrupacionDeMetrica,
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

// Los días de la semana se muestran siempre los siete y en su orden: recortarlos rompería la semana.
const VA_EN_ORDEN: Record<AgrupacionDeMetrica, boolean> = {
  NINGUNA: false,
  PRODUCTO: false,
  CLIENTE: false,
  CATEGORIA: false,
  DIA_DE_LA_SEMANA: true,
};

export type Parte = { etiqueta: string; valor: number };

/** Lo que recibe cada dibujo: la receta, lo que dio y las partes que se ven. */
export type PropsDelGrafico = {
  definicion: DefinicionDeMetrica;
  resultado: ResultadoDeMetrica;
  partes: Parte[];
};

/** Lo más que se dibuja de un reparto: entran todas, sin botón para ver más ni «Otros». */
export const MAXIMO_DE_PARTES = 10;

/** Las partes que se muestran: todas las del reparto (los días de la semana siempre los siete), hasta diez. */
export const partesParaMostrar = (
  definicion: DefinicionDeMetrica,
  resultado: ResultadoDeMetrica,
): Parte[] =>
  VA_EN_ORDEN[definicion.agrupar] ? resultado.filas : resultado.filas.slice(0, MAXIMO_DE_PARTES);

// Los colores de la paleta de Ensambla, en orden, para las partes de cada gráfico.
const COLORES_DE_LA_PALETA = [
  '#00506a',
  '#ee8000',
  '#f85f5a',
  '#4eb2d4',
  '#ebce32',
  '#003e53',
  '#a95700',
  '#416048',
];

/** Lo que cada gráfico necesita saber de una parte: cómo se llama, cómo se escribe y de qué color va. */
export type DatoDelGrafico = {
  etiqueta: string;
  corta: string;
  valor: number;
  detalle: string;
  porcentaje: number;
  color: string;
};

const LARGO_DE_LA_ETIQUETA_CORTA = 11;

const recortar = (etiqueta: string, largo: number) =>
  etiqueta.length > largo ? `${etiqueta.slice(0, largo - 1).trimEnd()}…` : etiqueta;

const colorDeLaParte = (lugar: number) =>
  COLORES_DE_LA_PALETA[lugar % COLORES_DE_LA_PALETA.length] ?? '#00506a';

/** Las partes listas para dibujar: etiqueta corta para el eje, valor escrito, porcentaje del total y color. */
export function datosDelGrafico(
  partes: Parte[],
  formato: ResultadoDeMetrica['formato'],
  etiquetaCorta: (etiqueta: string) => string = (etiqueta) =>
    recortar(etiqueta, LARGO_DE_LA_ETIQUETA_CORTA),
): DatoDelGrafico[] {
  const total = partes.reduce((suma, parte) => suma + parte.valor, 0);
  return partes.map((parte, lugar) => ({
    etiqueta: parte.etiqueta,
    corta: etiquetaCorta(parte.etiqueta),
    valor: parte.valor,
    detalle: formatoDelValor(parte.valor, formato),
    porcentaje: total > 0 ? Math.round((parte.valor / total) * 100) : 0,
    color: colorDeLaParte(lugar),
  }));
}

/** Quién encabeza el reparto («Doña Rosa, $ 7.200»); los días de la semana no tienen primero. */
export function elPrimero(
  definicion: DefinicionDeMetrica,
  resultado: ResultadoDeMetrica,
): string | null {
  const primero = resultado.filas[0];
  if (!primero || VA_EN_ORDEN[definicion.agrupar]) return null;
  return `${primero.etiqueta}, ${formatoDelValor(primero.valor, resultado.formato)}`;
}

/** Qué es el número grande, para que no haya que adivinarlo (un promedio no es el de cada cliente). */
export function queEsElNumero(definicion: DefinicionDeMetrica): string {
  const repartida = definicion.agrupar !== 'NINGUNA';
  if (definicion.calculo === 'PROMEDIO') return repartida ? 'Promedio de todas juntas' : 'Promedio';
  return repartida ? 'Total de todas las partes' : 'Total';
}
