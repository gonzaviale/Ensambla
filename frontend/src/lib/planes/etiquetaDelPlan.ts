import type { Plan } from '@/types/planes';
import { presentacionDelPlan } from './presentacionDelPlan';

export type EtiquetaDelPlan = { texto: string; clase: 'plan-label' | 'plan-destacado' };

type SituacionDelPlan = { plan: Plan; actual: boolean; pruebaActiva: boolean };

const ETIQUETA_DESTACADO: EtiquetaDelPlan = { texto: 'El más completo', clase: 'plan-destacado' };

export function etiquetaDelPlan(situacion: SituacionDelPlan): EtiquetaDelPlan | null {
  if (situacion.actual) return etiquetaDelPlanActual(situacion.pruebaActiva);
  if (presentacionDelPlan(situacion.plan).destacado) return ETIQUETA_DESTACADO;
  return null;
}

function etiquetaDelPlanActual(pruebaActiva: boolean): EtiquetaDelPlan {
  return { texto: pruebaActiva ? 'En prueba' : 'Tu plan', clase: 'plan-label' };
}
