import type {
  AgrupacionDeMetrica,
  CalculoDeMetrica,
  DefinicionDeMetrica,
  FuenteDeMetrica,
  LecturaDelPedido,
  ParteDeLaReceta,
  PeriodoDeMetrica,
} from '@/types/metricas';

// Lo mismo que el intérprete del servidor, en chico: busca frases fijas en el pedido (sin tildes ni mayúsculas) y
// arma la receta. Sin IA y sin números: el número lo pone después el cálculo de ejemplo. Como el servidor, cuenta qué
// tomó por defecto y avisa lo que pide algo que no se calcula («el finde», «ayer»); las demás palabras desconocidas
// sólo las detecta el servidor, que tiene la lista de palabras vacías.

export const NO_SE_ENTIENDE_LA_TABLA =
  'No me di cuenta de qué querés mirar. Decime si es de tus ventas, tus gastos o tu stock, por ejemplo: «cuánto vendí esta semana».';

const FUENTES: Record<FuenteDeMetrica, string[]> = {
  VENTAS: [
    'vend',
    'venta',
    'cobr',
    'factur',
    'recaud',
    'ingres',
    'cuanto hice',
    'fiad',
    'me deben',
    'me debe',
    'deuda',
    'deudor',
  ],
  GASTOS: ['gast', 'pague', 'pagamos', 'pago', 'egreso', 'compr'],
  INVENTARIO: [
    'stock',
    'inventario',
    'mercaderia',
    'me queda',
    'quedan',
    'deposito',
    'productos tengo',
    'productos hay',
  ],
};

const CALCULOS: Partial<Record<CalculoDeMetrica, string[]>> = {
  PROMEDIO: ['promedio', 'ticket'],
  CONTEO: [
    'cuantas ventas',
    'cantidad de ventas',
    'cantidad de gastos',
    'cuantas veces',
    'cuantos productos',
  ],
  UNIDADES: [
    'unidades',
    'mas vendido',
    'producto vendo mas',
    'productos vendo mas',
    'cuanto stock',
    'me queda',
  ],
};

const AGRUPACIONES: Partial<Record<AgrupacionDeMetrica, string[]>> = {
  PRODUCTO: ['por producto', 'cada producto', 'que producto', 'mas vendido'],
  CLIENTE: ['por cliente', 'cada cliente', 'que cliente', 'quien', 'deudores'],
  CATEGORIA: ['por categoria', 'cada categoria', 'en que gaste', 'en que gasto', 'tipo de gasto'],
  DIA_DE_LA_SEMANA: ['por dia', 'que dia', 'dia de la semana', 'cada dia'],
};

const PERIODOS: Partial<Record<PeriodoDeMetrica, string[]>> = {
  HOY: ['hoy'],
  SEMANA: ['esta semana', 'en la semana', 'semanal', 'ultima semana'],
  MES: ['este mes', ' mes', 'mensual'],
  TODO: ['en total', 'siempre', 'todo lo', 'historico'],
};

const FIADO = ['fiad', 'me deben', 'me debe', 'deuda', 'deudor'];

const NO_SE_CALCULAN = [
  'fin de semana',
  'finde',
  'ayer',
  'anoche',
  'semana pasada',
  'mes pasado',
  'por mes',
];

const normalizado = (texto: string) =>
  ` ${texto
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, ' ')} `;

const menciona = (pedido: string, frases: string[] = []) =>
  frases.some((frase) => pedido.includes(frase));

function loPrimeroQueMenciona<T extends string>(
  pedido: string,
  frasesDe: Partial<Record<T, string[]>>,
) {
  return (Object.keys(frasesDe) as T[]).find((opcion) => menciona(pedido, frasesDe[opcion]));
}

const nombreSugerido = (texto: string) => {
  const limpio = texto
    .replace(/^[\s¿¡?!.,;:]+|[\s¿¡?!.,;:]+$/g, '')
    .slice(0, 80)
    .trim();
  return limpio.charAt(0).toUpperCase() + limpio.slice(1);
};

type LoQueDice = {
  fuente: FuenteDeMetrica;
  calculo?: CalculoDeMetrica;
  agrupar?: AgrupacionDeMetrica;
  periodo?: PeriodoDeMetrica;
  soloFiado: boolean;
};

export type PedidoEntendido = { definicion: DefinicionDeMetrica; lectura: LecturaDelPedido };

/** La receta que pide el texto con cómo se lo entendió, o el motivo por el que no se entendió. */
export function interpretarPedido(texto: string): PedidoEntendido | { motivo: string } {
  const escrito = normalizado(texto);
  const calculable = sinLoQueNoSeCalcula(escrito);
  const fuente = loPrimeroQueMenciona(calculable, FUENTES);
  if (!fuente) return { motivo: NO_SE_ENTIENDE_LA_TABLA };
  const loQueDice = loQueDiceDe(calculable, fuente);
  return {
    definicion: recetaDe(texto, loQueDice),
    lectura: lecturaDe(loQueDice, loQueNoSeCalculaEn(escrito)),
  };
}

const sinLoQueNoSeCalcula = (pedido: string) =>
  NO_SE_CALCULAN.reduce((resto, frase) => resto.replace(` ${frase} `, ' '), pedido);

const loQueNoSeCalculaEn = (pedido: string) =>
  NO_SE_CALCULAN.filter((frase) => pedido.includes(` ${frase} `));

// El stock es lo que hay hoy: no tiene un período que pedir.
const loQueDiceDe = (pedido: string, fuente: FuenteDeMetrica): LoQueDice => ({
  fuente,
  calculo: loPrimeroQueMenciona(pedido, CALCULOS),
  agrupar: loPrimeroQueMenciona(pedido, AGRUPACIONES),
  periodo: fuente === 'INVENTARIO' ? undefined : loPrimeroQueMenciona(pedido, PERIODOS),
  soloFiado: menciona(pedido, FIADO),
});

const recetaDe = (texto: string, loQueDice: LoQueDice): DefinicionDeMetrica => ({
  nombre: nombreSugerido(texto),
  fuente: loQueDice.fuente,
  calculo: loQueDice.calculo ?? 'SUMA',
  agrupar: loQueDice.agrupar ?? 'NINGUNA',
  periodo: loQueDice.periodo ?? periodoDeSiempre(loQueDice),
  soloFiado: loQueDice.soloFiado,
});

// Lo que te deben se acumula y el stock es lo de hoy: sin un período dicho, miran todo lo cargado.
const periodoDeSiempre = ({ fuente, soloFiado }: LoQueDice): PeriodoDeMetrica =>
  fuente === 'INVENTARIO' || soloFiado ? 'TODO' : 'MES';

const LO_QUE_DIJO: Record<ParteDeLaReceta, (loQueDice: LoQueDice) => boolean> = {
  FUENTE: () => true,
  CALCULO: (loQueDice) => loQueDice.calculo !== undefined,
  AGRUPACION: (loQueDice) => loQueDice.agrupar !== undefined,
  PERIODO: (loQueDice) => loQueDice.periodo !== undefined,
  SOLO_FIADO: (loQueDice) => loQueDice.soloFiado,
};

const SE_TOMA_POR_DEFECTO: Record<ParteDeLaReceta, (loQueDice: LoQueDice) => boolean> = {
  FUENTE: () => false,
  CALCULO: (loQueDice) => !LO_QUE_DIJO.CALCULO(loQueDice),
  AGRUPACION: (loQueDice) => !LO_QUE_DIJO.AGRUPACION(loQueDice),
  PERIODO: (loQueDice) => !LO_QUE_DIJO.PERIODO(loQueDice) && loQueDice.fuente !== 'INVENTARIO',
  SOLO_FIADO: () => false,
};

const PARTES = Object.keys(LO_QUE_DIJO) as ParteDeLaReceta[];

const lecturaDe = (loQueDice: LoQueDice, noEntendidas: string[]): LecturaDelPedido => ({
  delPedido: PARTES.filter((parte) => LO_QUE_DIJO[parte](loQueDice)),
  porDefecto: PARTES.filter((parte) => SE_TOMA_POR_DEFECTO[parte](loQueDice)),
  noEntendidas,
});
