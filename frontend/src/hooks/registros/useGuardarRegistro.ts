import { useMutation, useQueryClient } from '@tanstack/react-query';
import { crearRegistro } from '@/api/registros';
import type { ResultadoGuardado, ValoresRegistro } from '@/types/registro';
import { registrosKeys } from './queries';
import { resultadoFallido } from './resultadoFallido';

export function useGuardarRegistro(bloqueId: string) {
  const queryClient = useQueryClient();
  const { mutateAsync } = useMutation({
    mutationFn: (valores: ValoresRegistro) => crearRegistro(bloqueId, valores),
    retry: false,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: registrosKeys.bloque(bloqueId) });
    },
  });

  return async (valores: ValoresRegistro): Promise<ResultadoGuardado> => {
    try {
      await mutateAsync(valores);
      return { exito: true };
    } catch (error) {
      return resultadoFallido(error);
    }
  };
}
