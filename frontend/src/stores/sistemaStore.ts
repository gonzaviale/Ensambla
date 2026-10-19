import { create } from 'zustand';
import { clone } from '@/lib/sistema/core';
import type { Aviso, Respuesta } from '@/lib/sistema/alertas';
import type { Bloque, ClavePlantilla, Seccion, Sistema } from '@/lib/sistema/tipos';
import { TUTORIAL_INICIAL, type EstadoDelTutorial } from '@/lib/sistema/tutorial';

// El estado del taller y del editor: los sistemas de la persona y lo que está haciendo con ellos. Es el
// equivalente a las variables de módulo de app.js del mockup; ahora los componentes lo leen del store y
// los cambios viajan a la API por un hook aparte (useSincronizarSistemas).

export type ModoDelEditor = 'build' | 'use';

export type Modal =
  | { tipo: 'blanco' }
  | { tipo: 'plantilla'; clave: ClavePlantilla }
  | { tipo: 'ayuda'; respuesta?: string }
  | { tipo: 'perfil' }
  | { tipo: 'renombrar' }
  | { tipo: 'renombrarPieza'; bloqueId: string }
  | { tipo: 'registro'; bloqueId: string; filaId?: string | null }
  | { tipo: 'campo'; bloqueId: string; clave?: string | null }
  | { tipo: 'bienvenida' }
  | { tipo: 'secciones' }
  | { tipo: 'agregarSeccion' }
  | { tipo: 'elegirBloque' }
  | {
      tipo: 'importar';
      /** El sistema al que se llevan los datos; sin sistema se crea uno nuevo. */
      sistemaId: string | null;
      bloqueId: string | null;
      /** `negocio`: la foto sirve para contarle a Sami cómo trabaja la persona. */
      contexto: 'registros' | 'negocio';
    }
  | {
      tipo: 'confirmar';
      titulo: string;
      mensaje: string;
      etiqueta: string;
      onConfirmar: () => void;
    };

export type MensajeDelTaller = { quien: 'sami' | 'you'; texto: string };

/** La conversación con Sami y la propuesta que va tomando forma (el `wizard` del mockup). */
export type Taller = {
  /** Distingue una conversación de otra: la respuesta tardía de una vieja se descarta. */
  id: number;
  /** El nombre que se eligió con un ejemplo, si se eligió alguno. */
  nombre: string;
  entrada: string;
  actividad: '' | 'warehouse' | 'orders' | 'shop';
  borrador: Sistema | null;
  seccionId: string | null;
  pensando: boolean;
  mensajes: MensajeDelTaller[];
};

/** El panel de avisos de Sami y lo que se ve de él. */
export type PanelDeAvisos = {
  abierto: boolean;
  filtro: string;
  respuesta: Respuesta | null;
  /** El aviso que se abrió desde el toast o la franja: el panel lo muestra resaltado. */
  foco: string | null;
};

const PANEL_CERRADO: PanelDeAvisos = {
  abierto: false,
  filtro: 'todas',
  respuesta: null,
  foco: null,
};

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
  modal: Modal | null;
  taller: Taller | null;
  /** Los avisos de Sami que la persona todavía no descartó. Los calcula el motor de avisos. */
  avisos: Aviso[];
  panel: PanelDeAvisos;
  /** «Sami vio un cambio»: el aviso nuevo que se muestra un rato arriba a la derecha. */
  avisoNuevo: { aviso: Aviso; cantidad: number } | null;
  /** El tutorial interactivo: en qué paso va y con qué está practicando la persona. */
  tutorial: EstadoDelTutorial;
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
  abrirModal: (modal: Modal) => void;
  cerrarModal: () => void;
  iniciarTaller: (saludo: string) => void;
  /** Cambia la conversación actual; si no hay, no hace nada. */
  actualizarTaller: (cambio: Partial<Taller> | ((taller: Taller) => Partial<Taller>)) => void;
  /** Aplica un cambio sobre una copia de la propuesta de Sami. */
  editarBorrador: (cambio: (borrador: Sistema) => void) => void;
  ponerAvisos: (avisos: Aviso[]) => void;
  abrirPanel: (foco?: string | null) => void;
  cerrarPanel: () => void;
  actualizarPanel: (cambio: Partial<PanelDeAvisos>) => void;
  mostrarAvisoNuevo: (aviso: Aviso, cantidad: number) => void;
  ocultarAvisoNuevo: () => void;
  /** Cambia lo que el tutorial recuerda (el paso, el bloque, etc.). */
  ponerTutorial: (cambio: Partial<EstadoDelTutorial>) => void;
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
  modal: null,
  taller: null,
  avisos: [],
  panel: PANEL_CERRADO,
  avisoNuevo: null,
  tutorial: TUTORIAL_INICIAL,
};

let contadorDeAvisos = 0;
let contadorDeTalleres = 0;

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
  abrirModal: (modal) => set({ modal }),
  cerrarModal: () => set({ modal: null }),

  iniciarTaller: (saludo) =>
    set({
      taller: {
        id: ++contadorDeTalleres,
        nombre: '',
        entrada: '',
        actividad: '',
        borrador: null,
        seccionId: null,
        pensando: false,
        mensajes: [{ quien: 'sami', texto: saludo }],
      },
    }),

  actualizarTaller: (cambio) =>
    set((estado) => {
      if (!estado.taller) return estado;
      const parcial = typeof cambio === 'function' ? cambio(estado.taller) : cambio;
      return { taller: { ...estado.taller, ...parcial } };
    }),

  ponerAvisos: (avisos) => set({ avisos }),
  abrirPanel: (foco = null) =>
    set((estado) => ({
      panel: {
        abierto: true,
        filtro: foco ? 'todas' : estado.panel.filtro,
        respuesta: null,
        foco,
      },
      avisoNuevo: null,
    })),
  cerrarPanel: () => set((estado) => ({ panel: { ...estado.panel, abierto: false } })),
  actualizarPanel: (cambio) => set((estado) => ({ panel: { ...estado.panel, ...cambio } })),
  mostrarAvisoNuevo: (aviso, cantidad) => set({ avisoNuevo: { aviso, cantidad } }),
  ocultarAvisoNuevo: () => set({ avisoNuevo: null }),
  ponerTutorial: (cambio) => set((estado) => ({ tutorial: { ...estado.tutorial, ...cambio } })),

  editarBorrador: (cambio) =>
    set((estado) => {
      const borrador = estado.taller?.borrador;
      if (!estado.taller || !borrador) return estado;
      const copia = clone(borrador);
      cambio(copia);
      return { taller: { ...estado.taller, borrador: copia } };
    }),
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
