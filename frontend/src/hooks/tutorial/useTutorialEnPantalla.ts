import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { useRouterState } from '@tanstack/react-router';
import {
  ACCIONES_DEL_PASO,
  PASOS,
  avanzar,
  type ContextoDelTutorial,
  type PasoDeTutorial,
  type VistaDelTutorial,
} from '@/lib/sistema/tutorial';
import { useSistemaStore } from '@/stores/sistemaStore';
import { salirDelTutorial } from './useAccionesDelTutorial';

export type Caja = { x: number; y: number; w: number; h: number };
export type PosicionDelCartel = { left: number; top: number; width: number };

const ENFOCABLES = 'button,input,select,textarea,a[href],[tabindex="0"]';
const vistaDe = (ruta: string): VistaDelTutorial =>
  ruta.startsWith('/sistemas/')
    ? 'editor'
    : ruta === '/plantillas'
      ? 'plantillas'
      : ruta === '/taller'
        ? 'taller'
        : ruta === '/zona'
          ? 'zona'
          : 'inicio';

const todos = (selector: string) => [...document.querySelectorAll<HTMLElement>(selector)];
const porMarca = (marca: string, id?: string | null) =>
  todos(`[data-tour~="${marca}"]`).filter((nodo) => !id || nodo.dataset.tourId === id);

type MarcaDelPaso = [marca: string, id: string | null];
type PedidoDeMarcas = { bloqueId: string | null; pensando: boolean };

/** «Tus piezas» se ve abierta o, fuera del celular, fijada al costado; si no, se señala «Bloques». */
function bibliotecaDePiezasVisible() {
  return (
    !!document.querySelector('.block-library.mobile-open') ||
    (window.innerWidth > 680 && !!document.querySelector('.block-library.fijada'))
  );
}

function marcasParaAgregarUnaPieza(): MarcaDelPaso[] {
  if (!bibliotecaDePiezasVisible()) {
    return [
      ['agregar-pieza-movil', null],
      ['agregar-pieza-vacio', null],
      ['lienzo', null],
    ];
  }
  return [
    ['agregar-pieza', null],
    ['lienzo', null],
    ['agregar-pieza-vacio', null],
  ];
}

const marcaDelMismoPaso = (paso: PasoDeTutorial) => (): MarcaDelPaso[] => [[paso, null]];
const marcaDelModal = (): MarcaDelPaso[] => [['modal', null]];
const marcaDelBloque =
  (paso: PasoDeTutorial) =>
  ({ bloqueId }: PedidoDeMarcas): MarcaDelPaso[] => [[paso, bloqueId]];

/** Cada paso sabe qué señala: agregar un paso sin sus marcas no compila. */
const marcasPorPaso: Record<PasoDeTutorial, (pedido: PedidoDeMarcas) => MarcaDelPaso[]> = {
  choose: marcaDelMismoPaso('choose'),
  review: marcaDelMismoPaso('review'),
  create: marcaDelMismoPaso('create'),
  pickTemplate: marcaDelMismoPaso('pickTemplate'),
  rename: marcaDelMismoPaso('rename'),
  fillRecord: marcaDelMismoPaso('fillRecord'),
  describe: ({ pensando }) => [[pensando ? 'describe-pensando' : 'describe', null]],
  useTemplate: marcaDelModal,
  blankConfirm: marcaDelModal,
  addBlock: marcasParaAgregarUnaPieza,
  addSecond: marcasParaAgregarUnaPieza,
  customize: marcaDelBloque('customize'),
  openRecord: marcaDelBloque('openRecord'),
  done: marcaDelBloque('done'),
  move: ({ bloqueId }) => [
    ['move', bloqueId],
    ['lienzo', null],
  ],
};

/** Los elementos que el paso señala: lo único que se puede tocar (más lo que cada paso permite aparte). */
const SELECTOR_DEL_FOCO: Partial<Record<PasoDeTutorial, string>> = {
  rename: '#block-name',
  describe: '#guide-message',
  fillRecord: '#overlay form input:not([type="hidden"]), #overlay form select',
};

function elementoParaEnfocar(paso: PasoDeTutorial | null | undefined, pensando: boolean) {
  if (!paso || (paso === 'describe' && pensando)) return null;
  const selector = SELECTOR_DEL_FOCO[paso];
  return selector ? document.querySelector<HTMLElement>(selector) : null;
}

type MedidasDelCartel = {
  ancho: number;
  alto: number;
  anchoDelCartel: number;
  altoDelCartel: number;
};

type PuntoDelCartel = { left: number; top: number };

const posicionAbajoAlaDerecha = (m: MedidasDelCartel): PuntoDelCartel => ({
  left: m.ancho - m.anchoDelCartel - 20,
  top: m.alto - m.altoDelCartel - 20,
});

const alineadoALaIzquierdaDe = (primera: DOMRect, m: MedidasDelCartel) =>
  Math.max(12, Math.min(m.ancho - m.anchoDelCartel - 12, primera.left));

const aLaDerechaDe = (primera: DOMRect, m: MedidasDelCartel): PuntoDelCartel => ({
  left: primera.right + 20,
  top: Math.max(12, Math.min(m.alto - m.altoDelCartel - 12, primera.top)),
});

const arribaDe = (primera: DOMRect, m: MedidasDelCartel): PuntoDelCartel => ({
  left: alineadoALaIzquierdaDe(primera, m),
  top: primera.top - m.altoDelCartel - 18,
});

const abajoDe = (primera: DOMRect, m: MedidasDelCartel): PuntoDelCartel => ({
  left: alineadoALaIzquierdaDe(primera, m),
  top: primera.bottom + 18,
});

function posicionEnPantallaGrande(primera: DOMRect, m: MedidasDelCartel): PuntoDelCartel {
  if (m.ancho - primera.right > m.anchoDelCartel + 34) return aLaDerechaDe(primera, m);
  if (primera.top > m.altoDelCartel + 30) return arribaDe(primera, m);
  if (m.alto - primera.bottom > m.altoDelCartel + 30) return abajoDe(primera, m);
  return posicionAbajoAlaDerecha(m);
}

// En el celular el cartel va abajo, salvo con el chat de Sami abierto (abajo se escribe) o cuando
// lo señalado está abajo: ahí sube arriba para no taparlo.
function elCartelDebeSubir(primera: DOMRect | undefined, alto: number) {
  const hayChatAbierto = Boolean(
    document.querySelector('.sami-conversation.flotante:not([hidden])'),
  );
  const loSenaladoEstaAbajo = primera !== undefined && primera.top + primera.height / 2 > alto / 2;
  return hayChatAbierto || loSenaladoEstaAbajo;
}

function posicionDelCartel(primera: DOMRect | undefined, m: MedidasDelCartel): PuntoDelCartel {
  if (primera && m.ancho > 760) return posicionEnPantallaGrande(primera, m);
  const abajo = posicionAbajoAlaDerecha(m);
  return elCartelDebeSubir(primera, m.alto) ? { ...abajo, top: 12 } : abajo;
}

export function objetivosDelPaso(paso: PasoDeTutorial, bloqueId: string | null, pensando: boolean) {
  const marcas = marcasPorPaso[paso]({ bloqueId, pensando });
  return [...new Set(marcas.flatMap(([marca, id]) => porMarca(marca, id)))];
}

// Todo lo que el tutorial necesita de la pantalla: decide cuándo se pasa al paso siguiente, señala los
// elementos del paso, bloquea lo que no corresponde y calcula dónde van el reflector y el cartel.
export function useTutorialEnPantalla() {
  const tutorial = useSistemaStore((estado) => estado.tutorial);
  const hayBienvenida = useSistemaStore((estado) => estado.modal?.tipo === 'bienvenida');
  const pensando = useSistemaStore((estado) => estado.taller?.pensando ?? false);
  const ruta = useRouterState({ select: (estado) => estado.location.pathname });
  const [pulso, setPulso] = useState(0);
  const [cajas, setCajas] = useState<Caja[]>([]);
  const [cartel, setCartel] = useState<PosicionDelCartel>({ left: 12, top: 12, width: 370 });
  const refCartel = useRef<HTMLElement>(null);
  const pasoAnterior = useRef<PasoDeTutorial | null>(null);
  const marcados = useRef<Set<HTMLElement>>(new Set());
  const cuadro = useRef(0);

  const enPausa = !tutorial.activo || hayBienvenida;
  const paso = tutorial.paso;

  // Pide volver a calcular todo en cuanto se pueda, una sola vez aunque lleguen varios avisos juntos.
  const programar = useRef<() => void>(() => undefined);
  programar.current = () => {
    cancelAnimationFrame(cuadro.current);
    cuadro.current = requestAnimationFrame(() => setPulso((valor) => valor + 1));
  };

  // 1. ¿La persona ya hizo lo que pedía el paso? Se mira cuando cambia lo que importa del store o la pantalla.
  // Se junta todo lo que cambió en un mismo momento y se avanza de a un paso, así un paso nunca se saltea.
  useEffect(() => {
    let cuadroDeRevision = 0;
    const revisar = () => {
      const estado = useSistemaStore.getState();
      const contexto: ContextoDelTutorial = {
        vista: vistaDe(ruta),
        activeId: estado.activeId,
        selectedId: estado.selectedId,
        sistemas: estado.sistemas,
        taller: estado.taller
          ? {
              borrador: estado.taller.borrador,
              pensando: estado.taller.pensando,
              hayMensajeDePersona: estado.taller.mensajes.some(
                (mensaje) => mensaje.quien === 'you',
              ),
            }
          : null,
        modal: estado.modal?.tipo ?? null,
        celular: window.matchMedia?.('(max-width: 680px)').matches ?? false,
      };
      const cambio = avanzar(estado.tutorial, contexto);
      if (cambio) estado.ponerTutorial(cambio);
    };
    const programarRevision = () => {
      cancelAnimationFrame(cuadroDeRevision);
      cuadroDeRevision = requestAnimationFrame(revisar);
    };
    revisar();
    const desuscribir = useSistemaStore.subscribe((estado, anterior) => {
      // Los avisos, el panel o lo que cambia el propio tutorial no cuentan como una acción de la persona.
      if (
        estado.sistemas !== anterior.sistemas ||
        estado.activeId !== anterior.activeId ||
        estado.selectedId !== anterior.selectedId ||
        estado.modal !== anterior.modal ||
        estado.taller !== anterior.taller
      ) {
        programarRevision();
      }
    });
    return () => {
      cancelAnimationFrame(cuadroDeRevision);
      desuscribir();
    };
  }, [ruta]);

  // 2. Señalar los elementos del paso y ubicar el reflector y el cartel.
  useLayoutEffect(() => {
    const raiz = document.querySelector<HTMLElement>('.mk-root');
    const soltar = () => {
      raiz?.classList.remove('tutorial-running', 'tutorial-modal');
      raiz?.style.removeProperty('--tour-space');
      for (const nodo of marcados.current) nodo.classList.remove('tour-target');
      marcados.current.clear();
    };
    if (enPausa || !paso || !raiz) {
      soltar();
      return;
    }
    raiz.classList.add('tutorial-running');
    raiz.classList.toggle('tutorial-modal', Boolean(document.querySelector('#overlay .modal')));
    const objetivos = objetivosDelPaso(paso, tutorial.bloqueId, pensando);

    // Sólo se toca lo que cambió, para no disparar al observador de cambios.
    for (const nodo of [...marcados.current]) {
      if (!objetivos.includes(nodo)) {
        nodo.classList.remove('tour-target');
        marcados.current.delete(nodo);
      }
    }
    for (const nodo of objetivos) {
      if (!marcados.current.has(nodo)) {
        nodo.classList.add('tour-target');
        marcados.current.add(nodo);
      }
    }

    // Al llegar a un paso nuevo: acercar lo señalado y poner el foco donde se va a escribir.
    if (pasoAnterior.current !== paso) {
      pasoAnterior.current = paso;
      const primero =
        objetivos.find((nodo) => nodo.getBoundingClientRect().width > 0) ?? objetivos[0];
      const rect = primero?.getBoundingClientRect();
      if (primero && rect && (rect.top < 80 || rect.bottom > window.innerHeight - 80)) {
        primero.scrollIntoView?.({
          block: 'center',
          behavior: window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
            ? 'instant'
            : 'smooth',
        });
      }
      elementoParaEnfocar(paso, pensando)?.focus({ preventScroll: true });
    }

    // Reflector y cartel.
    const ancho = window.innerWidth;
    const alto = Math.min(window.innerHeight, window.visualViewport?.height ?? window.innerHeight);
    const rects = objetivos
      .map((nodo) => nodo.getBoundingClientRect())
      .filter((r) => r.width > 0 && r.height > 0 && r.bottom > 0 && r.top < alto);
    setCajas(
      rects.map((r) => ({
        x: Math.max(5, r.left - 7),
        y: Math.max(5, r.top - 7),
        w: Math.min(ancho - 10, r.width + 14),
        h: Math.min(alto - 10, r.height + 14),
      })),
    );
    const anchoDelCartel = Math.min(370, ancho - 24);
    const altoDelCartel = refCartel.current?.offsetHeight || 270;
    raiz.style.setProperty('--tour-space', `${altoDelCartel + 26}px`);
    const { left, top } = posicionDelCartel(rects[0], {
      ancho,
      alto,
      anchoDelCartel,
      altoDelCartel,
    });
    setCartel({
      width: anchoDelCartel,
      left: Math.max(12, left),
      top: Math.max(12, Math.min(alto - altoDelCartel - 12, top)),
    });
  }, [pulso, paso, enPausa, tutorial.bloqueId, pensando, ruta]);

  // Al salir del tutorial no queda nada marcado.
  useEffect(
    () => () => {
      const raiz = document.querySelector<HTMLElement>('.mk-root');
      raiz?.classList.remove('tutorial-running', 'tutorial-modal');
      raiz?.style.removeProperty('--tour-space');
      for (const nodo of marcados.current) nodo.classList.remove('tour-target');
    },
    [],
  );

  // 3. Eventos: recalcular tras cada interacción, bloquear lo que no corresponde y atrapar Tab.
  useEffect(() => {
    if (enPausa) return;
    const nuevoPaso = () => useSistemaStore.getState().tutorial;

    const permitido = (objetivo: Element) => {
      const { paso: actual, bloqueId } = nuevoPaso();
      if (!actual) return true;
      if (objetivo.closest('#ensambla-tutorial, #toast')) return true;
      const accion = objetivo.closest<HTMLElement>('[data-accion]');
      const relevante =
        !accion ||
        Boolean(accion.closest('#ensambla-tutorial')) ||
        accion.dataset.accion === 'home' ||
        ACCIONES_DEL_PASO[actual].includes(accion.dataset.accion ?? '');
      if (!relevante) return false;
      if (objetivo.closest('[data-accion="home"]')) return true;
      if (
        actual === 'choose' &&
        objetivo.closest('[data-accion="guide"],[data-accion="templates"],[data-accion="blank"]')
      ) {
        return true;
      }
      const hayModal = Boolean(document.querySelector('#overlay .modal'));
      if (hayModal && ['fillRecord', 'useTemplate', 'blankConfirm'].includes(actual)) {
        return Boolean(objetivo.closest('#overlay'));
      }
      if (actual === 'review' && useSistemaStore.getState().modal?.tipo === 'renombrarPieza') {
        return Boolean(objetivo.closest('#overlay'));
      }
      return objetivosDelPaso(
        actual,
        bloqueId,
        useSistemaStore.getState().taller?.pensando ?? false,
      ).some((nodo) => nodo === objetivo || nodo.contains(objetivo));
    };

    const avisarQueFalta = () =>
      useSistemaStore
        .getState()
        .avisar(PASOS[nuevoPaso().paso ?? 'choose'].hint || 'Elegí una opción para continuar.');

    const alHacerClic = (evento: MouseEvent) => {
      const objetivo = evento.target as Element;
      // Volver al inicio es siempre posible: termina la guía y sigue de largo.
      if (objetivo.closest('[data-accion="home"]')) {
        salirDelTutorial();
        return;
      }
      if (!permitido(objetivo)) {
        evento.preventDefault();
        evento.stopImmediatePropagation();
        avisarQueFalta();
        return;
      }
      programar.current();
    };
    const alArrastrar = (evento: DragEvent) => {
      if (!permitido(evento.target as Element)) {
        evento.preventDefault();
        evento.stopImmediatePropagation();
        avisarQueFalta();
      }
    };
    const alInteractuar = () => programar.current();
    const alTeclear = (evento: KeyboardEvent) => {
      if (evento.key === 'Escape') {
        evento.preventDefault();
        evento.stopImmediatePropagation();
        salirDelTutorial();
        return;
      }
      if (evento.key !== 'Tab') return;
      const { paso: actual, bloqueId } = nuevoPaso();
      if (!actual) return;
      const objetivos = objetivosDelPaso(
        actual,
        bloqueId,
        useSistemaStore.getState().taller?.pensando ?? false,
      );
      const nodos = [
        ...new Set([
          ...objetivos.flatMap((nodo) => [
            ...(nodo.matches(ENFOCABLES) ? [nodo] : []),
            ...nodo.querySelectorAll<HTMLElement>(ENFOCABLES),
          ]),
          ...document.querySelectorAll<HTMLElement>('.tour-coach button'),
        ]),
      ].filter(
        (nodo) =>
          !(nodo as HTMLButtonElement).disabled &&
          !nodo.hidden &&
          getComputedStyle(nodo).display !== 'none',
      );
      if (!nodos.length) return;
      const indice = nodos.indexOf(document.activeElement as HTMLElement);
      const siguiente = evento.shiftKey
        ? indice <= 0
          ? nodos.length - 1
          : indice - 1
        : (indice + 1) % nodos.length;
      evento.preventDefault();
      evento.stopImmediatePropagation();
      nodos[siguiente]?.focus();
      programar.current();
    };
    const alMover = () => programar.current();

    document.addEventListener('click', alHacerClic, true);
    document.addEventListener('dragstart', alArrastrar, true);
    for (const tipo of ['input', 'change', 'submit', 'drop', 'pointerup', 'focusout']) {
      document.addEventListener(tipo, alInteractuar, true);
    }
    document.addEventListener('keydown', alTeclear, true);
    window.addEventListener('resize', alMover);
    window.addEventListener('scroll', alMover, true);
    window.visualViewport?.addEventListener('resize', alMover);
    window.visualViewport?.addEventListener('scroll', alMover);
    // Lo que React redibuja (un modal que aparece, un bloque nuevo) también mueve el reflector.
    const observador = new MutationObserver(alMover);
    const raiz = document.querySelector('.mk-root');
    if (raiz) observador.observe(raiz, { childList: true, subtree: true });

    return () => {
      document.removeEventListener('click', alHacerClic, true);
      document.removeEventListener('dragstart', alArrastrar, true);
      for (const tipo of ['input', 'change', 'submit', 'drop', 'pointerup', 'focusout']) {
        document.removeEventListener(tipo, alInteractuar, true);
      }
      document.removeEventListener('keydown', alTeclear, true);
      window.removeEventListener('resize', alMover);
      window.removeEventListener('scroll', alMover, true);
      window.visualViewport?.removeEventListener('resize', alMover);
      window.visualViewport?.removeEventListener('scroll', alMover);
      observador.disconnect();
    };
  }, [enPausa]);

  return { enPausa, paso, cajas, cartel, refCartel, pensando };
}
