import { formatoPesos } from '@/lib/objetivos';
import type {
  DecisionesObjetivo,
  DecisionObjetivo,
  MiniObjetivo,
  TipoDecision,
  TipoMiniObjetivo,
} from '@/types/objetivos';

// Cómo se cuentan en palabras los mini-objetivos, las decisiones y las semanas de un objetivo que se autogestiona.

const numero = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 1 });

/** El valor de un mini-objetivo en su unidad: pesos, unidades por semana o porcentaje. */
export function valorDeMini(tipo: TipoMiniObjetivo, valor: number): string {
  switch (tipo) {
    case 'SUBIR_PRECIO':
      return formatoPesos(valor);
    case 'VENTAS_SEMANALES':
      return `${formatoPesos(valor)} por semana`;
    case 'RECORTAR_GASTO':
      return `${formatoPesos(valor)} en el mes`;
    case 'MEJORAR_MARGEN':
      return `${numero.format(valor)} %`;
    case 'MOVER_PARADO':
    case 'PONER_A_LA_VISTA':
      return `${numero.format(valor)} por semana`;
  }
}

/** Qué se mide en cada tipo, para leer «hoy / meta» sin ambigüedad. */
export const QUE_SE_MIDE: Record<TipoMiniObjetivo, string> = {
  SUBIR_PRECIO: 'Precio',
  VENTAS_SEMANALES: 'Ventas',
  RECORTAR_GASTO: 'Gastos',
  MEJORAR_MARGEN: 'Margen',
  MOVER_PARADO: 'Unidades vendidas',
  PONER_A_LA_VISTA: 'Unidades vendidas',
};

export const ESTADO_DE_MINI: Record<MiniObjetivo['estado'], string> = {
  EN_CURSO: 'En curso',
  LOGRADO: 'Lo lograste',
  ABANDONADO: 'Lo quitaste',
  REEMPLAZADO: 'Lo cambiamos',
};

export const DECISION: Record<TipoDecision, { simbolo: string; texto: string; clase: string }> = {
  AVISO_APLICADO: { simbolo: '✓', texto: 'Aplicaste', clase: 'aplicada' },
  AVISO_DESCARTADO: { simbolo: '✕', texto: 'Descartaste', clase: 'descartada' },
  PASO_ACEPTADO: { simbolo: '●', texto: 'Dijiste «lo hago»', clase: 'aceptada' },
  PASO_IGNORADO: { simbolo: '○', texto: 'Dejaste para después', clase: 'ignorada' },
};

const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

/** «9 nov» a partir de una fecha YYYY-MM-DD. */
export function diaCorto(fecha: string): string {
  const [, mes, dia] = fecha.slice(0, 10).split('-').map(Number);
  return `${dia ?? ''} ${MESES[(mes ?? 1) - 1] ?? ''}`.trim();
}

/** «Semana del 9 nov». */
export const semanaDel = (lunes: string) => `Semana del ${diaCorto(lunes)}`;

/** Qué pasó con lo que decidió la persona, si ya se puede saber. */
export function efectoDe(decision: DecisionObjetivo): string | null {
  if (decision.efecto === 'MIDIENDO') return 'Sami todavía lo está midiendo.';
  if (decision.efecto !== 'MEDIDO' || decision.deltaIngresoSemanal == null) return null;
  const delta = decision.deltaIngresoSemanal;
  if (delta > 0) return `Después vendiste ${formatoPesos(delta)} más por semana.`;
  if (delta < 0) return `Después vendiste ${formatoPesos(-delta)} menos por semana.`;
  return 'Las ventas no cambiaron.';
}

/** Las decisiones de todas las semanas, en una sola lista, de la más vieja a la más nueva. */
export function todasLasDecisiones(decisiones: DecisionesObjetivo | undefined): DecisionObjetivo[] {
  return (decisiones?.semanas ?? [])
    .flatMap((semana) => semana.decisiones)
    .sort((a, b) => a.tomadaEn.localeCompare(b.tomadaEn));
}

const CLAVE_RESUMEN_VISTO = 'ensambla.objetivos.resumenVisto';

function vistos(): Record<string, string> {
  try {
    const guardado: unknown = JSON.parse(localStorage.getItem(CLAVE_RESUMEN_VISTO) ?? '{}');
    return guardado && typeof guardado === 'object' ? (guardado as Record<string, string>) : {};
  } catch {
    return {};
  }
}

/** Si la persona todavía no vio el último cierre del domingo de ese objetivo. */
export function resumenSinVer(objetivoId: string, ultimoCierre: string | null): boolean {
  return !!ultimoCierre && vistos()[objetivoId] !== ultimoCierre;
}

export function marcarResumenVisto(objetivoId: string, ultimoCierre: string) {
  localStorage.setItem(
    CLAVE_RESUMEN_VISTO,
    JSON.stringify({ ...vistos(), [objetivoId]: ultimoCierre }),
  );
}

/** El lunes de la semana de ese día (YYYY-MM-DD): las semanas de los objetivos van de lunes a domingo. */
export function lunesDe(dia: string): string {
  const fecha = new Date(`${dia}T12:00:00Z`);
  const desdeElLunes = (fecha.getUTCDay() + 6) % 7;
  return new Date(fecha.getTime() - desdeElLunes * 86_400_000).toISOString().slice(0, 10);
}

/** Lo último que la persona decidió sobre ese mini-objetivo en la semana, si fue «lo hago» o «ahora no». */
export function decisionDeLaSemana(
  decisiones: DecisionObjetivo[],
  miniId: string,
  semana: string,
): 'ACEPTADO' | 'IGNORADO' | undefined {
  const ultima = decisiones
    .filter(
      (decision) =>
        decision.miniObjetivoId === miniId &&
        decision.semanaInicio === semana &&
        (decision.tipo === 'PASO_ACEPTADO' || decision.tipo === 'PASO_IGNORADO'),
    )
    .at(-1);
  if (!ultima) return undefined;
  return ultima.tipo === 'PASO_ACEPTADO' ? 'ACEPTADO' : 'IGNORADO';
}

/** El domingo en que cierra la semana de ese lunes. */
export function domingoDe(lunes: string): string {
  return new Date(new Date(`${lunes}T12:00:00Z`).getTime() + 6 * 86_400_000)
    .toISOString()
    .slice(0, 10);
}

/** El número y su unidad por separado, para mostrar el número grande y la unidad chica. */
export function partesDeMini(
  tipo: TipoMiniObjetivo,
  valor: number,
): { numero: string; unidad: string } {
  const texto = valorDeMini(tipo, valor);
  if (tipo === 'MEJORAR_MARGEN') return { numero: texto, unidad: 'de margen' };
  const [numero = texto, ...resto] = texto.split(' ');
  return { numero, unidad: resto.join(' ') };
}

/** Cuánto le falta al mini-objetivo para su meta, en su unidad; nada si ya llegó. */
export function faltaParaLaMeta(mini: MiniObjetivo): string | null {
  const falta =
    mini.sentido === 'BAJA' ? mini.valorActual - mini.meta : mini.meta - mini.valorActual;
  if (falta <= 0) return null;
  return valorDeMini(mini.tipo, falta);
}
