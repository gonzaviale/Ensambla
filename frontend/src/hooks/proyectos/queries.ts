import { queryOptions, useQuery, useSuspenseQuery } from '@tanstack/react-query';
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
/**
 * Estructura (bloques y datos) de un proyecto para su tarjeta en Mis proyectos, sin suspender la
 * lista. GET /proyectos no la trae (el backend sólo la arma en GET /proyectos/{id}); se pide una vez
 * por proyecto y queda en la misma caché que usan Usar, Construir y los breadcrumbs al abrirlo.
 */
export function useEstructuraProyecto(id: string) {
  return useQuery(proyectoQuery(id));
}
