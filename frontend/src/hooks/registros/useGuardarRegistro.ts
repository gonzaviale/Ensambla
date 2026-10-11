import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ApiError } from '@/api/apiError';
import { crearRegistro } from '@/api/registros';
import type { ResultadoGuardado, ValoresRegistro } from '@/types/registro';

export function useGuardarRegistro(bloqueId: string) {
  const queryClient = useQueryClient();
  const { mutateAsync } = useMutation({
    mutationFn: (valores: ValoresRegistro) => crearRegistro(bloqueId, valores),
    retry: false,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['registros', bloqueId] });
    },
  });

  return async (valores: ValoresRegistro): Promise<ResultadoGuardado> => {
    try {
      await mutateAsync(valores);
      return { exito: true };
    } catch (error) {
      return {
        exito: false,
        mensaje:
          error instanceof ApiError
            ? error.message
            : 'No pudimos confirmar el guardado. Revisá antes de volver a intentar.',
        errores: error instanceof ApiError ? error.fieldErrors : {},
      };
    }
  };
}
