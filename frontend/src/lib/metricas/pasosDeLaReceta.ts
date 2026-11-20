import {
  ETIQUETA_DE_AGRUPACION,
  ETIQUETA_DE_FUENTE,
  ETIQUETA_DE_PERIODO,
  agrupacionesDe,
  ajustarALaFuente,
  calculosDe,
  etiquetaDeCalculo,
  periodosDe,
} from '@/lib/metricas/metricasAMedida';
import type { NombreDeIcono } from '@/lib/sistema/iconos';
import type {
  AgrupacionDeMetrica,
  CalculoDeMetrica,
  DefinicionDeMetrica,
  FuenteDeMetrica,
  PeriodoDeMetrica,
} from '@/types/metricas';

// Los pasos de «Armala vos»: cada uno pregunta una parte de la receta y la ofrece como tarjetas con ícono. El último
// paso, el nombre, no tiene tarjetas y lo dibuja el componente.

export type OpcionDelPaso = { valor: string; etiqueta: string; icono: NombreDeIcono };

export type PasoDeLaReceta = {
  titulo: string;
  corto: string;
  color: 'petroleo' | 'naranja' | 'celeste' | 'coral';
  opciones: (definicion: DefinicionDeMetrica, fuentes: FuenteDeMetrica[]) => OpcionDelPaso[];
  elegida: (definicion: DefinicionDeMetrica) => string;
  elegir: (definicion: DefinicionDeMetrica, valor: string) => DefinicionDeMetrica;
};

const ICONO_DE_FUENTE: Record<FuenteDeMetrica, NombreDeIcono> = {
  VENTAS: 'wallet',
  GASTOS: 'orders',
  INVENTARIO: 'box',
};

const ICONO_DE_CALCULO: Record<CalculoDeMetrica, NombreDeIcono> = {
  SUMA: 'plus',
  PROMEDIO: 'igual',
  CONTEO: 'check',
  UNIDADES: 'box',
  GANANCIA: 'sube',
  COSTO: 'wallet',
};

const ICONO_DE_AGRUPACION: Record<AgrupacionDeMetrica, NombreDeIcono> = {
  NINGUNA: 'spark',
  PRODUCTO: 'box',
  CLIENTE: 'users',
  CATEGORIA: 'folder',
  DIA_DE_LA_SEMANA: 'chart',
};

const ICONO_DE_PERIODO: Record<PeriodoDeMetrica, NombreDeIcono> = {
  HOY: 'pin',
  SEMANA: 'clock',
  MES: 'note',
  TODO: 'grid',
};

const QUE_MIRAR: PasoDeLaReceta = {
  titulo: '¿Qué querés mirar?',
  corto: 'Qué mirar',
  color: 'petroleo',
  opciones: (_, fuentes) =>
    fuentes.map((fuente) => ({
      valor: fuente,
      etiqueta: ETIQUETA_DE_FUENTE[fuente],
      icono: ICONO_DE_FUENTE[fuente],
    })),
  elegida: (definicion) => definicion.fuente,
  elegir: (definicion, valor) => ajustarALaFuente(definicion, valor as FuenteDeMetrica),
};

const QUE_CUENTA: PasoDeLaReceta = {
  titulo: '¿Qué cuenta querés hacer?',
  corto: 'Qué cuenta',
  color: 'naranja',
  opciones: ({ fuente }) =>
    calculosDe(fuente).map((calculo) => ({
      valor: calculo,
      etiqueta: etiquetaDeCalculo(fuente, calculo),
      icono: ICONO_DE_CALCULO[calculo],
    })),
  elegida: (definicion) => definicion.calculo,
  elegir: (definicion, valor) => ({ ...definicion, calculo: valor as CalculoDeMetrica }),
};

const COMO_REPARTIR: PasoDeLaReceta = {
  titulo: '¿Cómo la querés repartir?',
  corto: 'Cómo repartir',
  color: 'celeste',
  opciones: ({ fuente }) =>
    agrupacionesDe(fuente).map((agrupar) => ({
      valor: agrupar,
      etiqueta: ETIQUETA_DE_AGRUPACION[agrupar],
      icono: ICONO_DE_AGRUPACION[agrupar],
    })),
  elegida: (definicion) => definicion.agrupar,
  elegir: (definicion, valor) => ({ ...definicion, agrupar: valor as AgrupacionDeMetrica }),
};

const DE_CUANDO: PasoDeLaReceta = {
  titulo: '¿De cuándo?',
  corto: 'De cuándo',
  color: 'coral',
  opciones: ({ fuente }) =>
    periodosDe(fuente).map((periodo) => ({
      valor: periodo,
      etiqueta: ETIQUETA_DE_PERIODO[periodo],
      icono: ICONO_DE_PERIODO[periodo],
    })),
  elegida: (definicion) => definicion.periodo,
  elegir: (definicion, valor) => ({ ...definicion, periodo: valor as PeriodoDeMetrica }),
};

export const PASOS_CON_TARJETAS = [QUE_MIRAR, QUE_CUENTA, COMO_REPARTIR, DE_CUANDO];

/** El paso con tarjetas de ese lugar (contando desde 0); fuera de rango, el primero. */
export const pasoConTarjetas = (lugar: number) => PASOS_CON_TARJETAS[lugar] ?? QUE_MIRAR;

/** El quinto paso, el nombre, va después de las cuatro preguntas con tarjetas. */
export const PASO_DEL_NOMBRE = PASOS_CON_TARJETAS.length;

export const CANTIDAD_DE_PASOS = PASOS_CON_TARJETAS.length + 1;

/** La etiqueta de lo elegido en un paso, para el resumen de arriba («Ventas», «Por producto»). */
export const etiquetaElegida = (
  paso: PasoDeLaReceta,
  definicion: DefinicionDeMetrica,
  fuentes: FuenteDeMetrica[],
) =>
  paso.opciones(definicion, fuentes).find((opcion) => opcion.valor === paso.elegida(definicion))
    ?.etiqueta ?? '';

/** Las cinco solapas de «Armala vos», con el color de cada una; el nombre va en petróleo. */
export const SOLAPAS_DE_LOS_PASOS: { corto: string; color: PasoDeLaReceta['color'] }[] = [
  ...PASOS_CON_TARJETAS.map(({ corto, color }) => ({ corto, color })),
  { corto: 'Nombre', color: 'petroleo' },
];

/** Qué pasos quedan contestados al elegir en uno: si cambia lo que se mira, lo demás hay que volver a elegirlo. */
export const hechosDespuesDeElegir = (hechos: number[], paso: number, cambioLaFuente: boolean) => [
  ...new Set(cambioLaFuente ? [paso] : [...hechos, paso]),
];

/** Las preguntas con tarjetas sin contestar, con su nombre corto («Qué cuenta»). */
export const pasosSinContestar = (hechos: number[]) =>
  PASOS_CON_TARJETAS.filter((_, lugar) => !hechos.includes(lugar)).map(({ corto }) => corto);

/** Los pasos van en orden: se puede ir a uno si ya se contestaron todos los de antes (y volver a cualquiera hecho). */
export const sePuedeIrAlPaso = (hechos: number[], lugar: number) =>
  Array.from({ length: lugar }, (_, antes) => antes).every((antes) => hechos.includes(antes));
