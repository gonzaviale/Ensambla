import { diasDeCalendarioEntre } from '@/lib/diaDelNegocio';
import type { AccionDeAviso, Aviso, TipoDeAviso } from '@/lib/sistema/alertas';
import type { NombreDeIcono } from '@/lib/sistema/iconos';
import { comportamientoDeAviso, type DatoDelAviso } from './comportamientoDeAviso';

export type { DatoDelAviso };

export const ICONO_DE_AVISO: Record<TipoDeAviso, NombreDeIcono> = {
  stock: 'box',
  precio_zona: 'ubicacion',
  estancado: 'clock',
  muy_vendido: 'spark',
  combo: 'orders',
  consumo_zona: 'ubicacion',
  revisar_precio: 'search',
};

/** El nombre corto de cada tipo: lo primero que se lee para saber de qué se trata. */
export const NOMBRE_DE_TIPO: Record<TipoDeAviso, string> = {
  stock: 'Stock',
  precio_zona: 'Precio vs. zona',
  estancado: 'Sin movimiento',
  muy_vendido: 'Se vende mucho',
  combo: 'Combo',
  consumo_zona: 'Lo que pide la zona',
  revisar_precio: 'Revisar el precio',
};

export const NIVEL_DE_AVISO = { 3: 'Urgente', 2: 'Para hoy', 1: 'Para mirar' } as const;

/** Lo que dice el botón: la acción, tal cual («Subir a $ 3.820», «Cargar entrada»). */
export function textoDeAccion(accion: AccionDeAviso) {
  return accion.texto;
}

/** Los números del aviso listos para mostrar. Vacío cuando el aviso no los trae (los calculados en el navegador). */
export function datosDelAviso(aviso: Aviso): DatoDelAviso[] {
  return comportamientoDeAviso(aviso.tipo).datos(aviso);
}

/** Qué se propone hacer, en una línea y con números cuando se conocen. */
export function propuestaDelAviso(aviso: Aviso): string | undefined {
  if (!aviso.acciones[0]) return undefined;
  return comportamientoDeAviso(aviso.tipo).propuesta(aviso);
}

/** Si hay algo más que contar que lo que ya dice la fila: es lo que decide si se ofrece «Ver por qué». */
export function tieneDetalle(aviso: Aviso) {
  const [, ...secundarias] = aviso.acciones;
  return Boolean(
    restoDelAviso(aviso.detalle) ||
    datosDelAviso(aviso).length ||
    razonDelAviso(aviso) ||
    secundarias.length,
  );
}

/** «hoy», «ayer» o «hace N días» según los días de calendario argentino, no las horas transcurridas. */
export function haceCuanto(creadoEn: string | undefined, ahora = new Date()): string | undefined {
  if (!creadoEn) return undefined;
  const fecha = new Date(creadoEn);
  if (Number.isNaN(fecha.getTime())) return undefined;
  return antiguedadEnPalabras(diasDeCalendarioEntre(fecha, ahora));
}

function antiguedadEnPalabras(dias: number) {
  if (dias <= 0) return 'hoy';
  if (dias === 1) return 'ayer';
  return `hace ${dias} días`;
}

/** «Hoy», «Ayer» o «Hace N días»: lo que lleva el reloj arriba de cada aviso. */
export function cuandoSeDetecto(creadoEn: string | undefined, ahora = new Date()) {
  const texto = haceCuanto(creadoEn, ahora);
  return texto ? texto.charAt(0).toUpperCase() + texto.slice(1) : undefined;
}

/** Lleva a un tono más serio los textos que llegan del motor de avisos («Para no quedarte corto, pedí 5.»). */
export function suavizarTexto(texto: string) {
  return texto.replace(
    /Para no quedarte corto, pedí ([^.]+)\./g,
    'Te conviene pedir $1 para no quedarte sin stock.',
  );
}

/**
 * Lo que sigue a la primera idea del aviso, para «Ver por qué»: no repite el resumen de la tarjeta ni lo que ya dice
 * la línea de propuesta («Te conviene pedir 5»). Vacío si no queda nada más para decir.
 */
export function restoDelAviso(detalle: string) {
  const [primera = '', ...resto] = suavizarTexto(detalle).split(/(?<=\.)\s+/);
  const sinPropuesta = resto.filter((frase) => !frase.startsWith('Te conviene pedir'));
  return resumenDelAviso(detalle) === primera
    ? sinPropuesta.join(' ')
    : [primera, ...sinPropuesta].join(' ');
}

const RAZON_POR_TIPO: Partial<Record<TipoDeAviso, RegExp>> = {
  muy_vendido: /^En tu zona,/,
  estancado: /^En tu zona,/,
  precio_zona: /^En tu zona,/,
  combo: /^Por separado/,
};

/**
 * La razón principal del aviso, en una línea, para «Por qué de este aviso». El resto de los motivos no se muestran, y
 * la aclaración de que los datos de la zona son de varios comercios va una sola vez al pie de la lista.
 */
export function razonDelAviso(aviso: Aviso): string | undefined {
  const patron = RAZON_POR_TIPO[aviso.tipo];
  const motivo = (patron && aviso.porque.find((texto) => patron.test(texto))) ?? aviso.porque[0];
  return motivo && suavizarTexto(motivo).replace(/\s*Es un dato agregado[^.]*\./, '');
}

export type GrupoDeAvisos = { clave: string; avisos: Aviso[] };

/** Junta los avisos del mismo tipo y nivel, de cualquier negocio, y pone primero los más urgentes. */
export function agruparAvisos(avisos: Aviso[]): GrupoDeAvisos[] {
  const grupos = new Map<string, Aviso[]>();
  for (const aviso of avisos) {
    const clave = `${aviso.tipo}:${aviso.prioridad}`;
    grupos.set(clave, [...(grupos.get(clave) ?? []), aviso]);
  }
  return [...grupos]
    .map(([clave, juntos]) => ({ clave, avisos: juntos }))
    .sort((a, b) => prioridadDelGrupo(b) - prioridadDelGrupo(a));
}

function prioridadDelGrupo(grupo: GrupoDeAvisos) {
  return grupo.avisos[0]?.prioridad ?? 0;
}

/** El título de varios avisos juntos: el número va adelante («5 productos bajo el mínimo»). */
export function tituloDeGrupo(tipo: TipoDeAviso, prioridad: number, cantidad: number) {
  return comportamientoDeAviso(tipo).titulo(cantidad, prioridad);
}

/** La primera idea del aviso, en una línea o línea y media: el resto queda en «Ver por qué». */
export function resumenDelAviso(detalle: string) {
  const [primera = ''] = suavizarTexto(detalle).split(/(?<=\.)\s+/);
  if (primera.length <= 60) return primera;
  const corte = primera.search(/[;,]/);
  return corte >= 15 ? `${primera.slice(0, corte)}.` : primera;
}
