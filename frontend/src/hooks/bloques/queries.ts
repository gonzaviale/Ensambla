import { queryOptions, useQuery } from '@tanstack/react-query';
import { listarBloques } from '@/api/bloques';
import { listarCampos } from '@/api/campos';

export const bloquesKeys = {
  lista: (proyectoId: string) => ['bloques', proyectoId] as const,
};

export const camposKeys = {
  lista: (bloqueId: string) => ['campos', bloqueId] as const,
};

export const bloquesQuery = (proyectoId: string) =>
  queryOptions({
    queryKey: bloquesKeys.lista(proyectoId),
    queryFn: ({ signal }) => listarBloques(proyectoId, signal),
  });

export const camposQuery = (bloqueId: string) =>
  queryOptions({
    queryKey: camposKeys.lista(bloqueId),
    queryFn: ({ signal }) => listarCampos(bloqueId, signal),
  });

// No usan suspense: el constructor muestra la carga y el error dentro de cada panel.
export function useBloques(proyectoId: string) {
  return useQuery(bloquesQuery(proyectoId));
}

export function useCampos(bloqueId: string) {
  return useQuery(camposQuery(bloqueId));
}
