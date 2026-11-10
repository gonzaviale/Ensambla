// «Dónde abrir»: ayuda a elegir la zona de un próximo local. Todavía es una demostración: las zonas y sus
// números son de ejemplo (barrios de Buenos Aires con valores inventados pero verosímiles). El cálculo, en
// cambio, es el real: un puntaje con pesos por rubro que la persona puede ajustar con filtros, y que se puede
// mostrar paso a paso.

export type Rubro = 'almacen' | 'papeleria' | 'kiosco';
export type Publico = 'vecinos' | 'estudiantes' | 'oficinas';
/** Cuánto le importa a la persona un factor: 0 = nada, 1 = importa, 2 = importa mucho. */
export type Importancia = 0 | 1 | 2;

export type FactorClave =
  | 'competencia'
  | 'movimiento'
  | 'vecinos'
  | 'escuelas'
  | 'oficinas'
  | 'transporte'
  | 'parques'
  | 'salud'
  | 'complementarios'
  | 'poder'
  | 'alquiler';

/** Valor de «sin límite» en los controles de distancia, competencia y alquiler. */
export const SIN_LIMITE_KM = 6;
export const SIN_LIMITE_COMPETENCIA = 15;
export const SIN_LIMITE_ALQUILER = 100;

export type Limites = {
  /** Hasta dónde se busca desde el local actual, en km. */
  distanciaMaxKm: number;
  /** Máximo de comercios del mismo rubro a menos de 500 m. */
  competenciaMax: number;
  /** Mínimos de lo que tiene que haber a menos de 500 m (0 = no se exige). */
  escuelasMin: number;
  parquesMin: number;
  transporteMin: number;
  /** Nivel de alquiler máximo, de 0 a 100. */
  alquilerMax: number;
};

export type Filtros = {
  rubro: Rubro;
  publico: Publico;
  importancia: Record<FactorClave, Importancia>;
  limites: Limites;
};

const CLAVES_DE_FACTOR: FactorClave[] = [
  'competencia',
  'movimiento',
  'vecinos',
  'escuelas',
  'oficinas',
  'transporte',
  'parques',
  'salud',
  'complementarios',
  'poder',
  'alquiler',
];

const IMPORTANCIA_NORMAL = Object.fromEntries(
  CLAVES_DE_FACTOR.map((clave) => [clave, 1]),
) as Record<FactorClave, Importancia>;

export const FILTROS_INICIALES: Filtros = {
  rubro: 'almacen',
  publico: 'vecinos',
  importancia: IMPORTANCIA_NORMAL,
  limites: {
    distanciaMaxKm: 4,
    competenciaMax: SIN_LIMITE_COMPETENCIA,
    escuelasMin: 0,
    parquesMin: 0,
    transporteMin: 0,
    alquilerMax: SIN_LIMITE_ALQUILER,
  },
};

/** El local que la persona ya tiene (de ejemplo). */
export const LOCAL_ACTUAL = {
  nombre: 'Almacén de barrio',
  barrio: 'Villa Crespo',
  latitud: -34.5996,
  longitud: -58.4403,
} as const;

/** Por debajo de esta distancia, el local nuevo le quitaría clientes al actual. */
export const DISTANCIA_MINIMA_KM = 1;
/** Radio con el que se cuentan comercios, escuelas y paradas alrededor de cada zona. */
export const RADIO_DE_ANALISIS_M = 500;

export const NOMBRE_DE_RUBRO: Record<Rubro, string> = {
  almacen: 'almacén',
  papeleria: 'papelería',
  kiosco: 'kiosco',
};
export const UN_RUBRO: Record<Rubro, string> = {
  almacen: 'un almacén',
  papeleria: 'una papelería',
  kiosco: 'un kiosco',
};
export const PLURAL_DE_RUBRO: Record<Rubro, string> = {
  almacen: 'almacenes',
  papeleria: 'papelerías',
  kiosco: 'kioscos',
};

export const FACTORES: { clave: FactorClave; etiqueta: string; ayuda: string }[] = [
  {
    clave: 'competencia',
    etiqueta: 'Poca competencia',
    ayuda: 'Comercios similares al tuyo a menos de 500 m',
  },
  { clave: 'movimiento', etiqueta: 'Gente pasando', ayuda: 'Movimiento de personas en la calle' },
  { clave: 'vecinos', etiqueta: 'Vecinos cerca', ayuda: 'Personas que viven en la zona' },
  { clave: 'escuelas', etiqueta: 'Escuelas cerca', ayuda: 'Escuelas a menos de 500 m' },
  { clave: 'parques', etiqueta: 'Parques cerca', ayuda: 'Parques y plazas a menos de 500 m' },
  { clave: 'oficinas', etiqueta: 'Oficinas cerca', ayuda: 'Oficinas y bancos a menos de 500 m' },
  { clave: 'transporte', etiqueta: 'Transporte', ayuda: 'Paradas y estaciones a menos de 500 m' },
  {
    clave: 'salud',
    etiqueta: 'Salud cerca',
    ayuda: 'Hospitales y centros de salud a menos de 500 m',
  },
  {
    clave: 'complementarios',
    etiqueta: 'Comercios que atraen gente',
    ayuda: 'Farmacias, bancos y supermercados a menos de 500 m',
  },
  { clave: 'poder', etiqueta: 'Poder de compra', ayuda: 'Nivel socioeconómico de la zona' },
  {
    clave: 'alquiler',
    etiqueta: 'Alquiler accesible',
    ayuda: 'Qué tan barato es alquilar, comparado con las otras zonas',
  },
];

export type Jurisdiccion = 'caba' | 'la-matanza';

export const NOMBRE_DE_JURISDICCION: Record<Jurisdiccion, string> = {
  caba: 'Ciudad de Buenos Aires',
  'la-matanza': 'La Matanza',
};
export const SIGLA_DE_JURISDICCION: Record<Jurisdiccion, string> = {
  caba: 'CABA',
  'la-matanza': 'La Matanza',
};

export type DatosDeZona = {
  id: string;
  jurisdiccion: Jurisdiccion;
  nombre: string;
  latitud: number;
  longitud: number;
  /** Personas por km². */
  densidad: number;
  /** Movimiento peatonal, de 0 a 100. */
  movimiento: number;
  escuelas: number;
  oficinas: number;
  transporte: number;
  parques: number;
  salud: number;
  /** Farmacias, bancos y supermercados. */
  complementarios: number;
  /** Nivel socioeconómico, de 1 a 10. */
  poder: number;
  /** Nivel de alquiler, de 0 a 100 (100 = la más cara de las zonas comparadas). */
  alquiler: number;
  /** Comercios del mismo rubro a menos de 500 m. */
  competencia: Record<Rubro, number>;
};

// Zonas de ejemplo. Las posiciones son aproximadas; los números, inventados para la demostración.
export const ZONAS: DatosDeZona[] = [
  {
    id: 'almagro',
    jurisdiccion: 'caba',
    nombre: 'Almagro',
    latitud: -34.6087,
    longitud: -58.4195,
    densidad: 22400,
    movimiento: 80,
    escuelas: 6,
    oficinas: 7,
    transporte: 9,
    parques: 1,
    salud: 3,
    complementarios: 8,
    poder: 6,
    alquiler: 72,
    competencia: { almacen: 11, papeleria: 6, kiosco: 13 },
  },
  {
    id: 'boedo',
    jurisdiccion: 'caba',
    nombre: 'Boedo',
    latitud: -34.629,
    longitud: -58.419,
    densidad: 21200,
    movimiento: 62,
    escuelas: 5,
    oficinas: 2,
    transporte: 5,
    parques: 2,
    salud: 1,
    complementarios: 5,
    poder: 5,
    alquiler: 44,
    competencia: { almacen: 5, papeleria: 2, kiosco: 6 },
  },
  {
    id: 'caballito-norte',
    jurisdiccion: 'caba',
    nombre: 'Caballito Norte',
    latitud: -34.612,
    longitud: -58.435,
    densidad: 23400,
    movimiento: 72,
    escuelas: 9,
    oficinas: 4,
    transporte: 7,
    parques: 3,
    salud: 2,
    complementarios: 9,
    poder: 7,
    alquiler: 74,
    competencia: { almacen: 9, papeleria: 4, kiosco: 10 },
  },
  {
    id: 'caballito-sur',
    jurisdiccion: 'caba',
    nombre: 'Caballito Sur',
    latitud: -34.623,
    longitud: -58.443,
    densidad: 20800,
    movimiento: 56,
    escuelas: 6,
    oficinas: 2,
    transporte: 4,
    parques: 2,
    salud: 2,
    complementarios: 5,
    poder: 7,
    alquiler: 60,
    competencia: { almacen: 5, papeleria: 3, kiosco: 5 },
  },
  {
    id: 'chacarita',
    jurisdiccion: 'caba',
    nombre: 'Chacarita',
    latitud: -34.588,
    longitud: -58.454,
    densidad: 16200,
    movimiento: 64,
    escuelas: 3,
    oficinas: 7,
    transporte: 7,
    parques: 1,
    salud: 1,
    complementarios: 4,
    poder: 5,
    alquiler: 52,
    competencia: { almacen: 4, papeleria: 1, kiosco: 5 },
  },
  {
    id: 'colegiales',
    jurisdiccion: 'caba',
    nombre: 'Colegiales',
    latitud: -34.574,
    longitud: -58.449,
    densidad: 14800,
    movimiento: 54,
    escuelas: 5,
    oficinas: 5,
    transporte: 3,
    parques: 2,
    salud: 1,
    complementarios: 6,
    poder: 9,
    alquiler: 78,
    competencia: { almacen: 3, papeleria: 3, kiosco: 4 },
  },
  {
    id: 'villa-ortuzar',
    jurisdiccion: 'caba',
    nombre: 'Villa Ortúzar',
    latitud: -34.58,
    longitud: -58.468,
    densidad: 12400,
    movimiento: 42,
    escuelas: 3,
    oficinas: 2,
    transporte: 3,
    parques: 3,
    salud: 0,
    complementarios: 2,
    poder: 6,
    alquiler: 42,
    competencia: { almacen: 2, papeleria: 1, kiosco: 3 },
  },
  {
    id: 'paternal',
    jurisdiccion: 'caba',
    nombre: 'La Paternal',
    latitud: -34.599,
    longitud: -58.468,
    densidad: 17200,
    movimiento: 50,
    escuelas: 4,
    oficinas: 1,
    transporte: 5,
    parques: 1,
    salud: 1,
    complementarios: 3,
    poder: 4,
    alquiler: 36,
    competencia: { almacen: 3, papeleria: 2, kiosco: 4 },
  },
  {
    id: 'villa-crespo-sur',
    jurisdiccion: 'caba',
    nombre: 'Villa Crespo Sur',
    latitud: -34.605,
    longitud: -58.438,
    densidad: 21100,
    movimiento: 70,
    escuelas: 5,
    oficinas: 3,
    transporte: 5,
    parques: 1,
    salud: 2,
    complementarios: 7,
    poder: 6,
    alquiler: 62,
    competencia: { almacen: 8, papeleria: 3, kiosco: 9 },
  },
  {
    id: 'villa-del-parque',
    jurisdiccion: 'caba',
    nombre: 'Villa del Parque',
    latitud: -34.604,
    longitud: -58.493,
    densidad: 15200,
    movimiento: 49,
    escuelas: 5,
    oficinas: 1,
    transporte: 3,
    parques: 4,
    salud: 1,
    complementarios: 3,
    poder: 6,
    alquiler: 48,
    competencia: { almacen: 5, papeleria: 2, kiosco: 4 },
  },
  {
    id: 'flores-norte',
    jurisdiccion: 'caba',
    nombre: 'Flores Norte',
    latitud: -34.627,
    longitud: -58.46,
    densidad: 24600,
    movimiento: 63,
    escuelas: 6,
    oficinas: 1,
    transporte: 6,
    parques: 2,
    salud: 3,
    complementarios: 6,
    poder: 4,
    alquiler: 32,
    competencia: { almacen: 9, papeleria: 3, kiosco: 8 },
  },
  {
    id: 'ramos-mejia',
    jurisdiccion: 'la-matanza',
    nombre: 'Ramos Mejía',
    latitud: -34.6406,
    longitud: -58.5647,
    densidad: 9800,
    movimiento: 71,
    escuelas: 6,
    oficinas: 4,
    transporte: 7,
    parques: 2,
    salud: 2,
    complementarios: 7,
    poder: 6,
    alquiler: 40,
    competencia: { almacen: 8, papeleria: 3, kiosco: 9 },
  },
  {
    id: 'san-justo',
    jurisdiccion: 'la-matanza',
    nombre: 'San Justo',
    latitud: -34.6815,
    longitud: -58.5583,
    densidad: 8200,
    movimiento: 68,
    escuelas: 5,
    oficinas: 5,
    transporte: 5,
    parques: 1,
    salud: 3,
    complementarios: 6,
    poder: 4,
    alquiler: 28,
    competencia: { almacen: 7, papeleria: 2, kiosco: 8 },
  },
];

/* Presets: formas rápidas de decir qué importa. */

export type Preset = {
  id: string;
  nombre: string;
  importancia: Partial<Record<FactorClave, Importancia>>;
};

export const PRESETS: Preset[] = [
  { id: 'equilibrado', nombre: 'Equilibrado', importancia: {} },
  { id: 'alquiler', nombre: 'Alquiler bajo', importancia: { alquiler: 2, poder: 0 } },
  {
    id: 'movimiento',
    nombre: 'Mucho movimiento',
    importancia: { movimiento: 2, transporte: 2, vecinos: 0 },
  },
  {
    id: 'familias',
    nombre: 'Familias',
    importancia: { escuelas: 2, parques: 2, vecinos: 2, oficinas: 0 },
  },
];

export function conPreset(filtros: Filtros, preset: Preset): Filtros {
  return { ...filtros, importancia: { ...IMPORTANCIA_NORMAL, ...preset.importancia } };
}

/** El preset que coincide con la importancia actual, si hay alguno. */
export function presetActivo(filtros: Filtros): string | null {
  const coincide = (preset: Preset) =>
    CLAVES_DE_FACTOR.every(
      (clave) => filtros.importancia[clave] === (preset.importancia[clave] ?? 1),
    );
  return PRESETS.find(coincide)?.id ?? null;
}

/* Cálculo */

export type Pesos = Record<FactorClave, number>;

// Cuánto pesa cada factor según el rubro (antes de ajustar por lo que la persona pidió).
const PESOS_BASE: Record<Rubro, Pesos> = {
  almacen: {
    competencia: 0.2,
    movimiento: 0.1,
    vecinos: 0.22,
    escuelas: 0.05,
    oficinas: 0.03,
    transporte: 0.05,
    parques: 0.03,
    salud: 0.02,
    complementarios: 0.07,
    poder: 0.09,
    alquiler: 0.14,
  },
  papeleria: {
    competencia: 0.2,
    movimiento: 0.11,
    vecinos: 0.05,
    escuelas: 0.24,
    oficinas: 0.13,
    transporte: 0.05,
    parques: 0.02,
    salud: 0.01,
    complementarios: 0.07,
    poder: 0.05,
    alquiler: 0.07,
  },
  kiosco: {
    competencia: 0.13,
    movimiento: 0.24,
    vecinos: 0.03,
    escuelas: 0.11,
    oficinas: 0.11,
    transporte: 0.18,
    parques: 0.03,
    salud: 0.02,
    complementarios: 0.05,
    poder: 0.03,
    alquiler: 0.07,
  },
};

const MULTIPLICADOR_DE_IMPORTANCIA: Record<Importancia, number> = { 0: 0, 1: 1, 2: 2.2 };

/** Los pesos finales: los del rubro, ajustados por el público y por la importancia elegida, y vueltos a sumar 1. */
export function pesosPara(filtros: Filtros): Pesos {
  const pesos = { ...PESOS_BASE[filtros.rubro] };
  if (filtros.publico === 'vecinos') pesos.vecinos *= 1.6;
  if (filtros.publico === 'estudiantes') {
    pesos.escuelas *= 1.8;
    pesos.parques *= 1.2;
  }
  if (filtros.publico === 'oficinas') {
    pesos.oficinas *= 1.8;
    pesos.transporte *= 1.2;
  }
  for (const clave of CLAVES_DE_FACTOR) {
    pesos[clave] *= MULTIPLICADOR_DE_IMPORTANCIA[filtros.importancia[clave]];
  }
  const total = CLAVES_DE_FACTOR.reduce((suma, clave) => suma + pesos[clave], 0);
  // Si la persona dijo que nada le importa, se mira todo por igual en vez de dividir por cero.
  if (total === 0) {
    return Object.fromEntries(
      CLAVES_DE_FACTOR.map((clave) => [clave, 1 / CLAVES_DE_FACTOR.length]),
    ) as Pesos;
  }
  return Object.fromEntries(
    CLAVES_DE_FACTOR.map((clave) => [clave, pesos[clave] / total]),
  ) as Pesos;
}

const RADIO_TERRESTRE_KM = 6371;
const aRadianes = (grados: number) => (grados * Math.PI) / 180;

export function distanciaEnKm(
  a: { latitud: number; longitud: number },
  b: { latitud: number; longitud: number },
): number {
  const dLat = aRadianes(b.latitud - a.latitud);
  const dLng = aRadianes(b.longitud - a.longitud);
  const h =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(aRadianes(a.latitud)) * Math.cos(aRadianes(b.latitud)) * Math.sin(dLng / 2) ** 2;
  return 2 * RADIO_TERRESTRE_KM * Math.asin(Math.sqrt(h));
}

const valorCrudo = (zona: DatosDeZona, clave: FactorClave, rubro: Rubro): number => {
  switch (clave) {
    case 'competencia':
      return zona.competencia[rubro];
    case 'vecinos':
      return zona.densidad;
    default:
      return zona[clave];
  }
};

// En estos dos, menos es mejor.
const MENOS_ES_MEJOR: FactorClave[] = ['competencia', 'alquiler'];

/** De 0 a 10, comparando la zona con las demás: la mejor saca 10 y la peor, 0. */
function subpuntaje(zona: DatosDeZona, clave: FactorClave, rubro: Rubro): number {
  const valores = ZONAS.map((otra) => valorCrudo(otra, clave, rubro));
  const minimo = Math.min(...valores);
  const maximo = Math.max(...valores);
  if (maximo === minimo) return 5;
  const relativo = (valorCrudo(zona, clave, rubro) - minimo) / (maximo - minimo);
  return 10 * (MENOS_ES_MEJOR.includes(clave) ? 1 - relativo : relativo);
}

export type Aporte = {
  clave: FactorClave;
  etiqueta: string;
  peso: number;
  puntaje: number;
  aporte: number;
};

export type Coincidencia = 'buena' | 'intermedia' | 'mala';

export const ETIQUETA_DE_COINCIDENCIA: Record<Coincidencia, string> = {
  buena: 'Buena coincidencia',
  intermedia: 'Coincidencia intermedia',
  mala: 'Mala coincidencia',
};
/** Desde qué puntaje (de 0 a 10) una zona es de buena o de intermedia coincidencia. */
export const UMBRAL_BUENA = 6;
export const UMBRAL_INTERMEDIA = 4;

export type ZonaPuntuada = {
  zona: DatosDeZona;
  distanciaKm: number;
  puntaje: number;
  coincidencia: Coincidencia;
  aportes: Aporte[];
  porQue: string[];
  riesgos: string[];
};

const redondear = (valor: number, decimales = 1) => {
  const factor = 10 ** decimales;
  return Math.round(valor * factor) / factor;
};

const numero = new Intl.NumberFormat('es-AR');
const km = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 1, minimumFractionDigits: 1 });
export const formatoKm = (valor: number) => `${km.format(valor)} km`;
export const formatoPuntaje = (valor: number) =>
  new Intl.NumberFormat('es-AR', { minimumFractionDigits: 1, maximumFractionDigits: 1 }).format(
    valor,
  );

function frasePositiva(zona: DatosDeZona, clave: FactorClave, rubro: Rubro): string {
  switch (clave) {
    case 'competencia':
      return `Solo ${zona.competencia[rubro]} ${PLURAL_DE_RUBRO[rubro]} a menos de 500 m`;
    case 'movimiento':
      return `Mucha gente pasando por la calle (${zona.movimiento} de 100)`;
    case 'vecinos':
      return `Zona con muchos vecinos (${numero.format(zona.densidad)} personas por km²)`;
    case 'escuelas':
      return `${zona.escuelas} escuelas a menos de 500 m`;
    case 'oficinas':
      return `${zona.oficinas} oficinas y bancos a menos de 500 m`;
    case 'transporte':
      return `${zona.transporte} paradas y estaciones a menos de 500 m`;
    case 'parques':
      return `${zona.parques} ${zona.parques === 1 ? 'parque o plaza' : 'parques y plazas'} a menos de 500 m`;
    case 'salud':
      return `${zona.salud} hospitales o centros de salud cerca`;
    case 'complementarios':
      return `${zona.complementarios} comercios que atraen gente (farmacias, bancos, supermercados)`;
    case 'poder':
      return 'Vecinos con buen poder de compra';
    case 'alquiler':
      return 'Alquiler más bajo que en la mayoría de las zonas';
  }
}

function fraseDeRiesgo(zona: DatosDeZona, clave: FactorClave, rubro: Rubro): string {
  switch (clave) {
    case 'competencia':
      return `Hay ${zona.competencia[rubro]} ${PLURAL_DE_RUBRO[rubro]} a menos de 500 m: la competencia es fuerte`;
    case 'movimiento':
      return 'Pasa poca gente por la calle';
    case 'vecinos':
      return 'Hay pocos vecinos viviendo cerca';
    case 'escuelas':
      return 'Casi no hay escuelas cerca';
    case 'oficinas':
      return 'Casi no hay oficinas ni bancos cerca';
    case 'transporte':
      return 'Hay pocas paradas y estaciones cerca';
    case 'parques':
      return 'Casi no hay parques ni plazas cerca';
    case 'salud':
      return 'Hay pocos centros de salud cerca';
    case 'complementarios':
      return 'Hay pocos comercios que atraigan gente';
    case 'poder':
      return 'El poder de compra de la zona es más bajo que en otras';
    case 'alquiler':
      return 'El alquiler es más alto que en la mayoría de las zonas';
  }
}

function coincidenciaDe(puntaje: number): Coincidencia {
  if (puntaje >= UMBRAL_BUENA) return 'buena';
  if (puntaje >= UMBRAL_INTERMEDIA) return 'intermedia';
  return 'mala';
}

export function puntuar(zona: DatosDeZona, filtros: Filtros): ZonaPuntuada {
  const pesos = pesosPara(filtros);
  const distanciaKm = distanciaEnKm(LOCAL_ACTUAL, zona);
  const aportes: Aporte[] = FACTORES.map(({ clave, etiqueta }) => {
    const puntaje = subpuntaje(zona, clave, filtros.rubro);
    return { clave, etiqueta, peso: pesos[clave], puntaje, aporte: pesos[clave] * puntaje };
  });
  const cercaDelActual = distanciaKm < DISTANCIA_MINIMA_KM;
  const bruto = aportes.reduce((suma, item) => suma + item.aporte, 0);
  const puntaje = redondear(Math.min(10, Math.max(0, bruto - (cercaDelActual ? 1.2 : 0))));

  // Solo se habla de lo que a la persona le importa.
  const importantes = aportes.filter((item) => filtros.importancia[item.clave] > 0);
  const porAporte = [...aportes].sort((a, b) => b.aporte - a.aporte);
  const porQue = porAporte
    .filter((item) => filtros.importancia[item.clave] > 0 && item.puntaje >= 6)
    .slice(0, 3)
    .map((item) => frasePositiva(zona, item.clave, filtros.rubro));

  const riesgos = importantes
    .filter((item) => item.puntaje < 4.5 && item.peso >= 0.04)
    .sort((a, b) => b.peso * (10 - b.puntaje) - a.peso * (10 - a.puntaje))
    .slice(0, 2)
    .map((item) => fraseDeRiesgo(zona, item.clave, filtros.rubro));
  if (cercaDelActual) {
    riesgos.unshift(
      `Queda a ${formatoKm(distanciaKm)} de tu local actual: podrían quitarse clientes entre sí`,
    );
  }
  if (riesgos.length === 0) {
    riesgos.push('Son datos estimados: confirmalos caminando la zona antes de decidir');
  }

  return {
    zona,
    distanciaKm,
    puntaje,
    coincidencia: coincidenciaDe(puntaje),
    aportes: porAporte,
    porQue: porQue.length > 0 ? porQue : ['Ningún factor se destaca: es una zona pareja'],
    riesgos,
  };
}

/* Límites: lo que hace que una zona quede afuera. */

export type Descartada = { zona: DatosDeZona; distanciaKm: number; motivos: string[] };

export function motivosDeDescarte(zona: DatosDeZona, filtros: Filtros): string[] {
  const { limites } = filtros;
  const motivos: string[] = [];
  const distancia = distanciaEnKm(LOCAL_ACTUAL, zona);
  if (limites.distanciaMaxKm < SIN_LIMITE_KM && distancia > limites.distanciaMaxKm) {
    motivos.push(
      `Está a ${formatoKm(distancia)}: pediste hasta ${formatoKm(limites.distanciaMaxKm)}`,
    );
  }
  const competencia = zona.competencia[filtros.rubro];
  if (limites.competenciaMax < SIN_LIMITE_COMPETENCIA && competencia > limites.competenciaMax) {
    motivos.push(
      `Tiene ${competencia} ${PLURAL_DE_RUBRO[filtros.rubro]}: pediste hasta ${limites.competenciaMax}`,
    );
  }
  if (zona.escuelas < limites.escuelasMin) {
    motivos.push(`Tiene ${zona.escuelas} escuelas: pediste al menos ${limites.escuelasMin}`);
  }
  if (zona.parques < limites.parquesMin) {
    motivos.push(`Tiene ${zona.parques} parques: pediste al menos ${limites.parquesMin}`);
  }
  if (zona.transporte < limites.transporteMin) {
    motivos.push(
      `Tiene ${zona.transporte} paradas y estaciones: pediste al menos ${limites.transporteMin}`,
    );
  }
  if (limites.alquilerMax < SIN_LIMITE_ALQUILER && zona.alquiler > limites.alquilerMax) {
    motivos.push(`Su alquiler es de nivel ${zona.alquiler}: pediste hasta ${limites.alquilerMax}`);
  }
  return motivos;
}

export type Exploracion = {
  /** Las zonas que pasan los límites, de mayor a menor puntaje. */
  ranking: ZonaPuntuada[];
  descartadas: Descartada[];
  /** Cuántas zonas se compararon en total. */
  evaluadas: number;
};

export function explorar(filtros: Filtros): Exploracion {
  const ranking: ZonaPuntuada[] = [];
  const descartadas: Descartada[] = [];
  for (const zona of ZONAS) {
    const motivos = motivosDeDescarte(zona, filtros);
    if (motivos.length > 0) {
      descartadas.push({ zona, distanciaKm: distanciaEnKm(LOCAL_ACTUAL, zona), motivos });
    } else {
      ranking.push(puntuar(zona, filtros));
    }
  }
  ranking.sort((a, b) => b.puntaje - a.puntaje || a.distanciaKm - b.distanciaKm);
  descartadas.sort((a, b) => a.distanciaKm - b.distanciaKm);
  return { ranking, descartadas, evaluadas: ZONAS.length };
}

/* Filtros activos: lo que se apartó de los valores de siempre, listo para mostrar como etiquetas. */

export type FiltroActivo = { id: string; texto: string };

export function filtrosActivos(filtros: Filtros): FiltroActivo[] {
  const { limites } = filtros;
  const base = FILTROS_INICIALES.limites;
  const activos: FiltroActivo[] = [];
  // Solo cuenta lo que se apartó de lo de siempre: si no se tocó nada, no hay filtros activos.
  if (limites.distanciaMaxKm !== base.distanciaMaxKm) {
    activos.push({
      id: 'distanciaMaxKm',
      texto:
        limites.distanciaMaxKm >= SIN_LIMITE_KM
          ? 'Sin límite de distancia'
          : `Hasta ${formatoKm(limites.distanciaMaxKm)} de tu local`,
    });
  }
  if (limites.competenciaMax !== base.competenciaMax) {
    activos.push({
      id: 'competenciaMax',
      texto: `Hasta ${limites.competenciaMax} ${PLURAL_DE_RUBRO[filtros.rubro]} cerca`,
    });
  }
  if (limites.escuelasMin !== base.escuelasMin) {
    activos.push({ id: 'escuelasMin', texto: `Al menos ${limites.escuelasMin} escuelas` });
  }
  if (limites.parquesMin !== base.parquesMin) {
    activos.push({ id: 'parquesMin', texto: `Al menos ${limites.parquesMin} parques` });
  }
  if (limites.transporteMin !== base.transporteMin) {
    activos.push({
      id: 'transporteMin',
      texto: `Al menos ${limites.transporteMin} paradas o estaciones`,
    });
  }
  if (limites.alquilerMax !== base.alquilerMax) {
    activos.push({ id: 'alquilerMax', texto: `Alquiler hasta nivel ${limites.alquilerMax}` });
  }
  for (const { clave, etiqueta } of FACTORES) {
    if (filtros.importancia[clave] === 0) {
      activos.push({ id: `importancia:${clave}`, texto: `Sin mirar: ${etiqueta.toLowerCase()}` });
    }
    if (filtros.importancia[clave] === 2) {
      activos.push({ id: `importancia:${clave}`, texto: `Mucho: ${etiqueta.toLowerCase()}` });
    }
  }
  return activos;
}

/** Quita un filtro activo y vuelve al valor de siempre. */
export function sinFiltro(filtros: Filtros, id: string): Filtros {
  if (id.startsWith('importancia:')) {
    const clave = id.slice('importancia:'.length) as FactorClave;
    return { ...filtros, importancia: { ...filtros.importancia, [clave]: 1 } };
  }
  const limite = id as keyof Limites;
  return {
    ...filtros,
    limites: { ...filtros.limites, [limite]: FILTROS_INICIALES.limites[limite] },
  };
}

/** Qué conviene comprobar en persona, según el rubro. */
export function queVerificar(rubro: Rubro): string[] {
  const propias: Record<Rubro, string> = {
    almacen: 'Fijate si hay un supermercado grande a menos de 500 m y cómo le va.',
    papeleria: 'Confirmá cuándo es el ciclo de clases: la demanda cambia mucho en el año.',
    kiosco: 'Mirá el movimiento a la noche y los horarios de los comercios de al lado.',
  };
  return [
    'Caminá la zona un día de semana y un sábado, a distintas horas.',
    'Mirá locales en alquiler y pedí el precio real: acá el alquiler es un dato de ejemplo.',
    `Consultá en tu municipio qué habilitación necesita ${UN_RUBRO[rubro]} en esa dirección.`,
    propias[rubro],
  ];
}

/* Puntos para el mapa */

export type TipoDePunto =
  'escuela' | 'parque' | 'transporte' | 'competencia' | 'oficina' | 'salud' | 'complementario';

export type Punto = { tipo: TipoDePunto; latitud: number; longitud: number; nombre: string };

export const CAPAS: { tipo: TipoDePunto; etiqueta: string }[] = [
  { tipo: 'competencia', etiqueta: 'Comercios similares' },
  { tipo: 'escuela', etiqueta: 'Escuelas' },
  { tipo: 'parque', etiqueta: 'Parques y plazas' },
  { tipo: 'transporte', etiqueta: 'Paradas y estaciones' },
  { tipo: 'oficina', etiqueta: 'Oficinas y bancos' },
  { tipo: 'salud', etiqueta: 'Salud' },
  { tipo: 'complementario', etiqueta: 'Comercios que atraen gente' },
];

export const CAPAS_INICIALES: Record<TipoDePunto, boolean> = {
  competencia: true,
  escuela: true,
  parque: true,
  transporte: false,
  oficina: false,
  salud: false,
  complementario: false,
};

const METROS_POR_GRADO = 111_320;
const ANGULO_AUREO = 2.399963;

function dispersar(
  centro: { latitud: number; longitud: number },
  cantidad: number,
  semilla: number,
): { latitud: number; longitud: number }[] {
  return Array.from({ length: cantidad }, (_, indice) => {
    const angulo = indice * ANGULO_AUREO + semilla;
    const metros = 90 + 330 * Math.sqrt((indice + 0.5) / Math.max(cantidad, 1));
    return {
      latitud: centro.latitud + (metros * Math.cos(angulo)) / METROS_POR_GRADO,
      longitud:
        centro.longitud +
        (metros * Math.sin(angulo)) / (METROS_POR_GRADO * Math.cos(aRadianes(centro.latitud))),
    };
  });
}

const semillaDe = (texto: string) =>
  [...texto].reduce((suma, letra) => (suma * 31 + letra.charCodeAt(0)) % 997, 7) / 100;

/** Dónde dibujar lo que hay alrededor de la zona. Es siempre el mismo dibujo para la misma zona. */
export function puntosDeLaZona(zona: DatosDeZona, rubro: Rubro): Punto[] {
  const semilla = semillaDe(zona.id);
  const grupos: [TipoDePunto, number, string][] = [
    ['competencia', zona.competencia[rubro], `Competencia: ${NOMBRE_DE_RUBRO[rubro]}`],
    ['escuela', zona.escuelas, 'Escuela'],
    ['parque', zona.parques, 'Parque o plaza'],
    ['transporte', zona.transporte, 'Parada o estación'],
    ['oficina', zona.oficinas, 'Oficina o banco'],
    ['salud', zona.salud, 'Hospital o centro de salud'],
    ['complementario', zona.complementarios, 'Farmacia, banco o supermercado'],
  ];
  return grupos.flatMap(([tipo, cantidad, nombre], grupo) =>
    dispersar(zona, cantidad, semilla + grupo * 1.7).map((posicion) => ({
      tipo,
      nombre,
      ...posicion,
    })),
  );
}

/* Texto para compartir */

export function resumenParaCompartir(filtros: Filtros, exploracion: Exploracion): string {
  const lineas = exploracion.ranking
    .slice(0, 3)
    .map(
      (item, indice) =>
        `${indice + 1}. ${item.zona.nombre} (${ETIQUETA_DE_COINCIDENCIA[item.coincidencia].toLowerCase()}). ${item.porQue[0]}.`,
    );
  return [
    `Dónde abrir mi próximo local, ${NOMBRE_DE_RUBRO[filtros.rubro]} (estimación de Sami, con datos de ejemplo):`,
    ...lineas,
    'Ojo: es una estimación para decidir mejor, no una predicción de ventas.',
  ].join('\n');
}
