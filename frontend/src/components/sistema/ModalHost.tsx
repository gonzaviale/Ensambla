import { useEffect, useRef } from 'react';
import { useSistemaStore } from '@/stores/sistemaStore';
import { ModalAyuda } from './modales/ModalAyuda';
import { ModalBlanco } from './modales/ModalBlanco';
import { ModalConfirmar } from './modales/ModalConfirmar';
import { ModalPerfil } from './modales/ModalPerfil';
import { ModalPlantilla } from './modales/ModalPlantilla';
import { ModalRenombrar } from './modales/ModalRenombrar';

const ENFOCABLES =
  'button:not(:disabled),a[href],input:not([hidden]),select,textarea,[tabindex="0"]';
const PRIMER_CAMPO =
  'input:not([type="checkbox"]):not([hidden]),textarea,select,button:not([data-modal-close])';

function ContenidoDelModal() {
  const modal = useSistemaStore((estado) => estado.modal);
  if (!modal) return null;
  switch (modal.tipo) {
    case 'blanco':
      return <ModalBlanco />;
    case 'plantilla':
      return <ModalPlantilla clave={modal.clave} />;
    case 'ayuda':
      return <ModalAyuda respuesta={modal.respuesta} />;
    case 'perfil':
      return <ModalPerfil />;
    case 'renombrar':
      return <ModalRenombrar />;
    case 'confirmar':
      return <ModalConfirmar {...modal} />;
  }
}

// El `#overlay` del mockup: Esc cierra, Tab queda atrapado adentro, el fondo no se desplaza y al cerrar
// el foco vuelve a donde estaba.
export function ModalHost() {
  const hayModal = useSistemaStore((estado) => estado.modal !== null);
  const cerrarModal = useSistemaStore((estado) => estado.cerrarModal);
  const raiz = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!hayModal) return;
    const anterior = document.activeElement as HTMLElement | null;
    document.body.style.overflow = 'hidden';
    const primero =
      raiz.current?.querySelector<HTMLElement>(PRIMER_CAMPO) ??
      raiz.current?.querySelector<HTMLElement>('button');
    primero?.focus();

    const alTeclear = (evento: KeyboardEvent) => {
      if (evento.key === 'Escape') {
        cerrarModal();
        return;
      }
      if (evento.key !== 'Tab' || !raiz.current) return;
      const nodos = [...raiz.current.querySelectorAll<HTMLElement>(ENFOCABLES)].filter(
        (nodo) => !nodo.closest('[hidden]'),
      );
      const inicio = nodos[0];
      const fin = nodos.at(-1);
      if (evento.shiftKey && document.activeElement === inicio) {
        evento.preventDefault();
        fin?.focus();
      } else if (!evento.shiftKey && document.activeElement === fin) {
        evento.preventDefault();
        inicio?.focus();
      }
    };
    document.addEventListener('keydown', alTeclear);
    return () => {
      document.removeEventListener('keydown', alTeclear);
      document.body.style.overflow = '';
      if (anterior?.isConnected) anterior.focus();
      else document.getElementById('main')?.focus({ preventScroll: true });
    };
  }, [hayModal, cerrarModal]);

  return (
    <div id="overlay" ref={raiz}>
      <ContenidoDelModal />
    </div>
  );
}
