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
  | 'customize'
  | 'rename'
  | 'addSecond'
  | 'move'
  | 'openRecord'
  | 'fillRecord'
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
    text: 'Esta propuesta todavía no es una sección guardada. Podés seguir ajustándola o confirmar para empezar a usarla.',
    hint: 'Tocá “Sí, crear mi sección”.',
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
    hint: 'Confirmá la plantilla para crear tu sección.',
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
  customize: {
    grupo: 2,
    title: 'De acá se cambia.',
    text: 'Abrí los ajustes de esta pieza. Podés cambiar su nombre, sus campos, su color y la pestaña donde aparece.',
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
  done: {
    grupo: 4,
    title: 'Tu primera sección ya es tuya.',
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
  addBlock: ['add-block', 'herramientas', 'mobile-library', 'close-library'],
  customize: ['select-block'],
  rename: [],
  addSecond: ['add-block', 'herramientas', 'mobile-library', 'close-library'],
  move: ['move-up', 'move-down', 'noop'],
  openRecord: ['add-record'],
  fillRecord: ['close-modal'],
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
  activeId: string | null;
  selectedId: string | null;
  sistemas: Sistema[];
  taller: { borrador: Sistema | null; pensando: boolean; hayMensajeDePersona: boolean } | null;
  /** El tipo del modal abierto, si hay uno. */
  modal: string | null;
  /** Pantalla de celular: las piezas van en una columna y no hay anchos para elegir. */
  celular?: boolean;
};

// Un bloque con el que se puede practicar: tiene campos y no es un movimiento de stock.
export const esUsable = (bloque: Bloque) =>
  bloque.fields.length > 0 && !['entries', 'exits'].includes(bloque.type);

type Cambio = Partial<EstadoDelTutorial>;

/** Lo que una transición necesita mirar para decidir si el paso se cumplió. */
type Situacion = {
  estado: EstadoDelTutorial;
  contexto: ContextoDelTutorial;
  bloque?: Bloque;
  bloques: Bloque[];
  hayUnoNuevo: boolean;
};

type Transicion = (situacion: Situacion) => Cambio | null;

const sistemaDe = (estado: EstadoDelTutorial, contexto: ContextoDelTutorial) =>
  contexto.sistemas.find((item) => item.id === (estado.sistemaId ?? contexto.activeId));

const bloquesDe = (estado: EstadoDelTutorial, contexto: ContextoDelTutorial) =>
  allBlocks(sistemaDe(estado, contexto));

const bloqueDePractica = (estado: EstadoDelTutorial, contexto: ContextoDelTutorial) =>
  bloquesDe(estado, contexto).find((item) => item.id === estado.bloqueId);

const ordenDe = (bloques: Bloque[]) => bloques.map((item) => item.id).join(',');

const recordarLosSistemas = (_: EstadoDelTutorial, contexto: ContextoDelTutorial): Cambio => ({
  sistemasAlElegir: contexto.sistemas.length,
});

/** Lo que cada paso guarda al entrar, para comparar después si la persona cambió algo. */
const RECUERDOS: Partial<
  Record<PasoDeTutorial, (estado: EstadoDelTutorial, contexto: ContextoDelTutorial) => Cambio>
> = {
  review: (_, contexto) => ({
    anchos: Object.fromEntries(
      allBlocks(contexto.taller?.borrador ?? undefined).map((item) => [item.id, item.width]),
    ),
  }),
  rename: (estado, contexto) => ({ nombre: bloqueDePractica(estado, contexto)?.name ?? null }),
  move: (estado, contexto) => ({ orden: ordenDe(bloquesDe(estado, contexto)) }),
  openRecord: (estado, contexto) => ({
    filasIds: bloqueDePractica(estado, contexto)?.rows.map((fila) => fila.id) ?? [],
  }),
  choose: recordarLosSistemas,
  blankConfirm: recordarLosSistemas,
  useTemplate: recordarLosSistemas,
  create: recordarLosSistemas,
};

/** Entrar a un paso: guarda lo que hay que recordar para comparar después. */
export function entrarEn(
  paso: PasoDeTutorial,
  estado: EstadoDelTutorial,
  contexto: ContextoDelTutorial,
): Cambio {
  return { paso, ...RECUERDOS[paso]?.(estado, contexto) };
}

const irA = (
  paso: PasoDeTutorial,
  { estado, contexto }: Situacion,
  extra: Cambio = {},
): Cambio => ({
  ...extra,
  ...entrarEn(paso, estado, contexto),
});

// Al llegar al editor con un sistema nuevo: elegir con qué bloque practicar y en qué paso seguir.
function entrarAlEditor({ estado, contexto }: Situacion): Cambio {
  const sistema = contexto.sistemas.find((item) => item.id === contexto.activeId);
  const usables = allBlocks(sistema).filter(esUsable);
  const parcial: EstadoDelTutorial = {
    ...estado,
    sistemaId: contexto.activeId,
    bloqueId: usables[0]?.id ?? null,
  };
  const paso: PasoDeTutorial = usables.length ? 'customize' : 'addBlock';
  return {
    sistemaId: parcial.sistemaId,
    bloqueId: parcial.bloqueId,
    ...entrarEn(paso, parcial, contexto),
  };
}

const llegoAlEditorConUnoNuevo = ({ contexto, hayUnoNuevo }: Situacion) =>
  contexto.vista === 'editor' && Boolean(contexto.activeId) && hayUnoNuevo;

/** Los caminos que se eligen yendo a una pantalla: con la ayuda de Sami o con una plantilla. */
const CAMINOS: {
  vista: VistaDelTutorial;
  rama: EstadoDelTutorial['rama'];
  paso: PasoDeTutorial;
}[] = [
  { vista: 'taller', rama: 'guide', paso: 'describe' },
  { vista: 'plantillas', rama: 'template', paso: 'pickTemplate' },
];

function caminoElegido(situacion: Situacion): Cambio | null {
  const camino = CAMINOS.find(({ vista }) => situacion.contexto.vista === vista);
  return camino ? irA(camino.paso, situacion, { rama: camino.rama }) : null;
}

const elegirCamino: Transicion = (situacion) => {
  const camino = caminoElegido(situacion);
  if (camino) return camino;
  return situacion.contexto.modal === 'blanco'
    ? irA('blankConfirm', situacion, { rama: 'blank' })
    : null;
};

// Cerró el cartel sin confirmar: vuelve a elegir camino.
const cerroElLienzoSinConfirmar = ({ contexto, hayUnoNuevo }: Situacion) =>
  contexto.modal !== 'blanco' && contexto.vista === 'inicio' && !hayUnoNuevo;

const confirmarDesdeCero: Transicion = (situacion) => {
  if (llegoAlEditorConUnoNuevo(situacion)) return entrarAlEditor(situacion);
  const camino = caminoElegido(situacion);
  if (camino) return camino;
  return cerroElLienzoSinConfirmar(situacion) ? irA('choose', situacion, { rama: null }) : null;
};

const cerroLaPlantillaSinUsarla = ({ contexto, hayUnoNuevo }: Situacion) =>
  contexto.modal !== 'plantilla' && contexto.vista === 'plantillas' && !hayUnoNuevo;

const usarLaPlantilla: Transicion = (situacion) => {
  if (llegoAlEditorConUnoNuevo(situacion)) return entrarAlEditor(situacion);
  return cerroLaPlantillaSinUsarla(situacion) ? irA('pickTemplate', situacion) : null;
};

const samiYaPropuso = ({ contexto }: Situacion) =>
  contexto.vista === 'taller' &&
  Boolean(contexto.taller?.borrador) &&
  !contexto.taller?.pensando &&
  Boolean(contexto.taller?.hayMensajeDePersona);

// En el celular no hay anchos: se pasa directo a crear.
const pasoDespuesDeDescribir = ({ contexto }: Situacion): PasoDeTutorial =>
  contexto.celular ? 'create' : 'review';

function cambioAlgunAncho({ estado, contexto }: Situacion) {
  const borrador = contexto.taller?.borrador;
  if (!borrador) return false;
  return (
    Boolean(contexto.celular) ||
    allBlocks(borrador).some((item) => estado.anchos?.[item.id] !== item.width)
  );
}

function practicarConElPrimerBloque(situacion: Situacion): Cambio | null {
  const primero = situacion.bloques.find(esUsable);
  if (!primero) return null;
  const estado = { ...situacion.estado, bloqueId: primero.id };
  return { bloqueId: primero.id, ...entrarEn('customize', estado, situacion.contexto) };
}

const renombroElBloque = ({ bloque, estado }: Situacion) =>
  Boolean(bloque?.name) && bloque?.name !== estado.nombre;

const guardoUnRegistroPropio = ({ bloque, estado }: Situacion) =>
  Boolean(bloque?.rows.some((fila) => !estado.filasIds.includes(fila.id) && !fila.__example));

function llenarElRegistro(situacion: Situacion): Cambio | null {
  if (guardoUnRegistroPropio(situacion)) return irA('done', situacion);
  return situacion.contexto.modal !== 'registro' ? irA('openRecord', situacion) : null;
}

/** Qué hace pasar cada paso al siguiente. Es el `advance()` del mockup, más lo que allá resolvía cada clic. */
const TRANSICIONES: Record<PasoDeTutorial, Transicion> = {
  choose: elegirCamino,
  blankConfirm: confirmarDesdeCero,
  pickTemplate: (s) => (s.contexto.modal === 'plantilla' ? irA('useTemplate', s) : null),
  useTemplate: usarLaPlantilla,
  describe: (s) => (samiYaPropuso(s) ? irA(pasoDespuesDeDescribir(s), s) : null),
  review: (s) => (cambioAlgunAncho(s) ? irA('create', s) : null),
  create: (s) => (llegoAlEditorConUnoNuevo(s) ? entrarAlEditor(s) : null),
  addBlock: practicarConElPrimerBloque,
  customize: (s) => (s.contexto.selectedId === s.estado.bloqueId ? irA('rename', s) : null),
  rename: (s) => (renombroElBloque(s) ? irA(s.bloques.length > 1 ? 'move' : 'addSecond', s) : null),
  addSecond: (s) => (s.bloques.length > 1 ? irA('move', s) : null),
  move: (s) => (ordenDe(s.bloques) !== s.estado.orden ? irA('openRecord', s) : null),
  openRecord: (s) => (s.contexto.modal === 'registro' ? irA('fillRecord', s) : null),
  fillRecord: llenarElRegistro,
  done: () => null,
};

/**
 * Si la persona ya hizo lo que pide el paso, devuelve qué cambia (el paso siguiente y lo que hay que
 * recordar); si no, `null`.
 */
export function avanzar(estado: EstadoDelTutorial, contexto: ContextoDelTutorial): Cambio | null {
  if (!estado.activo || !estado.paso) return null;
  return TRANSICIONES[estado.paso]({
    estado,
    contexto,
    bloque: bloqueDePractica(estado, contexto),
    bloques: bloquesDe(estado, contexto),
    hayUnoNuevo: contexto.sistemas.length > estado.sistemasAlElegir,
  });
}
