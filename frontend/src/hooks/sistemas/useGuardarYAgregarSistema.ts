import { guardarSistema } from '@/api/sistemas';
import { useQueryClient } from '@tanstack/react-query';
import { objetivosKeys } from '@/hooks/objetivos/queries';
import type { Sistema } from '@/lib/sistema/tipos';
import { useSistemaStore } from '@/stores/sistemaStore';

/** Guarda un sistema nuevo en el servidor (que completa su estructura fija) y lo suma a la copia de la pantalla. */
export function useGuardarYAgregarSistema() {
  const queryClient = useQueryClient();

  return async (sistema: Sistema) => {
    const guardado = await guardarSistema(sistema);
    useSistemaStore.getState().agregarSistema(guardado);
    useSistemaStore.getState().marcarGuardados([guardado.id], []);
    void queryClient.invalidateQueries({ queryKey: objetivosKeys.pasos });
    return guardado;
  };
}
