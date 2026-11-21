import { useEffect, useId, useRef, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { Icon } from '@/components/sistema/Icon';

type Props = {
  titulo: string;
  tamano: 'chica' | 'mediana';
  onCerrar: () => void;
  children: ReactNode;
};

/**
 * Una ventana centrada encima de toda la pantalla (se dibuja al final de `.mk-root`, donde aplican los estilos del sistema, fuera de cualquier contenedor), con el fondo
 * oscuro: lo de atrás no se puede tocar ni mover. Se cierra con la ✕, con Escape o tocando el
 * fondo; al abrirse, el foco entra en ella (salvo que un campo de adentro ya lo haya tomado).
 */
export function VentanaFlotante({ titulo, tamano, onCerrar, children }: Props) {
  const idDelTitulo = useId();
  const ventana = useCerrarConEscape(onCerrar);
  useBloquearElScrollDeLaPagina();
  return createPortal(
    <div className="ventana-flotante-fondo" onClick={onCerrar}>
      <section
        ref={ventana}
        className={`ventana-flotante ventana-flotante-${tamano}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby={idDelTitulo}
        tabIndex={-1}
        onClick={(evento) => evento.stopPropagation()}
      >
        <header className="ventana-flotante-cabecera">
          <h2 id={idDelTitulo}>{titulo}</h2>
          <button
            type="button"
            className="ventana-flotante-cerrar"
            aria-label="Cerrar"
            onClick={onCerrar}
          >
            <Icon name="close" />
          </button>
        </header>
        {children}
      </section>
    </div>,
    document.querySelector('.mk-root') ?? document.body,
  );
}

// Con la ventana abierta la página de atrás no se mueve; el ancho de la barra de scroll se compensa para que nada salte.
function useBloquearElScrollDeLaPagina() {
  useEffect(() => {
    const { style } = document.body;
    const anterior = { overflow: style.overflow, relleno: style.paddingRight };
    const barra = window.innerWidth - document.documentElement.clientWidth;
    style.overflow = 'hidden';
    if (barra > 0) style.paddingRight = `${barra}px`;
    return () => {
      style.overflow = anterior.overflow;
      style.paddingRight = anterior.relleno;
    };
  }, []);
}

function useCerrarConEscape(onCerrar: () => void) {
  const ventana = useRef<HTMLElement>(null);
  useEffect(() => {
    if (!ventana.current?.contains(document.activeElement)) ventana.current?.focus();
    const alApretar = (evento: KeyboardEvent) => {
      if (evento.key === 'Escape') onCerrar();
    };
    document.addEventListener('keydown', alApretar);
    return () => document.removeEventListener('keydown', alApretar);
  }, [onCerrar]);
  return ventana;
}
