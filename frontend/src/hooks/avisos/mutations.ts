import { useMutation, useQueryClient } from '@tanstack/react-query';
import { aplicarAviso, descartarAviso } from '@/api/avisos';
import type { AvisoDelServidor } from '@/types/avisos';
import { avisosKeys } from './queries';

// El aviso sale de la lista al instante; si el servidor lo rechaza, vuelve. Al terminar se pregunta de nuevo,
// porque aplicar un cambio de datos puede hacer que otros avisos dejen de valer.
function useResolverAviso(resolver: (id: string) => Promise<AvisoDelServidor>) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: resolver,
    onMutate: async (id: string) => {
      await queryClient.cancelQueries({ queryKey: avisosKeys.pendientes });
      const anteriores = queryClient.getQueryData<AvisoDelServidor[]>(avisosKeys.pendientes);
      queryClient.setQueryData<AvisoDelServidor[]>(avisosKeys.pendientes, (actuales) =>
        actuales?.filter((aviso) => aviso.id !== id),
      );
      return { anteriores };
    },
    onError: (_error, _id, contexto) => {
      if (contexto?.anteriores) queryClient.setQueryData(avisosKeys.pendientes, contexto.anteriores);
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: avisosKeys.pendientes }),
  });
}

export const useAplicarAviso = () => useResolverAviso(aplicarAviso);

export const useDescartarAviso = () => useResolverAviso(descartarAviso);
