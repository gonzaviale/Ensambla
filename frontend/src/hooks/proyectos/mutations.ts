import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  crearProyecto,
  crearProyectoDesdePlantilla,
  eliminarProyecto,
  renombrarProyecto,
} from '@/api/proyectos';
import type { Proyecto } from '@/types/proyecto';
import { proyectosKeys } from './queries';

function useProyectoGuardado() {
  const queryClient = useQueryClient();
  return async (proyecto: Proyecto) => {
    queryClient.setQueryData(proyectosKeys.detalle(proyecto.id), proyecto);
    await queryClient.invalidateQueries({ queryKey: proyectosKeys.all, exact: true });
  };
}
export function useCrearProyecto() {
  return useMutation({ mutationFn: crearProyecto, onSuccess: useProyectoGuardado() });
}
export function useCrearDesdePlantilla() {
  return useMutation({ mutationFn: crearProyectoDesdePlantilla, onSuccess: useProyectoGuardado() });
}
export function useRenombrarProyecto() {
  return useMutation({ mutationFn: renombrarProyecto, onSuccess: useProyectoGuardado() });
}
export function useEliminarProyecto() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: eliminarProyecto,
    onSuccess: async (_, id) => {
      queryClient.removeQueries({ queryKey: proyectosKeys.detalle(id), exact: true });
      await queryClient.invalidateQueries({ queryKey: proyectosKeys.all, exact: true });
    },
  });
}
