import type { ReactNode } from 'react';
import { useSistemaStore } from '@/stores/sistemaStore';
import { IconBtn } from './IconBtn';

// Estructura de todos los modales: fondo, diálogo y botón de cerrar. Igual a showModal() del mockup.
export function Modal({ ancho = false, children }: { ancho?: boolean; children: ReactNode }) {
  const cerrarModal = useSistemaStore((estado) => estado.cerrarModal);
  return (
    <div
      className="overlay-backdrop"
      onClick={(evento) => {
        if (evento.target === evento.currentTarget) cerrarModal();
      }}
    >
      <section
        className={`modal ${ancho ? 'wide' : ''}`.trim()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
      >
        <IconBtn name="close" label="Cerrar" data-modal-close="true" onClick={cerrarModal} />
        {children}
      </section>
    </div>
  );
}
