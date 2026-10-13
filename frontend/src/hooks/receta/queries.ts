import { queryOptions, useQuery } from '@tanstack/react-query';
import { obtenerReceta } from '@/api/receta';

export const recetaKeys = {
  registro: (registroId: string) => ['receta', registroId] as const,
};

// El costo depende de los precios actuales de los insumos, que pueden haber cambiado desde la última vez:
// cada vez que se abre la receta se vuelve a pedir.
export const recetaQuery = (registroId: string) =>
  queryOptions({
    queryKey: recetaKeys.registro(registroId),
    queryFn: ({ signal }) => obtenerReceta(registroId, signal),
    staleTime: 0,
  });

export function useReceta(registroId: string) {
  return useQuery(recetaQuery(registroId));
}
