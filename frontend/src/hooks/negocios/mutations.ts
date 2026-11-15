import { useMutation, useQueryClient, type QueryClient } from '@tanstack/react-query';
import { useRouter } from '@tanstack/react-router';
import { activarNegocio, crearNegocio, eliminarNegocio, listarNegocios } from '@/api/negocios';
import { eliminarSistema, guardarSistema } from '@/api/sistemas';
import { borrarBorradorSesion } from '@/lib/borradorSesion';
import { useSistemaStore } from '@/stores/sistemaStore';
import type { NuevoNegocio } from '@/types/negocios';
import { negociosKeys } from './queries';

// Al cambiar de negocio ningún dato del anterior puede quedar visible: la sesión se conserva y todo lo demás
// se vuelve a pedir. La lista ya actualizada también se conserva para que la barra no parpadee.
function prepararCambio(queryClient: QueryClient) {
  useSistemaStore.getState().reiniciar();
  // El borrador de la sesión es del negocio que se deja: si quedara, se mezclaría con el del negocio nuevo.
  borrarBorradorSesion();
  queryClient.removeQueries({
    predicate: (query) => query.queryKey[0] !== 'auth' && query.queryKey[0] !== negociosKeys.all[0],
  });
  // El resumen conjunto marca cuál es el activo.
  void queryClient.invalidateQueries({ queryKey: negociosKeys.resumen });
}

// Lo que se cambió y todavía no se guardó pertenece al negocio de ahora: se lleva al servidor antes de cambiar, mientras
// la sesión sigue siendo la de ese negocio. Si no se puede, el cambio no se hace y no se pierde nada.
async function volcarPendientes() {
  const { pendientes, eliminados, sistemas, marcarGuardados } = useSistemaStore.getState();
  const aGuardar = sistemas.filter((sistema) => pendientes.includes(sistema.id));
  if (aGuardar.length === 0 && eliminados.length === 0) return;
  await Promise.all([
    ...aGuardar.map((sistema) => guardarSistema(sistema)),
    ...eliminados.map((id) => eliminarSistema(id)),
  ]);
  marcarGuardados(
    aGuardar.map((sistema) => sistema.id),
    eliminados,
  );
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
    mutationFn: async (negocioId: string) => {
      await volcarPendientes();
      return activarNegocio(negocioId);
    },
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
      await volcarPendientes();
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
      await volcarPendientes();
      await eliminarNegocio(id);
      return listarNegocios();
    },
    onSuccess: async (lista) => {
      queryClient.setQueryData(negociosKeys.lista, lista);
      await recargar();
    },
  });
}
