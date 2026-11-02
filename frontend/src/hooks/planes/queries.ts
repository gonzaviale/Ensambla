import { queryOptions, useQuery } from '@tanstack/react-query';
import { consultarPlanes } from '@/api/planes';
import { ApiError } from '@/api/apiError';

export const planesKeys = {
  all: ['planes'] as const,
  actual: ['planes', 'actual'] as const,
  prueba: ['planes', 'iniciar-prueba'] as const,
};

export const planesQuery = () =>
  queryOptions({
    queryKey: planesKeys.actual,
    queryFn: ({ signal }) => consultarPlanes(signal),
    staleTime: 0,
    retry: false,
    refetchOnWindowFocus: 'always',
    refetchInterval: (query) => {
      const datos = query.state.data;
      if (query.state.status === 'error' || !datos?.suscripcion.pruebaActiva || !datos.prueba)
        return 60_000;
      // Se usa la hora del servidor, sin depender de que el reloj del celular esté bien ajustado.
      const restante = Date.parse(datos.prueba.finEn) - Date.parse(datos.consultadoEn);
      return Math.min(60_000, Math.max(1_000, restante));
    },
  });

/** Si el plan actual incluye una función (por ejemplo `REPORTES_FILTRADOS`); mientras se consulta no se sabe. */
export function useFuncionDePlan(funcion: string) {
  const { data, isPending } = usePlanes();
  const plan = data?.planes.find((candidato) => candidato.codigo === data.suscripcion.codigoPlanActual);
  return { cargando: isPending, incluida: plan?.funciones.includes(funcion) ?? false };
}

export function usePlanes() {
  const consulta = useQuery(planesQuery());
  return {
    data: consulta.data,
    isPending: consulta.isPending,
    isError: consulta.isError,
    isFetching: consulta.isFetching,
    refetch: consulta.refetch,
    mensajeError:
      consulta.error instanceof ApiError
        ? consulta.error.message
        : 'No pudimos consultar tu plan. Probá nuevamente.',
  };
}
