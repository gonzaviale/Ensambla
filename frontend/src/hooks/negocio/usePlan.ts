import { useMemo } from 'react';
import { useShallow } from 'zustand/react/shallow';
import { PLANES, planEfectivo, planIncluye } from '@/lib/planes/planes';
import { useSuscripcionStore } from '@/stores/suscripcion';
import type { FuncionPlan } from '@/types/negocio';

// Plan y suscripción simulados (stores/suscripcion.ts). La UI sólo importa estos hooks.

export const useSuscripcion = () => useSuscripcionStore((s) => s.suscripcion);

/** Plan que rige hoy, estado de la suscripción y qué incluye. */
export function usePlanActual() {
  const suscripcion = useSuscripcion();
  return useMemo(() => {
    const efectivo = planEfectivo(suscripcion, new Date());
    return {
      ...efectivo,
      definicion: PLANES[efectivo.plan],
      suscripcion,
      incluye: (funcion: FuncionPlan) => planIncluye(efectivo.plan, funcion),
    };
  }, [suscripcion]);
}

/** Acciones de la suscripción (referencias estables). */
export function useAccionesPlan() {
  return useSuscripcionStore(
    useShallow((s) => ({
      iniciarPrueba: s.iniciarPrueba,
      suscribirse: s.suscribirse,
      cancelarSuscripcion: s.cancelarSuscripcion,
      reactivarSuscripcion: s.reactivarSuscripcion,
      simularFinDePrueba: s.simularFinDePrueba,
      reiniciar: s.reiniciar,
    })),
  );
}
