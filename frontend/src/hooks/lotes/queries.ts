import { useQuery } from '@tanstack/react-query';
import { obtenerLectura, obtenerLote } from '@/api/lotes';
import { ESTADOS_EN_CURSO } from '@/types/lectura';

const INTERVALO_CONSULTA_MS = 2000;

export const lecturaKey = (loteId: string) => ['lotes', loteId, 'lectura'] as const;
export const loteKey = (loteId: string) => ['lotes', loteId] as const;

// Consulta la lectura cada 2 s mientras la IA todavía la está procesando.
export function useLectura(loteId: string) {
  return useQuery({
    queryKey: lecturaKey(loteId),
    queryFn: ({ signal }) => obtenerLectura(loteId, signal),
    refetchInterval: ({ state }) =>
      !state.data || ESTADOS_EN_CURSO.includes(state.data.estado) ? INTERVALO_CONSULTA_MS : false,
  });
}

// Las páginas del lote (ids y orden) para mostrar sus fotos junto a los datos.
export function useLote(loteId: string) {
  return useQuery({
    queryKey: loteKey(loteId),
    queryFn: ({ signal }) => obtenerLote(loteId, signal),
  });
}
