import { useMutation } from '@tanstack/react-query';
import { useRouter } from '@tanstack/react-router';
import { guardarNegocioActivo } from '@/lib/negocios/lista';
import { makeSystem } from '@/lib/sistema/core';
import { useSistemaStore } from '@/stores/sistemaStore';

// Cada negocio es un cuaderno, así que cambiar, agregar o eliminar un negocio es elegir, crear o borrar un cuaderno
// en la copia de la pantalla: `useSincronizarSistemas` lo lleva después al servidor, como cualquier otro cambio.

function useVolverAlInicio() {
  const router = useRouter();
  return () => router.navigate({ to: '/inicio' });
}

/** Elegir el negocio después de ingresar (T-204): todavía no se entró a la app, alcanza con recordar cuál es. */
export function useElegirNegocioAlEntrar() {
  return useMutation({
    mutationFn: async (negocioId: string) => {
      guardarNegocioActivo(negocioId);
      useSistemaStore.getState().activarNegocio(negocioId);
    },
  });
}

export function useActivarNegocio() {
  const volverAlInicio = useVolverAlInicio();
  return useMutation({
    mutationFn: async (negocioId: string) => {
      useSistemaStore.getState().activarNegocio(negocioId);
      await volverAlInicio();
    },
  });
}

/** Un negocio nuevo arranca como un cuaderno vacío con ese nombre, y se pasa a trabajar con él. */
export function useCrearNegocio() {
  const volverAlInicio = useVolverAlInicio();
  return useMutation({
    mutationFn: async (nombre: string) => {
      useSistemaStore.getState().agregarSistema(makeSystem(nombre, [], false));
      await volverAlInicio();
    },
  });
}

export function useEliminarNegocio() {
  const volverAlInicio = useVolverAlInicio();
  return useMutation({
    mutationFn: async (negocioId: string) => {
      useSistemaStore.getState().eliminarSistema(negocioId);
      await volverAlInicio();
    },
  });
}
