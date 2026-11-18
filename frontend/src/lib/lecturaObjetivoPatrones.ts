import type { TipoDeObjetivo } from './lecturaObjetivo';

export type Periodo = 'dia' | 'semana' | 'mes' | 'anio';

export type Monto = {
  valor: number;
  inicio: number;
  fin: number;
  /** Lleva «$» o un multiplicador («mil», «k», «palos»): seguro es plata. */
  marcado: boolean;
};

/** Menos que esto no es una meta mensual: es un día, una cantidad o un precio chico. */
export const META_MINIMA = 1_000;

export const POR_MES: Record<Periodo, number> = { dia: 30, semana: 30 / 7, mes: 1, anio: 1 / 12 };
export const NOMBRE_PERIODO: Record<Periodo, string> = {
  dia: 'por día',
  semana: 'por semana',
  mes: 'por mes',
  anio: 'por año',
};

export const MULTIPLICADOR: Record<string, number> = {
  k: 1_000,
  mil: 1_000,
  luca: 1_000,
  lucas: 1_000,
  m: 1_000_000,
  millon: 1_000_000,
  millones: 1_000_000,
  palo: 1_000_000,
  palos: 1_000_000,
};

export const IMPORTE =
  /(\$\s*)?(\d{1,3}(?:[.,]\d{3})+|\d+)(?:[.,](\d{1,2}))?\s*(k|mil|lucas?|millones|millon|m|palos?)?(?![a-z\d])/g;
export const MILLON_EN_PALABRAS = /\b(un|medio)\s+(millon|palo)\b/g;

export const VERBOS: { tipo: TipoDeObjetivo; patron: RegExp }[] = [
  { tipo: 'RETIRAR', patron: /\b(retir\w*|sac\w*|sueldo|llevarme|llevo|cobrarme)\b/g },
  {
    tipo: 'GANAR',
    patron: /\b(gan\w*|qued\w*|dej\w*|limpio|neto|utilidad|rentabilidad|llegar|juntar|ahorrar)\b/g,
  },
  { tipo: 'VENDER', patron: /\b(vend\w*|ventas?|factur\w*|recaud\w*)\b/g },
  { tipo: 'CUBRIR_GASTOS', patron: /\b(gastos?|cubrir|alquiler|cuentas|recuperar)\b/g },
];

/** Palabras que dicen que el monto es el precio de un producto, no la meta. */
export const ES_PRECIO =
  /\b(precio|precios|cobr\w*|vale|cuesta|sale|subi\w*|baj\w*|pon\w*)\b[^$\d]*(a|en)?\s*$/;
export const ES_CANTIDAD =
  /^\s*(unidades|u\b|kilos?|kg|litros?|cajas?|paquetes?|clientes?|productos?|dias?|semanas?|meses|anos?|horas?|veces|%|por ciento)/;
export const ES_ANIO_ANTES = /\b(el|del|hasta|antes de|para|en|de|año|ano)\s*$/;
/** «400 mil en ventas», «1 millón de facturación»: el monto es lo que se vende. */
export const EN_VENTAS = /^\s*(en|de)\s+(ventas|facturacion|lo que vendo)/;
export const RELATIVO = /\b(duplic\w*|el doble|triplic\w*|el triple)\b/;
export const PORCENTAJE_MAS =
  /(\d+(?:[.,]\d+)?)\s*(%|por ciento)\s*(mas|arriba)?|\b(subir|aumentar|crecer|mejorar)\w*\s+(un|el)?\s*(\d+(?:[.,]\d+)?)\s*(%|por ciento)/;

export const PERIODOS: { periodo: Periodo; patron: RegExp }[] = [
  { periodo: 'dia', patron: /\b(por|al|cada|a)\s+dia\b|\bdiari\w*/ },
  { periodo: 'semana', patron: /\b(por|a la|cada|la)\s+semana\b|\bsemanal\w*/ },
  { periodo: 'mes', patron: /\b(por|al|cada|el|a)\s+mes\b|\bmensual\w*/ },
  { periodo: 'anio', patron: /\b(por|al|cada|el)\s+ano\b|\banual\w*/ },
];
