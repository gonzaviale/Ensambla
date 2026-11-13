import { useMutation, useQueryClient } from '@tanstack/react-query';
import { crearObjetivo, decidirPaso, eliminarObjetivo } from '@/api/objetivos';
import { useUsuarioOpcional } from '@/hooks/auth/queries';
import type { DecisionDePaso, ObjetivoDelServidor } from '@/types/objetivos';
import { objetivosKey, objetivosKeys } from './queries';

function useClaveDeObjetivos() {
  const usuario = useUsuarioOpcional();
  return [...objetivosKey, usuario?.id ?? 'sin-sesion'] as const;
}

/** Sami convierte lo que la persona escribió en una meta medible y el servidor lo guarda en su negocio. */
export function useDefinirObjetivo() {
  const queryClient = useQueryClient();
  const clave = useClaveDeObjetivos();
  return useMutation({
    mutationFn: ({ texto, meta }: { texto: string; meta: number }) =>
      crearObjetivo({ texto: texto.trim(), meta }),
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

// Decidir sobre un mini-objetivo cambia los minis, el progreso y las decisiones: se vuelve a pedir
// todo lo del objetivo.
function useAlCambiarElObjetivo(objetivoId: string) {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: objetivosKeys.detalle(objetivoId) });
}

export function useDecidirPaso(objetivoId: string) {
  const alCambiar = useAlCambiarElObjetivo(objetivoId);
  return useMutation({
    mutationFn: ({ miniId, tipo }: { miniId: string; tipo: DecisionDePaso }) =>
      decidirPaso(objetivoId, miniId, tipo),
    onSettled: alCambiar,
  });
}
