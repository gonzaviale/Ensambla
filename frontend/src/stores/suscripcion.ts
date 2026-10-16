import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';
import {
  cancelarSuscripcion,
  iniciarPrueba,
  reactivarSuscripcion,
  simularFinDePrueba,
  suscribirse,
  suscripcionInicial,
  type DatosSuscripcion,
} from '@/lib/planes/suscripcion';
import type { Suscripcion } from '@/types/negocio';

// Suscripción simulada: vive en el navegador (localStorage). No hay backend de planes todavía.

type Estado = {
  suscripcion: Suscripcion;
  iniciarPrueba: () => void;
  suscribirse: (datos: DatosSuscripcion) => void;
  cancelarSuscripcion: () => void;
  reactivarSuscripcion: () => void;
  simularFinDePrueba: () => void;
  reiniciar: () => void;
};

export const useSuscripcionStore = create<Estado>()(
  persist(
    (set) => ({
      suscripcion: suscripcionInicial(),
      iniciarPrueba: () => set((s) => ({ suscripcion: iniciarPrueba(s.suscripcion) })),
      suscribirse: (datos) => set((s) => ({ suscripcion: suscribirse(s.suscripcion, datos) })),
      cancelarSuscripcion: () => set((s) => ({ suscripcion: cancelarSuscripcion(s.suscripcion) })),
      reactivarSuscripcion: () =>
        set((s) => ({ suscripcion: reactivarSuscripcion(s.suscripcion) })),
      simularFinDePrueba: () => set((s) => ({ suscripcion: simularFinDePrueba(s.suscripcion) })),
      reiniciar: () => set({ suscripcion: suscripcionInicial() }),
    }),
    {
      name: 'ensambla-suscripcion-demo-v1',
      version: 1,
      storage: createJSONStorage(() => localStorage),
      partialize: (s) => ({ suscripcion: s.suscripcion }),
    },
  ),
);

/** Para las pruebas: vuelve a la prueba en curso. */
export function reiniciarSuscripcion() {
  useSuscripcionStore.setState({ suscripcion: suscripcionInicial() });
}
