import { useMutation, useQueryClient } from '@tanstack/react-query';
import { reintentarLectura, subirLote } from '@/api/lotes';
import { lecturaKey, loteKey } from './queries';

// El lote recién creado ya trae sus páginas: se deja en la caché para no volver a pedirlo.
export function useSubirLote() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: subirLote,
    onSuccess: (lote) => queryClient.setQueryData(loteKey(lote.id), lote),
  });
}

export function useReintentarLectura(loteId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => reintentarLectura(loteId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: lecturaKey(loteId) }),
  });
}
