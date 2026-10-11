import { useMutation, useQueryClient, type QueryClient } from '@tanstack/react-query';
import { crearBloque, eliminarBloque, renombrarBloque } from '@/api/bloques';
import { crearCampo, editarCampo, eliminarCampo } from '@/api/campos';
import type { GuardarCampo } from '@/types/campo';
import { proyectosKeys } from '../proyectos/queries';
import { registrosKeys } from '../registros/queries';
import { bloquesKeys, camposKeys } from './queries';

// La estructura también llega en GET /proyectos/{id} (modo usar): se refresca junto con la lista.
function refrescarEstructura(queryClient: QueryClient, proyectoId: string, bloqueId?: string) {
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: bloquesKeys.lista(proyectoId) }),
    queryClient.invalidateQueries({ queryKey: proyectosKeys.detalle(proyectoId), exact: true }),
    bloqueId && queryClient.invalidateQueries({ queryKey: camposKeys.lista(bloqueId) }),
    bloqueId && queryClient.invalidateQueries({ queryKey: registrosKeys.bloque(bloqueId) }),
  ]);
}

export function useCrearBloque(proyectoId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (nombre: string) => crearBloque({ proyectoId, nombre }),
    onSuccess: () => refrescarEstructura(queryClient, proyectoId),
  });
}

export function useRenombrarBloque(proyectoId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: renombrarBloque,
    onSuccess: () => refrescarEstructura(queryClient, proyectoId),
  });
}

export function useEliminarBloque(proyectoId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: eliminarBloque,
    onSuccess: async (_, bloqueId) => {
      queryClient.removeQueries({ queryKey: camposKeys.lista(bloqueId) });
      queryClient.removeQueries({ queryKey: registrosKeys.bloque(bloqueId) });
      await refrescarEstructura(queryClient, proyectoId);
    },
  });
}

export function useCrearCampo(proyectoId: string, bloqueId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (datos: GuardarCampo) => crearCampo({ bloqueId, datos }),
    onSuccess: () => refrescarEstructura(queryClient, proyectoId, bloqueId),
  });
}

export function useEditarCampo(proyectoId: string, bloqueId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: editarCampo,
    onSuccess: () => refrescarEstructura(queryClient, proyectoId, bloqueId),
  });
}

export function useEliminarCampo(proyectoId: string, bloqueId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: eliminarCampo,
    onSuccess: () => refrescarEstructura(queryClient, proyectoId, bloqueId),
  });
}
