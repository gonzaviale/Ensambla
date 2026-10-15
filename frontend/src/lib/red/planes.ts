import type {
  EstadoSuscripcion,
  FuncionPlan,
  PeriodoPlan,
  PlanId,
  Suscripcion,
  TipoAlerta,
} from '@/types/red';
import { diasEntre } from './calculos';

// Precios ilustrativos en pesos: hipótesis a validar con comercios reales (docs/propuesta-de-tema.md, IX).
// La prueba gratuita dura 7 días y da acceso al plan más completo (T-068).
export const DIAS_DE_PRUEBA = 7;
export const MESES_PAGOS_ANUAL = 10;

export type DefinicionPlan = {
  id: PlanId;
  nombre: string;
  bajada: string;
  precioMensual: number;
  /** null = sin tope. */
  listasPorMes: number | null;
  productos: number | null;
  usuarios: number;
  funciones: FuncionPlan[];
  destacado: boolean;
};

export const PLANES: Record<PlanId, DefinicionPlan> = {
  gratis: {
    id: 'gratis',
    nombre: 'Gratis',
    bajada: 'Para probar con tus listas y ver el primer precio corregido.',
    precioMensual: 0,
    listasPorMes: 2,
    productos: 40,
    usuarios: 1,
    funciones: ['alertas-precio'],
    destacado: false,
  },
  comercio: {
    id: 'comercio',
    nombre: 'Comercio',
    bajada: 'Todas tus listas y avisos, para que nunca vendas por debajo del costo.',
    precioMensual: 9900,
    listasPorMes: null,
    productos: 500,
    usuarios: 1,
    funciones: [
      'alertas-precio',
      'listas-ilimitadas',
      'stock-por-agotarse',
      'resumen-del-dia',
      'etiquetas',
      'historial-costos',
    ],
    destacado: false,
  },
  red: {
    id: 'red',
    nombre: 'Comercio + Red',
    bajada: 'Además, compará proveedores, enterate de ofertas y mirá si comprás caro.',
    precioMensual: 17900,
    listasPorMes: null,
    productos: 2000,
    usuarios: 3,
    funciones: [
      'alertas-precio',
      'listas-ilimitadas',
      'stock-por-agotarse',
      'resumen-del-dia',
      'etiquetas',
      'historial-costos',
      'comparar-proveedores',
      'comparacion-zona',
      'ofertas',
      'usuarios',
    ],
    destacado: true,
  },
};

export const ORDEN_PLANES: PlanId[] = ['gratis', 'comercio', 'red'];

export const NOMBRE_FUNCION: Record<FuncionPlan, string> = {
  'alertas-precio': 'Avisos de precio en riesgo y de lista nueva',
  'listas-ilimitadas': 'Listas de proveedores sin límite por mes',
  'stock-por-agotarse': 'Aviso de stock por agotarse y pedido sugerido',
  'resumen-del-dia': 'Resumen del día',
  etiquetas: 'Etiquetas y lista de cambios para el mostrador',
  'historial-costos': 'Historial de costos y de qué lista salió cada uno',
  'comparar-proveedores': 'Comparar el costo entre proveedores',
  'comparacion-zona': 'Saber si comprás caro frente a tu zona',
  ofertas: 'Ofertas de proveedores de lo que vendés',
  usuarios: 'Hasta 3 personas con acceso',
};

/** Qué función del plan necesita cada tipo de aviso. */
export const FUNCION_DE_ALERTA: Record<TipoAlerta, FuncionPlan> = {
  'precio-en-riesgo': 'alertas-precio',
  'lista-nueva': 'alertas-precio',
  'cambio-raro': 'alertas-precio',
  'stock-por-agotarse': 'stock-por-agotarse',
  'oferta-que-sirve': 'ofertas',
  'compras-caro': 'comparacion-zona',
  'mejor-proveedor': 'comparar-proveedores',
};

export const planIncluye = (plan: PlanId, funcion: FuncionPlan) =>
  PLANES[plan].funciones.includes(funcion);

/** El plan más barato que incluye la función. */
export function planMinimoPara(funcion: FuncionPlan): PlanId {
  return ORDEN_PLANES.find((id) => planIncluye(id, funcion)) ?? 'red';
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

export type PlanEfectivo = {
  /** El plan cuyas funciones se pueden usar ahora. */
  plan: PlanId;
  estado: EstadoSuscripcion;
  /** Días que quedan de la prueba (0 si no hay prueba en curso). */
  diasDePrueba: number;
  enPrueba: boolean;
};

/** Qué puede usar hoy el comercio: la prueba vencida o la suscripción cancelada vuelven al plan gratis. */
export function planEfectivo(suscripcion: Suscripcion, ahora: Date = new Date()): PlanEfectivo {
  const { estado } = suscripcion;
  if (estado === 'prueba') {
    const quedan = suscripcion.pruebaHasta ? diasEntre(ahora, suscripcion.pruebaHasta) : 0;
    if (quedan > 0) {
      return { plan: 'red', estado, diasDePrueba: Math.ceil(quedan), enPrueba: true };
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
