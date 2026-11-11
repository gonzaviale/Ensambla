import { queryOptions, useQuery } from '@tanstack/react-query';
import { listarOrdenes } from '@/api/pedidos';
import { useUsuarioOpcional } from '@/hooks/auth/queries';
import type { OrdenDelServidor } from '@/types/pedidos';

export const pedidosKey = ['pedidos'] as const;

// Cada cuenta ve las suyas: la clave lleva a la persona para no mostrarle las de otra sesión. Se vuelve a preguntar
// cada tanto porque las órdenes nuevas llegan por WhatsApp, sin que el dueño haga nada en la app.
export const pedidosQuery = (usuarioId: string) =>
  queryOptions({
    queryKey: [...pedidosKey, usuarioId],
    queryFn: ({ signal }) => listarOrdenes(signal),
    retry: false,
    refetchInterval: 30_000,
  });

const SIN_PEDIDOS: OrdenDelServidor[] = [];

/** Las órdenes del negocio, las más nuevas primero. Vacío mientras carga o si el servidor falla. */
export function usePedidos() {
  const usuario = useUsuarioOpcional();
  const consulta = useQuery({ ...pedidosQuery(usuario?.id ?? 'sin-sesion'), enabled: !!usuario });
  return {
    pedidos: consulta.data ?? SIN_PEDIDOS,
    cargando: consulta.isPending,
    fallo: consulta.isError,
  };
}
