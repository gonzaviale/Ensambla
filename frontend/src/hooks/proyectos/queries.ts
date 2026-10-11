import { queryOptions, useQueries, useQuery, useSuspenseQuery } from '@tanstack/react-query';
import { listarProyectos, obtenerProyecto } from '@/api/proyectos';
import type { Bloque } from '@/types/proyecto';

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
// La lista de proyectos sin suspender la pantalla que la usa (por ejemplo, para elegir un destino).
export function useProyectosDisponibles() {
  return useQuery(proyectosQuery());
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

/**
 * Estructura de varios proyectos en una sola pasada (un `useQueries` para toda la lista), para que
 * Mis sistemas muestre los resúmenes sin que cada tarjeta dispare y reaccione a su propia petición.
 * Devuelve un mapa id → resultado; comparte la caché de `proyectoQuery`.
 */
export function useEstructuras(ids: string[]) {
  return useQueries({
    queries: ids.map((id) => proyectoQuery(id)),
    combine: (resultados) =>
      Object.fromEntries(
        resultados.map((resultado, i) => [
          ids[i] as string,
          {
            bloques: resultado.data?.bloques,
            cargando: resultado.isPending,
            fallo: resultado.isError,
          },
        ]),
      ) as Record<string, { bloques: Bloque[] | undefined; cargando: boolean; fallo: boolean }>,
  });
}
