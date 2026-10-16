import type { MedioPago, PeriodoPlan, PlanId, Suscripcion } from '@/types/negocio';
import { DIAS_DE_PRUEBA, PLANES, importeACobrar } from './planes';

// Transiciones puras de la suscripción simulada: (suscripción, datos, ahora) → suscripción nueva.
// No hay backend de planes (T-066 a T-070): el estado vive en el navegador (stores/suscripcion.ts).

const sumarDias = (desde: Date, dias: number) =>
  new Date(desde.getTime() + dias * 24 * 60 * 60 * 1000).toISOString();

let secuencia = 0;
const nuevoId = (prefijo: string, ahora: Date) =>
  `${prefijo}-${ahora.getTime().toString(36)}${(secuencia++).toString(36)}`;

/** Estado inicial de la demostración: la prueba de 7 días ya empezó (hace 2 días). */
export function suscripcionInicial(ahora: Date = new Date()): Suscripcion {
  return {
    plan: 'negocio-plus',
    estado: 'prueba',
    periodo: 'mensual',
    pruebaUsada: true,
    pruebaDesde: sumarDias(ahora, -2),
    pruebaHasta: sumarDias(ahora, DIAS_DE_PRUEBA - 2),
    proximoCobro: null,
    activoHasta: null,
    medioPago: null,
    pagos: [],
  };
}

/** Prueba de 7 días del plan más completo. Se puede usar una sola vez. */
export function iniciarPrueba(s: Suscripcion, ahora: Date = new Date()): Suscripcion {
  if (s.pruebaUsada || s.estado === 'activa') return s;
  return {
    ...s,
    plan: 'negocio-plus',
    estado: 'prueba',
    pruebaUsada: true,
    pruebaDesde: ahora.toISOString(),
    pruebaHasta: sumarDias(ahora, DIAS_DE_PRUEBA),
  };
}

export type DatosSuscripcion = { plan: PlanId; periodo: PeriodoPlan; medioPago: MedioPago };

/** Pago aprobado: activa el plan. Los datos de pago son simulados, nunca reales. */
export function suscribirse(
  s: Suscripcion,
  datos: DatosSuscripcion,
  ahora: Date = new Date(),
): Suscripcion {
  if (datos.plan === 'gratis') return s;
  const dias = datos.periodo === 'anual' ? 365 : 30;
  return {
    ...s,
    plan: datos.plan,
    estado: 'activa',
    periodo: datos.periodo,
    medioPago: datos.medioPago,
    proximoCobro: sumarDias(ahora, dias),
    activoHasta: null,
    pagos: [
      {
        id: nuevoId('pago', ahora),
        fecha: ahora.toISOString(),
        concepto: `Plan ${PLANES[datos.plan].nombre} · ${datos.periodo === 'anual' ? 'anual' : 'mensual'}`,
        importe: importeACobrar(datos.plan, datos.periodo),
        estado: 'aprobado',
      },
      ...s.pagos,
    ],
  };
}

/** Cancelar no corta de golpe: lo ya pagado sigue activo hasta el próximo cobro. */
export function cancelarSuscripcion(s: Suscripcion, ahora: Date = new Date()): Suscripcion {
  if (s.estado === 'prueba') {
    return { ...s, plan: 'gratis', estado: 'gratis', pruebaHasta: ahora.toISOString() };
  }
  if (s.estado !== 'activa') return s;
  return {
    ...s,
    estado: 'cancelada',
    activoHasta: s.proximoCobro ?? ahora.toISOString(),
    proximoCobro: null,
  };
}

export function reactivarSuscripcion(s: Suscripcion): Suscripcion {
  if (s.estado !== 'cancelada' || !s.activoHasta) return s;
  return { ...s, estado: 'activa', proximoCobro: s.activoHasta, activoHasta: null };
}

/** Sólo para la demostración: adelanta el fin de la prueba para mostrar cómo queda el plan Gratis. */
export function simularFinDePrueba(s: Suscripcion, ahora: Date = new Date()): Suscripcion {
  if (s.estado !== 'prueba') return s;
  return { ...s, pruebaHasta: sumarDias(ahora, -1) };
}
