import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';
import { TOTAL_PASOS } from '@/lib/recorrido/pasos';

// Estado del recorrido guiado. Se guarda en el navegador para que el comercio no vea la oferta
// de nuevo y para retomar el recorrido si recarga la pantalla a la mitad.

type EstadoRecorrido = {
  /** Ya lo terminó, lo saltó o dijo «Ahora no»: no se vuelve a ofrecer solo. */
  visto: boolean;
  /** Hay un recorrido en curso. */
  activo: boolean;
  /** Paso actual (0-based). */
  paso: number;
};

type AccionesRecorrido = {
  iniciar: () => void;
  siguiente: () => void;
  anterior: () => void;
  /** Termina o salta el recorrido: lo da por visto. */
  cerrar: () => void;
  /** «Ahora no» en la oferta: la oculta y no la vuelve a ofrecer. */
  descartarOferta: () => void;
};

export type RecorridoStore = EstadoRecorrido & AccionesRecorrido;

const CLAVE = 'ensambla-recorrido-v1';

const limitar = (paso: number) => Math.min(Math.max(paso, 0), TOTAL_PASOS - 1);

export const useRecorridoStore = create<RecorridoStore>()(
  persist(
    (set) => ({
      visto: false,
      activo: false,
      paso: 0,
      iniciar: () => set({ activo: true, paso: 0 }),
      siguiente: () =>
        set((s) =>
          s.paso >= TOTAL_PASOS - 1
            ? { activo: false, visto: true, paso: 0 }
            : { paso: limitar(s.paso + 1) },
        ),
      anterior: () => set((s) => ({ paso: limitar(s.paso - 1) })),
      cerrar: () => set({ activo: false, visto: true, paso: 0 }),
      descartarOferta: () => set({ visto: true }),
    }),
    {
      name: CLAVE,
      version: 1,
      storage: createJSONStorage(() => localStorage),
      // Sólo se guardan los datos, no las funciones.
      partialize: (s): EstadoRecorrido => ({ visto: s.visto, activo: s.activo, paso: s.paso }),
      // Un paso guardado que ya no existe (la lista cambió) vuelve al principio.
      merge: (guardado, actual) => {
        const datos = (guardado ?? {}) as Partial<EstadoRecorrido>;
        return {
          ...actual,
          visto: datos.visto === true,
          activo: datos.activo === true,
          paso: limitar(typeof datos.paso === 'number' ? datos.paso : 0),
        };
      },
    },
  ),
);

/** Para las pruebas: deja el recorrido «visto» (por defecto) o como la primera vez. */
export function reiniciarRecorrido(visto = true) {
  useRecorridoStore.setState({ visto, activo: false, paso: 0 });
}
