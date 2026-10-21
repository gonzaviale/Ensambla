import { useEffect, useRef } from 'react';
import { Icon } from './Icon';

// La foto adjunta, en grande: se cierra con la X, con un click afuera o con Escape, y el foco vuelve a quien la abrió.
export function VisorDeFoto({
  src,
  alt,
  alCerrar,
}: {
  src: string;
  alt: string;
  alCerrar: () => void;
}) {
  const cerrar = useRef<HTMLButtonElement>(null);
  // Siempre la última versión, para que el efecto de abajo corra una sola vez por apertura.
  const cierre = useRef(alCerrar);
  useEffect(() => {
    cierre.current = alCerrar;
  });

  useEffect(() => {
    const previo = document.activeElement as HTMLElement | null;
    cerrar.current?.focus();
    const alTeclear = (evento: KeyboardEvent) => {
      if (evento.key === 'Escape') {
        evento.stopPropagation();
        cierre.current();
      } else if (evento.key === 'Tab') {
        // El único elemento enfocable es el botón de cerrar: el foco no sale del diálogo.
        evento.preventDefault();
        cerrar.current?.focus();
      }
    };
    document.addEventListener('keydown', alTeclear, true);
    return () => {
      document.removeEventListener('keydown', alTeclear, true);
      previo?.focus();
    };
  }, []);

  return (
    <div
      className="photo-viewer-backdrop"
      onClick={(evento) => {
        if (evento.target === evento.currentTarget) alCerrar();
      }}
    >
      <div className="photo-viewer" role="dialog" aria-modal="true" aria-label="La foto en grande">
        <button
          ref={cerrar}
          type="button"
          className="photo-viewer-close"
          aria-label="Cerrar"
          onClick={alCerrar}
        >
          <Icon name="close" />
        </button>
        <img src={src} alt={alt} />
      </div>
    </div>
  );
}
