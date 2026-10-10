import { queryOptions, useSuspenseQuery } from '@tanstack/react-query';
import { listarProyectos, obtenerProyecto } from '@/api/proyectos';

export const proyectosKeys = {
  all: ['proyectos'] as const,
  detalle: (id: string) => ['proyectos', id] as const,
};
export const proyectosQuery = () =>
  queryOptions({
    queryKey: proyectosKeys.all,
    queryFn: ({ signal }) => listarProyectos(signal),
  });
export const proyectoQuery = (id: string) =>
  queryOptions({
    queryKey: proyectosKeys.detalle(id),
    queryFn: ({ signal }) => obtenerProyecto(id, signal),
  });
export function useProyectos() {
  return useSuspenseQuery(proyectosQuery()).data;
}
export function useProyecto(id: string) {
  return useSuspenseQuery(proyectoQuery(id)).data;
}
