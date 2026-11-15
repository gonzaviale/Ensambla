import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  crearObjetivo,
  decidirPaso,
  eliminarObjetivo,
  guardarObjetivoConSami,
  proponerObjetivo,
  responderMiniObjetivo,
} from '@/api/objetivos';
import { useUsuarioOpcional } from '@/hooks/auth/queries';
import type {
  CuotaObjetivos,
  DecisionDePaso,
  MensajeDeConversacion,
  ObjetivoConSami,
  ObjetivoDelServidor,
  RespuestaDeMini,
} from '@/types/objetivos';
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

const claveDeLaCuota = (usuarioId: string) => [...objetivosKey, 'cuota', usuarioId] as const;

/** Sami lee lo que la persona escribió y propone mini-objetivos. No guarda nada: sólo al guardar se gasta cuota. */
export function useProponerObjetivo() {
  const queryClient = useQueryClient();
  const usuario = useUsuarioOpcional();
  return useMutation({
    mutationFn: (mensajes: MensajeDeConversacion[]) => proponerObjetivo(mensajes),
    onSuccess: ({ cuota }) => {
      if (cuota && usuario)
        queryClient.setQueryData<CuotaObjetivos>(claveDeLaCuota(usuario.id), cuota);
    },
  });
}

/** Guarda lo que la persona aceptó (o el seguimiento manual) y vuelve a preguntar cuántos objetivos con IA le quedan. */
export function useGuardarObjetivoConSami() {
  const queryClient = useQueryClient();
  const clave = useClaveDeObjetivos();
  const usuario = useUsuarioOpcional();
  return useMutation({
    mutationFn: (objetivo: ObjetivoConSami) => guardarObjetivoConSami(objetivo),
    onSuccess: (creado) => {
      queryClient.setQueryData<ObjetivoDelServidor[]>(clave, (actuales) => [
        ...(actuales ?? []),
        creado,
      ]);
      void queryClient.invalidateQueries({ queryKey: claveDeLaCuota(usuario?.id ?? 'sin-sesion') });
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

/**
 * La persona le cuenta a Sami cómo le fue con un manual. Se vuelve a pedir lo del objetivo y los pendientes: si el
 * servidor ya tenía una respuesta (409), la primera queda y la pantalla deja de pedirla.
 */
export function useResponderMiniObjetivo(objetivoId: string) {
  const queryClient = useQueryClient();
  const usuario = useUsuarioOpcional();
  const actualizar = () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: objetivosKeys.detalle(objetivoId) }),
      queryClient.invalidateQueries({
        queryKey: [...objetivosKey, 'pendientes', usuario?.id ?? 'sin-sesion'],
      }),
    ]);
  return useMutation({
    mutationFn: ({ miniId, respuesta }: { miniId: string; respuesta: RespuestaDeMini }) =>
      responderMiniObjetivo(objetivoId, miniId, respuesta),
    onSuccess: actualizar,
    onError: actualizar,
  });
}
