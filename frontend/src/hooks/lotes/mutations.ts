import { useMutation, useQueryClient } from '@tanstack/react-query';
import { reintentarLectura, subirLote } from '@/api/lotes';
import { lecturaKey } from './queries';

export function useSubirLote() {
  return useMutation({ mutationFn: subirLote });
}

export function useReintentarLectura(loteId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => reintentarLectura(loteId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: lecturaKey(loteId) }),
  });
}
