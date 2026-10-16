import type {
  EstadoSuscripcion,
  FuncionPlan,
  PeriodoPlan,
  PlanId,
  Suscripcion,
} from '@/types/negocio';

// Precios ilustrativos en pesos: hipótesis a validar en el piloto (docs/propuesta-de-tema.md, IX).
// La prueba gratuita dura 7 días y da acceso al plan más completo (T-068). Todo es simulado.
export const DIAS_DE_PRUEBA = 7;
export const MESES_PAGOS_ANUAL = 10;
const MS_DIA = 24 * 60 * 60 * 1000;

export type DefinicionPlan = {
  id: PlanId;
  nombre: string;
  bajada: string;
  precioMensual: number;
  /** null = sin tope. */
  bloques: number | null;
  /** Cargas de historia (foto, lista o planilla) por mes; null = sin tope. */
  cargasHistoriaMes: number | null;
  personas: number;
  funciones: FuncionPlan[];
  destacado: boolean;
};

export const PLANES: Record<PlanId, DefinicionPlan> = {
  gratis: {
    id: 'gratis',
    nombre: 'Gratis',
    bajada: 'Para armar tu sistema y ver tus primeras respuestas.',
    precioMensual: 0,
    bloques: 3,
    cargasHistoriaMes: 2,
    personas: 1,
    funciones: [],
    destacado: false,
  },
  negocio: {
    id: 'negocio',
    nombre: 'Negocio',
    bajada: 'Todos los bloques y todas las respuestas, sin límites.',
    precioMensual: 9900,
    bloques: null,
    cargasHistoriaMes: null,
    personas: 1,
    funciones: [
      'bloques-ilimitados',
      'respuestas-completas',
      'comparativas',
      'historial',
      'descarga-xlsx',
    ],
    destacado: true,
  },
  'negocio-plus': {
    id: 'negocio-plus',
    nombre: 'Negocio +',
    bajada: 'Para trabajar en equipo y manejar más de un negocio.',
    precioMensual: 17900,
    bloques: null,
    cargasHistoriaMes: null,
    personas: 3,
    funciones: [
      'bloques-ilimitados',
      'respuestas-completas',
      'comparativas',
      'historial',
      'descarga-xlsx',
      'varias-personas',
      'varios-negocios',
      'resumen-semanal',
    ],
    destacado: false,
  },
};

export const ORDEN_PLANES: PlanId[] = ['gratis', 'negocio', 'negocio-plus'];

export const NOMBRE_FUNCION: Record<FuncionPlan, string> = {
  'bloques-ilimitados': 'Todos los bloques que quieras',
  'respuestas-completas': 'Todas las respuestas',
  comparativas: 'Comparar con el mes anterior y entre listas',
  historial: 'Historial de meses',
  'descarga-xlsx': 'Descargar tus datos en planilla',
  'varias-personas': 'Hasta 3 personas con acceso',
  'varios-negocios': 'Más de un negocio',
  'resumen-semanal': 'Resumen semanal por WhatsApp',
};

export const planIncluye = (plan: PlanId, funcion: FuncionPlan) =>
  PLANES[plan].funciones.includes(funcion);

/** El plan más barato que incluye la función. */
export function planMinimoPara(funcion: FuncionPlan): PlanId {
  return ORDEN_PLANES.find((id) => planIncluye(id, funcion)) ?? 'negocio-plus';
}

/** Precio por mes según el período: pagando por año se pagan 10 de 12 meses. */
export function precioPorMes(plan: PlanId, periodo: PeriodoPlan): number {
  const base = PLANES[plan].precioMensual;
  return periodo === 'anual' ? Math.round((base * MESES_PAGOS_ANUAL) / 12) : base;
}

export function importeACobrar(plan: PlanId, periodo: PeriodoPlan): number {
  const base = PLANES[plan].precioMensual;
  return periodo === 'anual' ? base * MESES_PAGOS_ANUAL : base;
}

export const diasEntre = (desde: string | Date, hasta: string | Date) =>
  (new Date(hasta).getTime() - new Date(desde).getTime()) / MS_DIA;

export type PlanEfectivo = {
  /** El plan cuyas funciones se pueden usar ahora. */
  plan: PlanId;
  estado: EstadoSuscripcion;
  /** Días que quedan de la prueba (0 si no hay prueba en curso). */
  diasDePrueba: number;
  enPrueba: boolean;
};

/** Qué puede usar hoy: la prueba vencida o la suscripción cancelada vuelven al plan Gratis. */
export function planEfectivo(suscripcion: Suscripcion, ahora: Date = new Date()): PlanEfectivo {
  const { estado } = suscripcion;
  if (estado === 'prueba') {
    const quedan = suscripcion.pruebaHasta ? diasEntre(ahora, suscripcion.pruebaHasta) : 0;
    if (quedan > 0) {
      return { plan: 'negocio-plus', estado, diasDePrueba: Math.ceil(quedan), enPrueba: true };
    }
    return { plan: 'gratis', estado: 'vencida', diasDePrueba: 0, enPrueba: false };
  }
  if (estado === 'activa') {
    return { plan: suscripcion.plan, estado, diasDePrueba: 0, enPrueba: false };
  }
  if (estado === 'cancelada') {
    const vigente = suscripcion.activoHasta ? diasEntre(ahora, suscripcion.activoHasta) > 0 : false;
    return {
      plan: vigente ? suscripcion.plan : 'gratis',
      estado: vigente ? estado : 'vencida',
      diasDePrueba: 0,
      enPrueba: false,
    };
  }
  return { plan: 'gratis', estado, diasDePrueba: 0, enPrueba: false };
}
