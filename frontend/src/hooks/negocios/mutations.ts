import { useMutation, useQueryClient } from '@tanstack/react-query';
import { activarNegocio } from '@/api/negocios';
import { negociosKeys } from './queries';

// Pasa a ser activo el negocio elegido. Sólo pone al día lo que es de la cuenta (la lista y el resumen conjunto):
// al iniciar sesión todavía no hay datos de ningún negocio cargados. Cambiar de negocio con la app abierta
// (el selector, T-202) además tiene que descartar los datos del negocio anterior.
export function useActivarNegocio() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (negocioId: string) => activarNegocio(negocioId),
    onSuccess: (lista) => {
      queryClient.setQueryData(negociosKeys.lista, lista);
      void queryClient.invalidateQueries({ queryKey: negociosKeys.resumen });
    },
  });
}
