import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';
import { nuevoObjetivo, objetivoDeEjemplo, type Objetivo } from '@/lib/objetivos';

// Los objetivos de la persona. Todavía no existen en el backend: se guardan en el navegador, como la base
// simulada, para que sobrevivan a recargar la página.

type EstadoDeObjetivos = {
  objetivos: Objetivo[];
  /** La demo cargada siembra un objetivo de ejemplo una sola vez, aunque después se borre. */
  ejemploSembrado: boolean;
  definir: (texto: string) => Objetivo;
  eliminar: (id: string) => void;
  sembrarEjemplo: () => void;
};

export const useObjetivosStore = create<EstadoDeObjetivos>()(
  persist(
    (set, get) => ({
      objetivos: [],
      ejemploSembrado: false,
      definir: (texto) => {
        const objetivo = nuevoObjetivo(texto);
        set({ objetivos: [...get().objetivos, objetivo] });
        return objetivo;
      },
      eliminar: (id) =>
        set({ objetivos: get().objetivos.filter((objetivo) => objetivo.id !== id) }),
      sembrarEjemplo: () => {
        if (get().ejemploSembrado) return;
        set({ objetivos: [objetivoDeEjemplo(), ...get().objetivos], ejemploSembrado: true });
      },
    }),
    {
      name: 'ensambla.objetivos.v1',
      storage: createJSONStorage(() => localStorage),
      partialize: ({ objetivos, ejemploSembrado }) => ({ objetivos, ejemploSembrado }),
    },
  ),
);
