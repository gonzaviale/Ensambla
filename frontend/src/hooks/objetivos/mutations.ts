import { useMutation, useQueryClient } from '@tanstack/react-query';
import { crearObjetivo, eliminarObjetivo } from '@/api/objetivos';
import { useUsuarioOpcional } from '@/hooks/auth/queries';
import { metaDesdeTexto } from '@/lib/objetivos';
import type { ObjetivoDelServidor } from '@/types/objetivos';
import { objetivosKey } from './queries';

function useClaveDeObjetivos() {
  const usuario = useUsuarioOpcional();
  return [...objetivosKey, usuario?.id ?? 'sin-sesion'] as const;
}

/** Sami convierte lo que la persona escribió en una meta medible y el servidor lo guarda en su negocio. */
export function useDefinirObjetivo() {
  const queryClient = useQueryClient();
  const clave = useClaveDeObjetivos();
  return useMutation({
    mutationFn: (texto: string) =>
      crearObjetivo({ texto: texto.trim(), meta: metaDesdeTexto(texto) }),
    onSuccess: (creado) => {
      queryClient.setQueryData<ObjetivoDelServidor[]>(clave, (actuales) => [
        ...(actuales ?? []),
        creado,
      ]);
    },
  });
}

export function useEliminarObjetivo() {
  const queryClient = useQueryClient();
  const clave = useClaveDeObjetivos();
  return useMutation({
    mutationFn: eliminarObjetivo,
    onSuccess: (_resultado, id) => {
      queryClient.setQueryData<ObjetivoDelServidor[]>(clave, (actuales) =>
        actuales?.filter((objetivo) => objetivo.id !== id),
      );
    },
    // Si el servidor no lo tenía (404) o falló, se vuelve a preguntar para mostrar lo que hay de verdad.
    onError: () => queryClient.invalidateQueries({ queryKey: clave }),
  });
}
