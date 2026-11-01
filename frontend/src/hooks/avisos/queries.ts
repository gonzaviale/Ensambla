import { queryOptions, useQuery } from '@tanstack/react-query';
import { listarAvisosPendientes, obtenerImpactoAvisos } from '@/api/avisos';
import type { AvisoDelServidor } from '@/types/avisos';

export const avisosKeys = {
  all: ['avisos'] as const,
  pendientes: ['avisos', 'pendientes'] as const,
  impacto: ['avisos', 'impacto'] as const,
};

export const avisosPendientesQuery = () =>
  queryOptions({
    queryKey: avisosKeys.pendientes,
    queryFn: ({ signal }) => listarAvisosPendientes(signal),
    // El motor corre solo en el servidor: se vuelve a preguntar cada tanto sin que la persona lo pida.
    refetchInterval: 60_000,
    // Si el servidor no responde, los avisos que Sami calcula en el navegador siguen funcionando.
    retry: false,
  });

const SIN_AVISOS: AvisoDelServidor[] = [];

/** Los avisos pendientes del motor del servidor; vacío mientras carga o si falla. `respondio` es true si contestó bien. */
export function useAvisosDelServidor() {
  const consulta = useQuery(avisosPendientesQuery());
  return { avisos: consulta.data ?? SIN_AVISOS, cargando: consulta.isPending, respondio: consulta.isSuccess };
}

export const impactoDeAvisosQuery = () =>
  queryOptions({
    queryKey: avisosKeys.impacto,
    queryFn: ({ signal }) => obtenerImpactoAvisos(signal),
    // El servidor mide una vez al día como mucho: no hace falta preguntar seguido, y si falla la pantalla sigue igual.
    staleTime: 5 * 60_000,
    retry: false,
  });

/** Qué pasó con las ventas después de aplicar avisos; `undefined` mientras carga o si el servidor no responde. */
export function useImpactoDeAvisos() {
  return useQuery(impactoDeAvisosQuery()).data;
}
