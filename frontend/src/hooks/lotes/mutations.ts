import { useMutation } from '@tanstack/react-query';
import { subirLote } from '@/api/lotes';

export function useSubirLote() {
  return useMutation({ mutationFn: subirLote });
}
