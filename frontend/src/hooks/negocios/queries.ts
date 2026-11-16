import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { planesQuery } from '@/hooks/planes/queries';
import { sistemasQuery } from '@/hooks/sistemas/queries';
import { leerNegocioGuardado, listaDeNegocios } from '@/lib/negocios/lista';
import { useSistemaStore } from '@/stores/sistemaStore';

// Cada negocio de la cuenta es un cuaderno: la lista no se le pide al servidor, sale de los cuadernos.

/** Cuántos cuadernos deja tener el plan actual. Mientras no se sabe no se limita: el servidor lo hace al guardar. */
function useLimiteDeNegocios() {
  const { data } = useQuery(planesQuery());
  const actual = data?.planes.find((plan) => plan.codigo === data.suscripcion.codigoPlanActual);
  return actual?.limiteProyectos ?? null;
}

/** Dentro de la app: los negocios son los cuadernos que se están editando y el activo es el elegido en la barra. */
export function useNegociosDeLaCuenta() {
  const sistemas = useSistemaStore((estado) => estado.sistemas);
  const activoId = useSistemaStore((estado) => estado.negocioActivoId);
  const limite = useLimiteDeNegocios();
  const data = useMemo(
    () => listaDeNegocios(sistemas, activoId, limite),
    [sistemas, activoId, limite],
  );
  return { data };
}

/** Antes de entrar a la app (el pop-up de elegir negocio): los cuadernos se leen como los manda el servidor. */
export function useNegociosAlEntrar() {
  const consulta = useQuery({ ...sistemasQuery(), retry: false });
  const limite = useLimiteDeNegocios();
  const data = useMemo(
    () =>
      consulta.data ? listaDeNegocios(consulta.data, leerNegocioGuardado(), limite) : undefined,
    [consulta.data, limite],
  );
  return { data, isError: consulta.isError, isSuccess: consulta.isSuccess };
}
