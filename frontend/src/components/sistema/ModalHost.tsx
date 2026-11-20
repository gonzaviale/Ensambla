import { useEffect, useRef, type RefObject } from 'react';
import { useSistemaStore } from '@/stores/sistemaStore';
import { contenidoDeModal } from './modales/contenidoDeModal';

const ENFOCABLES =
  'button:not(:disabled),a[href],input:not([hidden]),select,textarea,[tabindex="0"]';
const PRIMER_CAMPO =
  'input:not([type="checkbox"]):not([hidden]),textarea,select,button:not([data-modal-close])';

function ContenidoDelModal() {
  const modal = useSistemaStore((estado) => estado.modal);
  return modal ? contenidoDeModal(modal) : null;
}

function enfocarPrimerCampo(raiz: HTMLElement | null) {
  const primero =
    raiz?.querySelector<HTMLElement>(PRIMER_CAMPO) ?? raiz?.querySelector<HTMLElement>('button');
  primero?.focus();
}

function enfocablesDe(raiz: HTMLElement) {
  return [...raiz.querySelectorAll<HTMLElement>(ENFOCABLES)].filter(
    (nodo) => !nodo.closest('[hidden]'),
  );
}

/** Tab queda atrapado adentro: del último vuelve al primero y, con Shift, del primero al último. */
function atraparTab(evento: KeyboardEvent, raiz: HTMLElement) {
  const nodos = enfocablesDe(raiz);
  const inicio = nodos[0];
  const fin = nodos.at(-1);
  const [salida, destino] = evento.shiftKey ? [inicio, fin] : [fin, inicio];
  if (document.activeElement !== salida) return;
  evento.preventDefault();
  destino?.focus();
}

function responderALaTecla(evento: KeyboardEvent, raiz: HTMLElement | null, cerrarModal: () => void) {
  if (evento.key === 'Escape') {
    cerrarModal();
    return;
  }
  if (evento.key === 'Tab' && raiz) atraparTab(evento, raiz);
}

function devolverElFoco(anterior: HTMLElement | null) {
  if (anterior?.isConnected && !anterior.matches(':disabled')) {
    anterior.focus();
    return;
  }
  document.getElementById('main')?.focus({ preventScroll: true });
}

function useModalAccesible(
  raiz: RefObject<HTMLDivElement | null>,
  hayModal: boolean,
  cerrarModal: () => void,
) {
  useEffect(() => {
    if (!hayModal) return;
    const anterior = document.activeElement as HTMLElement | null;
    document.body.style.overflow = 'hidden';
    enfocarPrimerCampo(raiz.current);
    const alTeclear = (evento: KeyboardEvent) => responderALaTecla(evento, raiz.current, cerrarModal);
    document.addEventListener('keydown', alTeclear);
    return () => {
      document.removeEventListener('keydown', alTeclear);
      document.body.style.overflow = '';
      devolverElFoco(anterior);
    };
  }, [hayModal, cerrarModal, raiz]);
}

// El `#overlay` del mockup: Esc cierra, Tab queda atrapado adentro, el fondo no se desplaza y al cerrar
// el foco vuelve a donde estaba.
export function ModalHost() {
  const hayModal = useSistemaStore((estado) => estado.modal !== null);
  const cerrarModal = useSistemaStore((estado) => estado.cerrarModal);
  const raiz = useRef<HTMLDivElement>(null);
  useModalAccesible(raiz, hayModal, cerrarModal);

  return (
    <div id="overlay" ref={raiz}>
      <ContenidoDelModal />
    </div>
  );
}
