import type { ReactNode } from 'react';
import { useSistemaStore } from '@/stores/sistemaStore';
import { IconBtn } from './IconBtn';

// Estructura de todos los modales: fondo, diálogo y botón de cerrar. Igual a showModal() del mockup.
export function Modal({
  ancho = false,
  clase = '',
  children,
}: {
  ancho?: boolean;
  /** Clases de más para el diálogo (la bienvenida usa `welcome-modal`). */
  clase?: string;
  children: ReactNode;
}) {
  const cerrarModal = useSistemaStore((estado) => estado.cerrarModal);
  return (
    <div
      className="overlay-backdrop"
      onClick={(evento) => {
        if (evento.target === evento.currentTarget) cerrarModal();
      }}
    >
      <section
        className={`modal ${ancho ? 'wide' : ''} ${clase}`.replace(/\s+/g, ' ').trim()}
        data-tour="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
      >
        <IconBtn
          name="close"
          label="Cerrar"
          data-modal-close="true"
          data-accion="close-modal"
          onClick={cerrarModal}
        />
        {children}
      </section>
    </div>
  );
}
