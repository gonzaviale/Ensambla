import { useQuery } from '@tanstack/react-query';
import { obtenerLectura } from '@/api/lotes';
import { ESTADOS_EN_CURSO } from '@/types/lectura';

const INTERVALO_CONSULTA_MS = 2000;

export const lecturaKey = (loteId: string) => ['lotes', loteId, 'lectura'] as const;

// Consulta la lectura cada 2 s mientras la IA todavía la está procesando.
export function useLectura(loteId: string) {
  return useQuery({
    queryKey: lecturaKey(loteId),
    queryFn: ({ signal }) => obtenerLectura(loteId, signal),
    refetchInterval: ({ state }) =>
      !state.data || ESTADOS_EN_CURSO.includes(state.data.estado) ? INTERVALO_CONSULTA_MS : false,
  });
}
