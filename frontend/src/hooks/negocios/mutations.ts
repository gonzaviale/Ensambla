import { useMutation, useQueryClient, type QueryClient } from '@tanstack/react-query';
import { useRouter } from '@tanstack/react-router';
import { activarNegocio, crearNegocio, eliminarNegocio, listarNegocios } from '@/api/negocios';
import { useSistemaStore } from '@/stores/sistemaStore';
import type { NuevoNegocio } from '@/types/negocios';
import { negociosKeys } from './queries';

// Al cambiar de negocio ningún dato del anterior puede quedar visible: la sesión se conserva y todo lo demás
// se vuelve a pedir. La lista ya actualizada también se conserva para que la barra no parpadee.
function prepararCambio(queryClient: QueryClient) {
  useSistemaStore.getState().reiniciar();
  queryClient.removeQueries({
    predicate: (query) => query.queryKey[0] !== 'auth' && query.queryKey[0] !== negociosKeys.all[0],
  });
  // El resumen conjunto marca cuál es el activo.
  void queryClient.invalidateQueries({ queryKey: negociosKeys.resumen });
}

function useRecargarNegocio() {
  const queryClient = useQueryClient();
  const router = useRouter();

  return async () => {
    prepararCambio(queryClient);
    await router.navigate({ to: '/inicio' });
    await router.invalidate();
  };
}

// Elegir el negocio después de ingresar (T-204): todavía no hay datos de ningún negocio cargados, así que alcanza
// con poner al día lo que es de la cuenta; quien llama decide a dónde se entra.
export function useElegirNegocioAlEntrar() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: activarNegocio,
    onSuccess: (lista) => {
      queryClient.setQueryData(negociosKeys.lista, lista);
      void queryClient.invalidateQueries({ queryKey: negociosKeys.resumen });
    },
  });
}

export function useActivarNegocio() {
  const queryClient = useQueryClient();
  const recargar = useRecargarNegocio();
  return useMutation({
    mutationFn: activarNegocio,
    onSuccess: async (lista) => {
      queryClient.setQueryData(negociosKeys.lista, lista);
      await recargar();
    },
  });
}

export function useCrearNegocio() {
  const queryClient = useQueryClient();
  const recargar = useRecargarNegocio();
  return useMutation({
    mutationFn: async (datos: NuevoNegocio) => {
      const creado = await crearNegocio(datos);
      return activarNegocio(creado.id);
    },
    onSuccess: async (lista) => {
      queryClient.setQueryData(negociosKeys.lista, lista);
      await recargar();
    },
  });
}

export function useEliminarNegocio() {
  const queryClient = useQueryClient();
  const recargar = useRecargarNegocio();
  return useMutation({
    mutationFn: async (id: string) => {
      await eliminarNegocio(id);
      return listarNegocios();
    },
    onSuccess: async (lista) => {
      queryClient.setQueryData(negociosKeys.lista, lista);
      await recargar();
    },
  });
}
