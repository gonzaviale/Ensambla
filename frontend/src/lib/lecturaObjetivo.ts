import type { ComoVieneTuNegocio } from '@/types/comoVieneTuNegocio';
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

type Negocio = Pick<ComoVieneTuNegocio, 'hayVentas' | 'vendiste' | 'queda'>;

type Periodo = 'dia' | 'semana' | 'mes' | 'anio';

type Monto = {
  valor: number;
  inicio: number;
  fin: number;
  /** Lleva «$» o un multiplicador («mil», «k», «palos»): seguro es plata. */
  marcado: boolean;
};

/** Menos que esto no es una meta mensual: es un día, una cantidad o un precio chico. */
const META_MINIMA = 1_000;

const POR_MES: Record<Periodo, number> = { dia: 30, semana: 30 / 7, mes: 1, anio: 1 / 12 };
const NOMBRE_PERIODO: Record<Periodo, string> = {
  dia: 'por día',
  semana: 'por semana',
  mes: 'por mes',
  anio: 'por año',
};

const MULTIPLICADOR: Record<string, number> = {
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

const IMPORTE =
  /(\$\s*)?(\d{1,3}(?:[.,]\d{3})+|\d+)(?:[.,](\d{1,2}))?\s*(k|mil|lucas?|millones|millon|m|palos?)?(?![a-z\d])/g;
const MILLON_EN_PALABRAS = /\b(un|medio)\s+(millon|palo)\b/g;

const VERBOS: { tipo: TipoDeObjetivo; patron: RegExp }[] = [
  { tipo: 'RETIRAR', patron: /\b(retir\w*|sac\w*|sueldo|llevarme|llevo|cobrarme)\b/g },
  {
    tipo: 'GANAR',
    patron: /\b(gan\w*|qued\w*|dej\w*|limpio|neto|utilidad|rentabilidad|llegar|juntar|ahorrar)\b/g,
  },
  { tipo: 'VENDER', patron: /\b(vend\w*|ventas?|factur\w*|recaud\w*)\b/g },
  { tipo: 'CUBRIR_GASTOS', patron: /\b(gastos?|cubrir|alquiler|cuentas|recuperar)\b/g },
];

/** Palabras que dicen que el monto es el precio de un producto, no la meta. */
const ES_PRECIO =
  /\b(precio|precios|cobr\w*|vale|cuesta|sale|subi\w*|baj\w*|pon\w*)\b[^$\d]*(a|en)?\s*$/;
const ES_CANTIDAD =
  /^\s*(unidades|u\b|kilos?|kg|litros?|cajas?|paquetes?|clientes?|productos?|dias?|semanas?|meses|anos?|horas?|veces|%|por ciento)/;
const ES_ANIO_ANTES = /\b(el|del|hasta|antes de|para|en|de|año|ano)\s*$/;
/** «400 mil en ventas», «1 millón de facturación»: el monto es lo que se vende. */
const EN_VENTAS = /^\s*(en|de)\s+(ventas|facturacion|lo que vendo)/;
const RELATIVO = /\b(duplic\w*|el doble|triplic\w*|el triple)\b/;
const PORCENTAJE_MAS =
  /(\d+(?:[.,]\d+)?)\s*(%|por ciento)\s*(mas|arriba)?|\b(subir|aumentar|crecer|mejorar)\w*\s+(un|el)?\s*(\d+(?:[.,]\d+)?)\s*(%|por ciento)/;

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

const PERIODOS: { periodo: Periodo; patron: RegExp }[] = [
  { periodo: 'dia', patron: /\b(por|al|cada|a)\s+dia\b|\bdiari\w*/ },
  { periodo: 'semana', patron: /\b(por|a la|cada|la)\s+semana\b|\bsemanal\w*/ },
  { periodo: 'mes', patron: /\b(por|al|cada|el|a)\s+mes\b|\bmensual\w*/ },
  { periodo: 'anio', patron: /\b(por|al|cada|el)\s+ano\b|\banual\w*/ },
];

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

const redondear = (valor: number) => Math.round(valor / 1_000) * 1_000;

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
  const avisos: string[] = [];
  const candidatos = montosDe(normal).filter((monto) => esPlataDeLaMeta(normal, monto));
  const conVerbo = candidatos.filter((monto) => tipoEn(clausulaAntes(normal, monto.inicio)));
  const elegido = conVerbo.find((monto) => monto.marcado) ?? conVerbo[0] ?? candidatos[0];
  const distintos = new Set(candidatos.map((monto) => monto.valor));
  const quedaHoy = negocio?.hayVentas && negocio.queda > 0 ? negocio.queda : undefined;

  if (!elegido) {
    const factor = factorRelativo(normal);
    if (factor) {
      const crecimiento =
        factor >= 2
          ? factor === 2
            ? 'el doble'
            : 'el triple'
          : `un ${Math.round((factor - 1) * 100)} % más`;
      if (quedaHoy) {
        return {
          meta: redondear(quedaHoy * factor),
          tipo: 'CRECER',
          explicacion: `Ganar ${crecimiento} de lo que te queda hoy (${formatoPesos(quedaHoy)} por mes).`,
          avisos,
        };
      }
      return {
        meta: null,
        tipo: 'CRECER',
        explicacion: `Querés ganar ${crecimiento}, pero todavía no tengo tus ventas para calcularlo.`,
        avisos: ['Escribí abajo cuánto querés ganar por mes, en pesos.'],
      };
    }
    const tipo = tipoEn(normal) ?? 'GANAR';
    return {
      meta: null,
      tipo,
      explicacion:
        tipo === 'CUBRIR_GASTOS'
          ? 'Querés cubrir tus gastos. Sami mide lo que te queda después de pagar la mercadería y los gastos.'
          : 'No encontré cuánto querés ganar.',
      avisos: ['Escribí abajo cuánto querés que te quede por mes, en pesos.'],
    };
  }

  const clausula = clausulaAntes(normal, elegido.inicio);
  const despues = normal.slice(elegido.fin);
  const tipo = EN_VENTAS.test(despues) ? 'VENDER' : (tipoEn(clausula) ?? tipoEn(normal) ?? 'GANAR');
  const periodo = periodoDe(despues, clausula);
  const mensual = periodo === 'mes' ? elegido.valor : redondear(elegido.valor * POR_MES[periodo]);
  const escrito =
    periodo === 'mes'
      ? `${formatoPesos(elegido.valor)} por mes`
      : `${formatoPesos(elegido.valor)} ${NOMBRE_PERIODO[periodo]} (unos ${formatoPesos(mensual)} por mes)`;
  if (distintos.size > 1) {
    const otros = [...distintos].filter((valor) => valor !== elegido.valor).map(formatoPesos);
    avisos.push(
      `También escribiste ${otros.join(' y ')}. Tomé ${formatoPesos(elegido.valor)} como meta.`,
    );
  }

  if (tipo === 'VENDER') {
    const margen =
      negocio?.hayVentas && negocio.vendiste > 0 ? negocio.queda / negocio.vendiste : 0;
    if (margen > 0) {
      const meta = redondear(mensual * margen);
      return {
        meta,
        tipo,
        explicacion: `Vender ${escrito}. Con lo que te queda hoy de cada venta (${Math.round(margen * 100)} %), eso son unos ${formatoPesos(meta)} de ganancia por mes: eso mide Sami.`,
        avisos,
      };
    }
    return {
      meta: null,
      tipo,
      explicacion: `Vender ${escrito}. Sami mide lo que te queda después de pagar mercadería y gastos, no lo que vendés.`,
      avisos: [...avisos, 'Escribí abajo cuánto querés que te quede por mes, en pesos.'],
    };
  }

  const explicacion =
    tipo === 'RETIRAR'
      ? `Retirar ${escrito}: el negocio tiene que dejarte al menos eso después de pagar mercadería y gastos.`
      : tipo === 'CUBRIR_GASTOS'
        ? `Juntar ${escrito} para tus gastos. Si ya los anotás como gastos en una sección, Sami ya los descuenta: la meta es lo que te queda aparte.`
        : `Ganar ${escrito}, después de pagar la mercadería y los gastos.`;
  if (quedaHoy && mensual <= quedaHoy) {
    avisos.push(`Hoy ya te quedan ${formatoPesos(quedaHoy)} por mes: esta meta ya la cumplís.`);
  }
  return { meta: mensual, tipo, explicacion, avisos };
}

/** Cuánto se aleja la meta de lo que el negocio hace hoy, y un primer escalón para empezar. */
export type AlcanceDeLaMeta = {
  nivel: 'imposible' | 'lejos';
  mensaje: string;
  /** Un primer escalón: lo que queda hoy más un 30 %; null si hoy no queda nada con qué calcularlo. */
  sugerida: number | null;
};

/** Más que esto veces lo que queda hoy ya es «lejos»: se puede, pero lleva varios domingos. */
const VECES_LEJOS = 2;
const PRIMER_ESCALON = 1.3;

/**
 * Si la meta (por mes) se puede alcanzar con lo que vende el negocio hoy (últimos 30 días). «Imposible» es pedir
 * más de lo que se vende: ni sin pagar la mercadería ni los gastos quedaría esa plata. Sin ventas no se opina.
 */
export function alcanceDeLaMeta(meta: number, negocio?: Negocio): AlcanceDeLaMeta | null {
  if (!negocio?.hayVentas || negocio.vendiste <= 0 || meta <= 0) return null;
  const sugerida = negocio.queda > 0 ? redondear(negocio.queda * PRIMER_ESCALON) : null;
  const escalon = sugerida !== null && sugerida < meta ? sugerida : null;
  const empezar =
    escalon === null
      ? ''
      : ` Te conviene empezar por ${formatoPesos(escalon)} y subirla cuando llegues.`;
  if (meta > negocio.vendiste) {
    return {
      nivel: 'imposible',
      mensaje: `Hoy vendés unos ${formatoPesos(negocio.vendiste)} por mes. Aunque no pagaras la mercadería ni los gastos, no te quedarían ${formatoPesos(meta)}.${empezar}`,
      sugerida: escalon,
    };
  }
  if (negocio.queda > 0 && meta > negocio.queda * VECES_LEJOS) {
    return {
      nivel: 'lejos',
      mensaje: `Es más del doble de lo que te queda hoy (${formatoPesos(negocio.queda)} por mes). Se puede, pero lleva varias semanas: Sami va por escalones.${empezar}`,
      sugerida: escalon,
    };
  }
  return null;
}
