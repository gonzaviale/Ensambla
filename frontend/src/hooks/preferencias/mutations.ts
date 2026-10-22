import { useMutation, useQueryClient } from '@tanstack/react-query';
import { guardarPreferencias } from '@/api/sistemas';
import type { Preferencias } from '@/types/preferencias';
import { preferenciasKeys } from './queries';

// La pantalla ve el cambio al instante; si el servidor lo rechaza, vuelve a lo anterior.
export function useGuardarPreferencias() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: guardarPreferencias,
    onMutate: async (siguientes: Preferencias) => {
      await queryClient.cancelQueries({ queryKey: preferenciasKeys.all });
      const anteriores = queryClient.getQueryData<Preferencias>(preferenciasKeys.all);
      queryClient.setQueryData(preferenciasKeys.all, siguientes);
      return { anteriores };
    },
    onError: (_error, _siguientes, contexto) => {
      if (contexto?.anteriores) queryClient.setQueryData(preferenciasKeys.all, contexto.anteriores);
    },
  });
}
