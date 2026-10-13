import { useMutation, useQueryClient } from '@tanstack/react-query';
import { guardarReceta } from '@/api/receta';
import type { GuardarReceta } from '@/types/receta';
import { recetaKeys } from './queries';

export function useGuardarReceta(registroId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (receta: GuardarReceta) => guardarReceta(registroId, receta),
    onSuccess: (receta) => queryClient.setQueryData(recetaKeys.registro(registroId), receta),
  });
}
