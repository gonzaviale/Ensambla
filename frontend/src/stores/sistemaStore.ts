import { create } from 'zustand';
import { clone } from '@/lib/sistema/core';
import type { Bloque, Seccion, Sistema } from '@/lib/sistema/tipos';

// El estado del taller y del editor: los sistemas de la persona y lo que está haciendo con ellos. Es el
// equivalente a las variables de módulo de app.js del mockup; ahora los componentes lo leen del store y
// los cambios viajan a la API por un hook aparte (useSincronizarSistemas).

export type ModoDelEditor = 'build' | 'use';

const MAX_HISTORIAL = 30;

type EstadoDelStore = {
  usuarioId: string | null;
  sistemas: Sistema[];
  /** Ids de los sistemas con cambios todavía sin guardar en el servidor. */
  pendientes: string[];
  /** Ids de los sistemas que hay que borrar en el servidor. */
  eliminados: string[];

  activeId: string | null;
  sectionId: string | null;
  selectedId: string | null;
  modo: ModoDelEditor;
  /** Copias anteriores del sistema activo, para deshacer. */
  historial: Sistema[];
  /** Recién creado: se muestra el cartel «tu sistema está listo». */
  listo: boolean;
  aviso: { id: number; texto: string } | null;
};

type Acciones = {
  /** Carga lo que dice el servidor. Si es la misma persona que ya estaba, no pisa lo que editó. */
  hidratar: (usuarioId: string, sistemas: Sistema[]) => void;
  reiniciar: () => void;
  abrirSistema: (id: string, modo?: ModoDelEditor, esNuevo?: boolean) => void;
  agregarSistema: (sistema: Sistema) => void;
  eliminarSistema: (id: string) => void;
  /** Aplica un cambio sobre una copia del sistema activo y lo marca para guardar. */
  commit: <T>(cambio: (sistema: Sistema) => T) => T | undefined;
  /** Cambia un sistema sin pasar por el historial (por ejemplo al renombrar desde la lista). */
  actualizarSistema: (id: string, cambio: (sistema: Sistema) => void) => void;
  deshacer: () => boolean;
  seleccionar: (bloqueId: string | null) => void;
  elegirSeccion: (seccionId: string) => void;
  cambiarModo: (modo: ModoDelEditor) => void;
  descartarCartelListo: () => void;
  marcarGuardados: (ids: string[], eliminados: string[]) => void;
  avisar: (texto: string) => void;
  cerrarAviso: () => void;
};

export type SistemaStore = EstadoDelStore & Acciones;

const ESTADO_INICIAL: EstadoDelStore = {
  usuarioId: null,
  sistemas: [],
  pendientes: [],
  eliminados: [],
  activeId: null,
  sectionId: null,
  selectedId: null,
  modo: 'build',
  historial: [],
  listo: false,
  aviso: null,
};

let contadorDeAvisos = 0;

const sinRepetir = (ids: string[], id: string) => (ids.includes(id) ? ids : [...ids, id]);

export const useSistemaStore = create<SistemaStore>()((set, get) => ({
  ...ESTADO_INICIAL,

  hidratar: (usuarioId, sistemas) => {
    if (get().usuarioId === usuarioId) return;
    set({ ...ESTADO_INICIAL, usuarioId, sistemas });
  },

  reiniciar: () => set({ ...ESTADO_INICIAL }),

  abrirSistema: (id, modo = 'use', esNuevo = false) => {
    const sistema = get().sistemas.find((item) => item.id === id);
    set({
      activeId: id,
      sectionId: sistema?.sections[0]?.id ?? null,
      selectedId: null,
      modo,
      historial: [],
      listo: esNuevo,
    });
  },

  agregarSistema: (sistema) =>
    set((estado) => ({
      sistemas: [...estado.sistemas, sistema],
      pendientes: sinRepetir(estado.pendientes, sistema.id),
    })),

  eliminarSistema: (id) =>
    set((estado) => ({
      sistemas: estado.sistemas.filter((item) => item.id !== id),
      pendientes: estado.pendientes.filter((pendiente) => pendiente !== id),
      eliminados: sinRepetir(estado.eliminados, id),
      ...(estado.activeId === id
        ? { activeId: null, sectionId: null, selectedId: null, historial: [] }
        : {}),
    })),

  commit: (cambio) => {
    const { activeId, sistemas, historial, pendientes } = get();
    const actual = sistemas.find((item) => item.id === activeId);
    if (!actual) return undefined;
    const antes = clone(actual);
    const despues = clone(actual);
    const resultado = cambio(despues);
    despues.updatedAt = new Date().toISOString();
    set({
      sistemas: sistemas.map((item) => (item.id === despues.id ? despues : item)),
      historial: [...historial, antes].slice(-MAX_HISTORIAL),
      pendientes: sinRepetir(pendientes, despues.id),
    });
    return resultado;
  },

  actualizarSistema: (id, cambio) =>
    set((estado) => {
      const actual = estado.sistemas.find((item) => item.id === id);
      if (!actual) return estado;
      const despues = clone(actual);
      cambio(despues);
      despues.updatedAt = new Date().toISOString();
      return {
        sistemas: estado.sistemas.map((item) => (item.id === id ? despues : item)),
        pendientes: sinRepetir(estado.pendientes, id),
      };
    }),

  deshacer: () => {
    const { historial, sistemas, pendientes, sectionId } = get();
    const anterior = historial.at(-1);
    if (!anterior) return false;
    set({
      sistemas: sistemas.map((item) => (item.id === anterior.id ? anterior : item)),
      historial: historial.slice(0, -1),
      pendientes: sinRepetir(pendientes, anterior.id),
      // Si la sección elegida ya no existe en la copia anterior, se vuelve a la primera.
      sectionId: anterior.sections.some((seccion) => seccion.id === sectionId)
        ? sectionId
        : (anterior.sections[0]?.id ?? null),
      selectedId: null,
    });
    return true;
  },

  seleccionar: (bloqueId) => set({ selectedId: bloqueId }),
  elegirSeccion: (seccionId) => set({ sectionId: seccionId, selectedId: null }),
  cambiarModo: (modo) => set({ modo, selectedId: null }),
  descartarCartelListo: () => set({ listo: false }),

  marcarGuardados: (ids, eliminados) =>
    set((estado) => ({
      pendientes: estado.pendientes.filter((id) => !ids.includes(id)),
      eliminados: estado.eliminados.filter((id) => !eliminados.includes(id)),
    })),

  avisar: (texto) => set({ aviso: { id: ++contadorDeAvisos, texto } }),
  cerrarAviso: () => set({ aviso: null }),
}));

/* Lectores: lo que el mockup calculaba con system(), section() y block(). */

export const seleccionarSistemaActivo = (estado: SistemaStore): Sistema | undefined =>
  estado.sistemas.find((item) => item.id === estado.activeId);

export const seleccionarSeccionActiva = (estado: SistemaStore): Seccion | undefined => {
  const sistema = seleccionarSistemaActivo(estado);
  return sistema?.sections.find((item) => item.id === estado.sectionId) ?? sistema?.sections[0];
};

export const seleccionarBloqueElegido = (estado: SistemaStore): Bloque | undefined =>
  seleccionarSistemaActivo(estado)
    ?.sections.flatMap((seccion) => seccion.blocks)
    .find((bloque) => bloque.id === estado.selectedId);
