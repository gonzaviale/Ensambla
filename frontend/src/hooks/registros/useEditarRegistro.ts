import { useMutation, useQueryClient } from '@tanstack/react-query';
import { editarRegistro } from '@/api/registros';
import type { CambiosRegistro, ResultadoEdicion } from '@/types/registro';
import { registrosKeys } from './queries';
import { resultadoFallido } from './resultadoFallido';

export function useEditarRegistro(bloqueId: string) {
  const queryClient = useQueryClient();
  const { mutateAsync } = useMutation({
    mutationFn: ({ id, cambios }: { id: string; cambios: CambiosRegistro }) =>
      editarRegistro(id, cambios),
    retry: false,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: registrosKeys.bloque(bloqueId) });
    },
  });

  return async (id: string, cambios: CambiosRegistro): Promise<ResultadoEdicion> => {
    try {
      return { exito: true, registro: await mutateAsync({ id, cambios }) };
    } catch (error) {
      return resultadoFallido(error);
    }
  };
}
