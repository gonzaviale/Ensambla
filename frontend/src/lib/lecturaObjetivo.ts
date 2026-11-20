import type { ComoVieneTuNegocio } from '@/types/comoVieneTuNegocio';
import {
  EN_VENTAS,
  ES_ANIO_ANTES,
  ES_CANTIDAD,
  ES_PRECIO,
  IMPORTE,
  META_MINIMA,
  MILLON_EN_PALABRAS,
  MULTIPLICADOR,
  NOMBRE_PERIODO,
  PERIODOS,
  PORCENTAJE_MAS,
  POR_MES,
  RELATIVO,
  VERBOS,
  type Monto,
  type Periodo,
} from './lecturaObjetivoPatrones';
import { formatoPesos } from './objetivos';

/**
 * Qué entiende Sami del objetivo que escribió la persona, sin IA: busca los montos, descarta los que no son la meta
 * (precios de productos, cantidades, días, años, porcentajes), mira qué quiere hacer (ganar, retirar, vender, crecer)
 * y lo pasa a la meta que se mide: lo que queda por mes después de pagar mercadería y gastos.
 */

export type TipoDeObjetivo = 'GANAR' | 'RETIRAR' | 'VENDER' | 'CUBRIR_GASTOS' | 'CRECER';

export type LecturaDeObjetivo = {
  /** La meta por mes que se va a medir; null si Sami no la puede saber y la persona tiene que escribirla. */
  meta: number | null;
  tipo: TipoDeObjetivo;
  /** Qué entendió Sami, en una frase para la persona. */
  explicacion: string;
  /** Lo que conviene que la persona revise antes de guardar. */
  avisos: string[];
};

export type Negocio = Pick<ComoVieneTuNegocio, 'hayVentas' | 'vendiste' | 'queda'>;

/** Pasa a minúsculas y saca los acentos, para comparar palabras sin importar cómo se escribieron. */
function normalizar(texto: string) {
  return texto.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
}

/** El pedazo de frase donde está el monto: desde el último punto, coma con espacio, «y» o «pero» anterior. */
function clausulaAntes(texto: string, inicio: number) {
  const antes = texto.slice(0, inicio);
  const cortes = [...antes.matchAll(/[.;:!?]\s|,\s|\s(y|pero|aunque|sin)\s/g)];
  const ultimo = cortes.at(-1);
  return ultimo ? antes.slice((ultimo.index ?? 0) + ultimo[0].length) : antes;
}

function montosDe(texto: string): Monto[] {
  const montos: Monto[] = [...texto.matchAll(IMPORTE)].map((encontrado) => {
    const [completo, peso, entero = '', decimales = '', sufijo] = encontrado;
    const multiplicador = sufijo ? MULTIPLICADOR[sufijo] : undefined;
    const valor =
      Number(`${entero.replace(/[.,]/g, '')}.${decimales || '0'}`) * (multiplicador ?? 1);
    const inicio = encontrado.index ?? 0;
    return {
      valor: Math.round(valor),
      inicio,
      fin: inicio + completo.trimEnd().length,
      marcado: Boolean(peso) || multiplicador !== undefined,
    };
  });
  for (const encontrado of texto.matchAll(MILLON_EN_PALABRAS)) {
    const inicio = encontrado.index ?? 0;
    montos.push({
      valor: encontrado[1] === 'medio' ? 500_000 : 1_000_000,
      inicio,
      fin: inicio + encontrado[0].length,
      marcado: true,
    });
  }
  return montos.sort((a, b) => a.inicio - b.inicio);
}

/** Si el monto es una meta posible y no un precio, una cantidad, una fecha o un año. */
function esPlataDeLaMeta(texto: string, monto: Monto) {
  const despues = texto.slice(monto.fin);
  const clausula = clausulaAntes(texto, monto.inicio);
  if (ES_CANTIDAD.test(despues)) return false;
  if (/^\s*\/\s*\d/.test(despues) || /\d\s*\/\s*$/.test(texto.slice(0, monto.inicio))) return false;
  if (monto.valor < META_MINIMA) return false;
  const esAnio = !monto.marcado && monto.valor >= 1900 && monto.valor <= 2100;
  if (esAnio && ES_ANIO_ANTES.test(clausula)) return false;
  return !ES_PRECIO.test(clausula) || tipoEn(clausula) !== undefined;
}

/** El verbo más cercano antes del monto (o en todo el texto si no hay): qué quiere hacer la persona. */
function tipoEn(texto: string): TipoDeObjetivo | undefined {
  let mejor: { tipo: TipoDeObjetivo; indice: number } | undefined;
  for (const { tipo, patron } of VERBOS) {
    for (const encontrado of texto.matchAll(patron)) {
      const indice = encontrado.index ?? 0;
      if (!mejor || indice > mejor.indice) mejor = { tipo, indice };
    }
  }
  return mejor?.tipo;
}

/** El período que acompaña al monto: el primero que aparece después, en la misma frase; si no, el de antes. */
function periodoDe(despues: string, antes: string): Periodo {
  const hastaElCorte = despues.split(/[.;:!?]\s|,\s|\s(y|pero|aunque|sin)\s/)[0] ?? '';
  for (const tramo of [hastaElCorte, antes]) {
    const encontrados = PERIODOS.map(({ periodo, patron }) => ({
      periodo,
      indice: patron.exec(tramo)?.index,
    })).filter((encontrado) => encontrado.indice !== undefined);
    encontrados.sort((a, b) => (a.indice ?? 0) - (b.indice ?? 0));
    if (encontrados[0]) return encontrados[0].periodo;
  }
  return 'mes';
}

export const redondear = (valor: number) => Math.round(valor / 1_000) * 1_000;

function factorRelativo(texto: string): number | undefined {
  if (/\b(triplic\w*|el triple)\b/.test(texto)) return 3;
  if (RELATIVO.test(texto)) return 2;
  const porcentaje = PORCENTAJE_MAS.exec(texto);
  if (!porcentaje) return undefined;
  const numero = Number((porcentaje[1] ?? porcentaje[6] ?? '').replace(',', '.'));
  const esSuba = porcentaje[3] !== undefined || porcentaje[4] !== undefined;
  return esSuba && numero > 0 ? 1 + numero / 100 : undefined;
}

export function leerObjetivo(texto: string, negocio?: Negocio): LecturaDeObjetivo {
  const normal = normalizar(texto);
  const candidatos = montosDe(normal).filter((monto) => esPlataDeLaMeta(normal, monto));
  const elegido = montoElegido(normal, candidatos);
  const quedaHoy = negocio?.hayVentas && negocio.queda > 0 ? negocio.queda : undefined;
  if (!elegido) return lecturaSinMonto(normal, quedaHoy);
  return lecturaConMonto({ normal, elegido, candidatos, negocio, quedaHoy });
}

function montoElegido(normal: string, candidatos: Monto[]): Monto | undefined {
  const conVerbo = candidatos.filter((monto) => tipoEn(clausulaAntes(normal, monto.inicio)));
  return conVerbo.find((monto) => monto.marcado) ?? conVerbo[0] ?? candidatos[0];
}

function lecturaSinMonto(normal: string, quedaHoy: number | undefined): LecturaDeObjetivo {
  const factor = factorRelativo(normal);
  if (factor) return lecturaDeCrecimiento(factor, quedaHoy);
  return lecturaSinMeta(tipoEn(normal) ?? 'GANAR');
}

function nombreDelCrecimiento(factor: number): string {
  if (factor === 2) return 'el doble';
  if (factor > 2) return 'el triple';
  return `un ${Math.round((factor - 1) * 100)} % más`;
}

function lecturaDeCrecimiento(factor: number, quedaHoy: number | undefined): LecturaDeObjetivo {
  const crecimiento = nombreDelCrecimiento(factor);
  if (!quedaHoy) {
    return {
      meta: null,
      tipo: 'CRECER',
      explicacion: `Querés ganar ${crecimiento}, pero todavía no tengo tus ventas para calcularlo.`,
      avisos: ['Escribí abajo cuánto querés ganar por mes, en pesos.'],
    };
  }
  return {
    meta: redondear(quedaHoy * factor),
    tipo: 'CRECER',
    explicacion: `Ganar ${crecimiento} de lo que te queda hoy (${formatoPesos(quedaHoy)} por mes).`,
    avisos: [],
  };
}

function lecturaSinMeta(tipo: TipoDeObjetivo): LecturaDeObjetivo {
  const explicacion =
    tipo === 'CUBRIR_GASTOS'
      ? 'Querés cubrir tus gastos. Sami mide lo que te queda después de pagar la mercadería y los gastos.'
      : 'No encontré cuánto querés ganar.';
  return {
    meta: null,
    tipo,
    explicacion,
    avisos: ['Escribí abajo cuánto querés que te quede por mes, en pesos.'],
  };
}

type Lectura = {
  normal: string;
  elegido: Monto;
  candidatos: Monto[];
  negocio?: Negocio;
  quedaHoy: number | undefined;
};

function lecturaConMonto({ normal, elegido, candidatos, negocio, quedaHoy }: Lectura) {
  const clausula = clausulaAntes(normal, elegido.inicio);
  const despues = normal.slice(elegido.fin);
  const tipo = EN_VENTAS.test(despues) ? 'VENDER' : (tipoEn(clausula) ?? tipoEn(normal) ?? 'GANAR');
  const periodo = periodoDe(despues, clausula);
  const mensual = periodo === 'mes' ? elegido.valor : redondear(elegido.valor * POR_MES[periodo]);
  const escrito = descripcionDelMonto(elegido.valor, mensual, periodo);
  const avisos = avisoDeMontosDistintos(candidatos, elegido);
  if (tipo === 'VENDER') return lecturaDeVentas({ escrito, mensual, avisos, negocio });
  return lecturaDeGanancia({ tipo, escrito, mensual, avisos, quedaHoy });
}

function descripcionDelMonto(valor: number, mensual: number, periodo: Periodo): string {
  if (periodo === 'mes') return `${formatoPesos(valor)} por mes`;
  return `${formatoPesos(valor)} ${NOMBRE_PERIODO[periodo]} (unos ${formatoPesos(mensual)} por mes)`;
}

function avisoDeMontosDistintos(candidatos: Monto[], elegido: Monto): string[] {
  const distintos = new Set(candidatos.map((monto) => monto.valor));
  if (distintos.size <= 1) return [];
  const otros = [...distintos].filter((valor) => valor !== elegido.valor).map(formatoPesos);
  return [
    `También escribiste ${otros.join(' y ')}. Tomé ${formatoPesos(elegido.valor)} como meta.`,
  ];
}

type DatosDeVentas = { escrito: string; mensual: number; avisos: string[]; negocio?: Negocio };

function lecturaDeVentas({ escrito, mensual, avisos, negocio }: DatosDeVentas): LecturaDeObjetivo {
  const margen = negocio?.hayVentas && negocio.vendiste > 0 ? negocio.queda / negocio.vendiste : 0;
  if (margen <= 0) {
    return {
      meta: null,
      tipo: 'VENDER',
      explicacion: `Vender ${escrito}. Sami mide lo que te queda después de pagar mercadería y gastos, no lo que vendés.`,
      avisos: [...avisos, 'Escribí abajo cuánto querés que te quede por mes, en pesos.'],
    };
  }
  const meta = redondear(mensual * margen);
  return {
    meta,
    tipo: 'VENDER',
    explicacion: `Vender ${escrito}. Con lo que te queda hoy de cada venta (${Math.round(margen * 100)} %), eso son unos ${formatoPesos(meta)} de ganancia por mes: eso mide Sami.`,
    avisos,
  };
}

type DatosDeGanancia = {
  tipo: TipoDeObjetivo;
  escrito: string;
  mensual: number;
  avisos: string[];
  quedaHoy: number | undefined;
};

const EXPLICACION_DE_GANANCIA: Partial<Record<TipoDeObjetivo, (escrito: string) => string>> = {
  RETIRAR: (escrito) =>
    `Retirar ${escrito}: el negocio tiene que dejarte al menos eso después de pagar mercadería y gastos.`,
  CUBRIR_GASTOS: (escrito) =>
    `Juntar ${escrito} para tus gastos. Si ya los anotás como gastos en una sección, Sami ya los descuenta: la meta es lo que te queda aparte.`,
};

const explicacionPorDefecto = (escrito: string) =>
  `Ganar ${escrito}, después de pagar la mercadería y los gastos.`;

function lecturaDeGanancia(datos: DatosDeGanancia): LecturaDeObjetivo {
  const { tipo, escrito, mensual, quedaHoy } = datos;
  const explicar = EXPLICACION_DE_GANANCIA[tipo] ?? explicacionPorDefecto;
  const yaLaCumple = quedaHoy !== undefined && mensual <= quedaHoy;
  const avisoDeCumplida = yaLaCumple
    ? [`Hoy ya te quedan ${formatoPesos(quedaHoy)} por mes: esta meta ya la cumplís.`]
    : [];
  return {
    meta: mensual,
    tipo,
    explicacion: explicar(escrito),
    avisos: [...datos.avisos, ...avisoDeCumplida],
  };
}

export { alcanceDeLaMeta, type AlcanceDeLaMeta } from './lecturaObjetivoAlcance';
