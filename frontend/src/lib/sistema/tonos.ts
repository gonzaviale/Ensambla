export type Tono = 'naranja' | 'coral' | 'celeste' | 'petroleo';

export const TONOS = ['celeste', 'naranja', 'coral', 'petroleo'] as const satisfies readonly Tono[];

const TONO_POR_CONCEPTO = {
  ventas: 'petroleo',
  ganancia: 'celeste',
  costoDeMercaderia: 'coral',
  otrosGastos: 'naranja',
  gastos: 'naranja',
  fiado: 'coral',
  cobrado: 'celeste',
  efectivo: 'celeste',
  transferencia: 'naranja',
  tarjeta: 'coral',
  pestanaResumenDelDia: 'petroleo',
  pestanaCobros: 'celeste',
  pestanaCajaAbierta: 'naranja',
  pestanaCierresGuardados: 'coral',
  venderEscanear: 'celeste',
  venderBuscar: 'naranja',
  venderFavoritos: 'coral',
  accesoNuevaVenta: 'naranja',
  accesoEscanearStock: 'celeste',
  botonDeAccesosRapidos: 'petroleo',
  cruzDeCerrar: 'coral',
} as const satisfies Record<string, Tono>;

export type ConceptoDeColor = keyof typeof TONO_POR_CONCEPTO;

export const CONCEPTOS_DE_COLOR = Object.keys(TONO_POR_CONCEPTO) as ConceptoDeColor[];

export function tonoDe(concepto: ConceptoDeColor): Tono {
  return TONO_POR_CONCEPTO[concepto];
}

export function claseDeSeleccion(seleccionado: boolean) {
  return seleccionado ? 'solido' : 'solido-contorno';
}

export function tonoPorPosicion(indice: number): Tono {
  return TONOS[Math.abs(indice) % TONOS.length] ?? TONOS[0];
}
