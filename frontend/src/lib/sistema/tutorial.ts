import { allBlocks } from './core';
import type { Ancho, Bloque, Sistema } from './tipos';

// El tutorial interactivo: cada paso comprueba una acción real de la persona. Acá vive la parte pura (los
// pasos, sus textos y cuándo se pasa al siguiente); la interfaz que lo dibuja está en components/sistema/tutorial.

export type PasoDeTutorial =
  | 'choose'
  | 'describe'
  | 'review'
  | 'create'
  | 'pickTemplate'
  | 'useTemplate'
  | 'blankConfirm'
  | 'addBlock'
  | 'buildMode'
  | 'customize'
  | 'rename'
  | 'addSecond'
  | 'move'
  | 'openRecord'
  | 'fillRecord'
  | 'useMode'
  | 'done';

export type DetalleDePaso = {
  /** A qué etapa (de 5) pertenece; la barra de progreso la marca. */
  grupo: 0 | 1 | 2 | 3 | 4;
  title: string;
  text: string;
  hint: string;
};

export const PASOS: Record<PasoDeTutorial, DetalleDePaso> = {
  choose: {
    grupo: 0,
    title: 'Tres maneras de empezar.',
    text: 'Podés empezar con mi ayuda, elegir una plantilla o empezar desde cero. Elegí el camino que quieras probar.',
    hint: 'Tocá una de las tres opciones.',
  },
  describe: {
    grupo: 1,
    title: 'Contame lo que necesitás.',
    text: 'Comentame cómo te manejás, qué querés organizar o el negocio que tenés en mente, y te ayudo a crearlo.',
    hint: 'Escribí tu idea y enviá el mensaje.',
  },
  review: {
    grupo: 1,
    title: 'Tu idea ya tiene forma.',
    text: 'Esta es mi propuesta. Probá cambiar el ancho de una pieza con ½, ⅔ o 1. Así elegís cómo se reparte la pantalla.',
    hint: 'Elegí un ancho diferente en una pieza.',
  },
  create: {
    grupo: 1,
    title: 'Cuando te cierre, la creamos.',
    text: 'Esta propuesta todavía no es un sistema guardado. Podés seguir ajustándola o confirmar para empezar a usarla.',
    hint: 'Tocá “Sí, crear mi cuaderno”.',
  },
  pickTemplate: {
    grupo: 1,
    title: 'Una plantilla es un conjunto.',
    text: 'Cada base reúne varias piezas que trabajan juntas. Elegí una y abrí su vista previa para ver qué incluye.',
    hint: 'Abrí la plantilla que te sirva.',
  },
  useTemplate: {
    grupo: 1,
    title: 'Mirá las piezas antes de empezar.',
    text: 'Revisá los bloques y sus opciones. Cuando te sirva esta base, elegí empezar con este conjunto.',
    hint: 'Confirmá la plantilla para crear tu sistema.',
  },
  blankConfirm: {
    grupo: 1,
    title: 'Tu lienzo, desde cero.',
    text: 'Podés seguir con el espacio vacío o cambiar de camino. Para esta prueba, confirmá que querés empezar desde cero.',
    hint: 'Tocá “Seguir desde cero”.',
  },
  addBlock: {
    grupo: 1,
    title: 'Traé tu primera pieza.',
    text: 'Elegí un bloque del catálogo y arrastralo al lienzo. También podés tocar el +. Para empezar, Clientes o Inventario son buenas opciones.',
    hint: 'Agregá un bloque al lienzo.',
  },
  buildMode: {
    grupo: 2,
    title: 'Usar y construir, en el mismo lugar.',
    text: 'Entrá en Construir para cambiar las piezas, sus campos y la distribución. Tus datos se conservan al cambiar de modo.',
    hint: 'Tocá “Construir”.',
  },
  customize: {
    grupo: 2,
    title: 'De acá se cambia.',
    text: 'Abrí los ajustes de esta pieza. Podés cambiar su nombre, sus campos, su color y la sección donde aparece.',
    hint: 'Tocá el botón de ajustes del bloque.',
  },
  rename: {
    grupo: 2,
    title: 'Ponelo en tus palabras.',
    text: 'Cambiá el nombre de este bloque por uno que te resulte claro. Al salir del campo, el cambio se guarda solo.',
    hint: 'Escribí otro nombre y salí del campo con Tab o tocando afuera.',
  },
  addSecond: {
    grupo: 2,
    title: 'Sumemos otra pieza para moverlas.',
    text: 'Agregá un segundo bloque del catálogo. Con dos piezas vas a poder probar cómo cambiar su orden.',
    hint: 'Arrastrá otra pieza o tocá su +.',
  },
  move: {
    grupo: 2,
    title: 'De acá se mueve.',
    text: 'Arrastrá el bloque desde los seis puntitos o usá las flechas de subir y bajar. Cambialo de lugar para probar.',
    hint: 'Mové el bloque a otra posición.',
  },
  openRecord: {
    grupo: 3,
    title: 'De acá se llena.',
    text: 'El diseño está listo para empezar. Tocá Registrar en esta pieza para cargar tu primer dato.',
    hint: 'Abrí el formulario con “Registrar”.',
  },
  fillRecord: {
    grupo: 3,
    title: 'Cargá un dato tuyo.',
    text: 'Completá los campos obligatorios y guardá el registro. Si falta algo, el formulario te lo marca y podés corregirlo.',
    hint: 'El paso sigue cuando el dato queda guardado.',
  },
  useMode: {
    grupo: 3,
    title: 'Ahora, a usarlo.',
    text: 'Pasá a Usar cuaderno para trabajar con tus datos. Después podés volver a Construir cuando quieras cambiar algo.',
    hint: 'Tocá “Usar cuaderno”.',
  },
  done: {
    grupo: 4,
    title: 'Tu primer sistema ya es tuyo.',
    text: 'Elegiste una base, personalizaste una pieza, la moviste y cargaste un registro. Podés seguir explorando, sumar bloques o probar otro camino.',
    hint: 'Todo lo que hiciste quedó en esta demo.',
  },
};

/** Lo que cada paso deja tocar, por nombre de acción (como el `stepActions` del mockup). */
export const ACCIONES_DEL_PASO: Record<PasoDeTutorial, string[]> = {
  choose: ['guide', 'templates', 'blank'],
  describe: [],
  review: ['draft-width'],
  create: ['guide-create'],
  pickTemplate: ['preview-template', 'filter-template'],
  useTemplate: ['use-template', 'close-modal'],
  blankConfirm: ['confirm-blank', 'guide', 'templates', 'close-modal'],
  addBlock: ['add-block', 'mobile-library', 'close-library'],
  buildMode: ['mode'],
  customize: ['select-block'],
  rename: [],
  addSecond: ['add-block', 'mobile-library', 'close-library'],
  move: ['move-up', 'move-down', 'noop'],
  openRecord: ['add-record'],
  fillRecord: ['close-modal'],
  useMode: ['mode'],
  done: [],
};

export type EstadoDelTutorial = {
  activo: boolean;
  paso: PasoDeTutorial | null;
  rama: 'guide' | 'template' | 'blank' | null;
  /** El bloque con el que se está practicando. */
  bloqueId: string | null;
  sistemaId: string | null;
  /** Lo que había al entrar a un paso, para saber si la persona cambió algo. */
  anchos: Record<string, Ancho> | null;
  orden: string | null;
  nombre: string | null;
  filasIds: string[];
  /** Cuántos sistemas había al elegir un camino: si aparece uno nuevo, el camino se completó. */
  sistemasAlElegir: number;
};

export const TUTORIAL_INICIAL: EstadoDelTutorial = {
  activo: false,
  paso: null,
  rama: null,
  bloqueId: null,
  sistemaId: null,
  anchos: null,
  orden: null,
  nombre: null,
  filasIds: [],
  sistemasAlElegir: 0,
};

export type VistaDelTutorial = 'inicio' | 'plantillas' | 'zona' | 'taller' | 'editor';

/** Lo que el tutorial necesita saber de la pantalla para decidir si pasó al siguiente paso. */
export type ContextoDelTutorial = {
  vista: VistaDelTutorial;
  modo: 'build' | 'use';
  activeId: string | null;
  selectedId: string | null;
  sistemas: Sistema[];
  taller: { borrador: Sistema | null; pensando: boolean; hayMensajeDePersona: boolean } | null;
  /** El tipo del modal abierto, si hay uno. */
  modal: string | null;
};

// Un bloque con el que se puede practicar: tiene campos y no es un movimiento de stock.
export const esUsable = (bloque: Bloque) =>
  bloque.fields.length > 0 && !['entries', 'exits'].includes(bloque.type);

const sistemaDe = (estado: EstadoDelTutorial, contexto: ContextoDelTutorial) =>
  contexto.sistemas.find((item) => item.id === (estado.sistemaId ?? contexto.activeId));

const bloquesDe = (estado: EstadoDelTutorial, contexto: ContextoDelTutorial) =>
  allBlocks(sistemaDe(estado, contexto));

/** Entrar a un paso: guarda lo que hay que recordar para comparar después. */
export function entrarEn(
  paso: PasoDeTutorial,
  estado: EstadoDelTutorial,
  contexto: ContextoDelTutorial,
): Partial<EstadoDelTutorial> {
  const bloque = bloquesDe(estado, contexto).find((item) => item.id === estado.bloqueId);
  const patch: Partial<EstadoDelTutorial> = { paso };
  if (paso === 'review') {
    patch.anchos = Object.fromEntries(
      allBlocks(contexto.taller?.borrador ?? undefined).map((item) => [item.id, item.width]),
    );
  }
  if (paso === 'rename') patch.nombre = bloque?.name ?? null;
  if (paso === 'move')
    patch.orden = bloquesDe(estado, contexto)
      .map((item) => item.id)
      .join(',');
  if (paso === 'openRecord') patch.filasIds = bloque?.rows.map((fila) => fila.id) ?? [];
  if (['choose', 'blankConfirm', 'useTemplate', 'create'].includes(paso)) {
    patch.sistemasAlElegir = contexto.sistemas.length;
  }
  return patch;
}

// Al llegar al editor con un sistema nuevo: elegir con qué bloque practicar y en qué paso seguir.
function entrarAlEditor(
  estado: EstadoDelTutorial,
  contexto: ContextoDelTutorial,
): Partial<EstadoDelTutorial> {
  const sistema = contexto.sistemas.find((item) => item.id === contexto.activeId);
  const usables = allBlocks(sistema).filter(esUsable);
  const parcial: EstadoDelTutorial = {
    ...estado,
    sistemaId: contexto.activeId,
    bloqueId: usables[0]?.id ?? null,
  };
  const paso: PasoDeTutorial = !usables.length
    ? 'addBlock'
    : contexto.modo === 'build'
      ? 'customize'
      : 'buildMode';
  return {
    sistemaId: parcial.sistemaId,
    bloqueId: parcial.bloqueId,
    ...entrarEn(paso, parcial, contexto),
  };
}

/**
 * Si la persona ya hizo lo que pide el paso, devuelve qué cambia (el paso siguiente y lo que hay que
 * recordar); si no, `null`. Es el `advance()` del mockup, más lo que allá resolvía cada clic.
 */
export function avanzar(
  estado: EstadoDelTutorial,
  contexto: ContextoDelTutorial,
): Partial<EstadoDelTutorial> | null {
  if (!estado.activo || !estado.paso) return null;
  const bloque = bloquesDe(estado, contexto).find((item) => item.id === estado.bloqueId);
  const bloques = bloquesDe(estado, contexto);
  const hayUnoNuevo = contexto.sistemas.length > estado.sistemasAlElegir;

  switch (estado.paso) {
    case 'choose':
      if (contexto.vista === 'taller')
        return { rama: 'guide', ...entrarEn('describe', estado, contexto) };
      if (contexto.vista === 'plantillas')
        return { rama: 'template', ...entrarEn('pickTemplate', estado, contexto) };
      if (contexto.modal === 'blanco')
        return { rama: 'blank', ...entrarEn('blankConfirm', estado, contexto) };
      return null;
    case 'blankConfirm':
      if (contexto.vista === 'editor' && contexto.activeId && hayUnoNuevo)
        return entrarAlEditor(estado, contexto);
      if (contexto.vista === 'taller')
        return { rama: 'guide', ...entrarEn('describe', estado, contexto) };
      if (contexto.vista === 'plantillas')
        return { rama: 'template', ...entrarEn('pickTemplate', estado, contexto) };
      // Cerró el cartel sin confirmar: vuelve a elegir camino.
      if (contexto.modal !== 'blanco' && contexto.vista === 'inicio' && !hayUnoNuevo) {
        return { rama: null, ...entrarEn('choose', estado, contexto) };
      }
      return null;
    case 'pickTemplate':
      return contexto.modal === 'plantilla' ? entrarEn('useTemplate', estado, contexto) : null;
    case 'useTemplate':
      if (contexto.vista === 'editor' && contexto.activeId && hayUnoNuevo)
        return entrarAlEditor(estado, contexto);
      if (contexto.modal !== 'plantilla' && contexto.vista === 'plantillas' && !hayUnoNuevo) {
        return entrarEn('pickTemplate', estado, contexto);
      }
      return null;
    case 'describe':
      return contexto.vista === 'taller' &&
        contexto.taller?.borrador &&
        !contexto.taller.pensando &&
        contexto.taller.hayMensajeDePersona
        ? entrarEn('review', estado, contexto)
        : null;
    case 'review':
      return contexto.taller?.borrador &&
        allBlocks(contexto.taller.borrador).some((item) => estado.anchos?.[item.id] !== item.width)
        ? entrarEn('create', estado, contexto)
        : null;
    case 'create':
      return contexto.vista === 'editor' && contexto.activeId && hayUnoNuevo
        ? entrarAlEditor(estado, contexto)
        : null;
    case 'addBlock': {
      const primero = bloques.find(esUsable);
      return primero
        ? {
            bloqueId: primero.id,
            ...entrarEn('customize', { ...estado, bloqueId: primero.id }, contexto),
          }
        : null;
    }
    case 'buildMode':
      return contexto.modo === 'build' && contexto.vista === 'editor'
        ? entrarEn('customize', estado, contexto)
        : null;
    case 'customize':
      return contexto.selectedId === estado.bloqueId && contexto.modo === 'build'
        ? entrarEn('rename', estado, contexto)
        : null;
    case 'rename':
      return bloque?.name && bloque.name !== estado.nombre
        ? entrarEn(bloques.length > 1 ? 'move' : 'addSecond', estado, contexto)
        : null;
    case 'addSecond':
      return bloques.length > 1 ? entrarEn('move', estado, contexto) : null;
    case 'move':
      return bloques.map((item) => item.id).join(',') !== estado.orden
        ? entrarEn('openRecord', estado, contexto)
        : null;
    case 'openRecord':
      return contexto.modal === 'registro' ? entrarEn('fillRecord', estado, contexto) : null;
    case 'fillRecord':
      if (bloque?.rows.some((fila) => !estado.filasIds.includes(fila.id) && !fila.__example)) {
        return entrarEn('useMode', estado, contexto);
      }
      return contexto.modal !== 'registro' ? entrarEn('openRecord', estado, contexto) : null;
    case 'useMode':
      return contexto.modo === 'use' ? entrarEn('done', estado, contexto) : null;
    case 'done':
      return null;
  }
}
