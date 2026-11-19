import type {
  AgrupacionDeMetrica,
  CalculoDeMetrica,
  DefinicionDeMetrica,
  FuenteDeMetrica,
  PeriodoDeMetrica,
} from '@/types/metricas';

// Lo mismo que el intérprete del servidor, en chico: busca frases fijas en el pedido (sin tildes ni mayúsculas) y
// arma la receta. Sin IA y sin números: el número lo pone después el cálculo de ejemplo.

export const NO_SE_ENTIENDE_LA_TABLA =
  'No me di cuenta de qué querés mirar. Decime si es de tus ventas, tus gastos o tu stock, por ejemplo: «cuánto vendí esta semana».';

const FUENTES: Record<FuenteDeMetrica, string[]> = {
  VENTAS: ['vend', 'venta', 'cobr', 'factur', 'ingres', 'fiad', 'me deben', 'me debe', 'deuda'],
  GASTOS: ['gast', 'pague', 'pagamos', 'egreso'],
  INVENTARIO: ['stock', 'inventario', 'mercaderia', 'me queda', 'quedan', 'deposito'],
};

const CALCULOS: Partial<Record<CalculoDeMetrica, string[]>> = {
  PROMEDIO: ['promedio', 'ticket'],
  CONTEO: ['cuantas ventas', 'cantidad de ventas', 'cantidad de gastos', 'cuantas veces'],
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
  CLIENTE: ['por cliente', 'cada cliente', 'que cliente', 'quien'],
  CATEGORIA: ['por categoria', 'cada categoria', 'en que gaste', 'en que gasto', 'tipo de gasto'],
  DIA_DE_LA_SEMANA: ['por dia', 'que dia', 'dia de la semana', 'cada dia'],
};

const PERIODOS: Partial<Record<PeriodoDeMetrica, string[]>> = {
  HOY: ['hoy'],
  SEMANA: ['esta semana', 'en la semana', 'semanal', 'ultima semana'],
  MES: ['este mes', ' mes', 'mensual'],
  TODO: ['en total', 'siempre', 'todo lo', 'historico'],
};

const FIADO = ['fiad', 'me deben', 'me debe', 'deuda'];

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

/** La receta que pide el texto, o el motivo por el que no se entendió. */
export function interpretarPedido(texto: string): DefinicionDeMetrica | { motivo: string } {
  const pedido = normalizado(texto);
  const fuente = loPrimeroQueMenciona(pedido, FUENTES);
  if (!fuente) return { motivo: NO_SE_ENTIENDE_LA_TABLA };
  const soloFiado = menciona(pedido, FIADO);
  return {
    nombre: nombreSugerido(texto),
    fuente,
    calculo: loPrimeroQueMenciona(pedido, CALCULOS) ?? 'SUMA',
    agrupar: loPrimeroQueMenciona(pedido, AGRUPACIONES) ?? 'NINGUNA',
    periodo: periodoDe(pedido, fuente, soloFiado),
    soloFiado,
  };
}

function periodoDe(pedido: string, fuente: FuenteDeMetrica, soloFiado: boolean): PeriodoDeMetrica {
  if (fuente === 'INVENTARIO') return 'TODO';
  return loPrimeroQueMenciona(pedido, PERIODOS) ?? (soloFiado ? 'TODO' : 'MES');
}
