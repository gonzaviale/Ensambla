import { useEffect, useRef, useId, type ReactNode } from 'react';
import { Icon } from '@/components/sistema/Icon';

export function DialogoCaja({
  titulo,
  onCerrar,
  children,
  bloqueado = false,
  rol = 'dialog',
}: {
  titulo: string;
  onCerrar: () => void;
  children: ReactNode;
  bloqueado?: boolean;
  rol?: 'dialog' | 'alertdialog';
}) {
  const tituloId = useId();
  const raiz = useRef<HTMLElement>(null);
  const cierre = useRef(onCerrar);
  useEffect(() => {
    cierre.current = onCerrar;
  }, [onCerrar]);
  useEffect(() => {
    const anterior = document.activeElement as HTMLElement | null;
    const scroll = document.documentElement.style.overflow;
    document.documentElement.style.overflow = 'hidden';
    raiz.current?.querySelector<HTMLElement>('input,button')?.focus();
    const teclado = (e: KeyboardEvent) => {
      if ([...document.querySelectorAll('[aria-modal="true"]')].at(-1) !== raiz.current) return;
      if (e.key === 'Escape' && !bloqueado) {
        e.stopPropagation();
        cierre.current();
      }
      if (e.key !== 'Tab') return;
      const elementos = [
        ...(raiz.current?.querySelectorAll<HTMLElement>(
          'input:not(:disabled),button:not(:disabled),textarea',
        ) ?? []),
      ].filter((elemento) => elemento.getClientRects().length > 0);
      const primero = elementos[0],
        ultimo = elementos.at(-1);
      if (e.shiftKey && document.activeElement === primero) {
        e.preventDefault();
        ultimo?.focus();
      }
      if (!e.shiftKey && document.activeElement === ultimo) {
        e.preventDefault();
        primero?.focus();
      }
    };
    document.addEventListener('keydown', teclado, true);
    return () => {
      document.documentElement.style.overflow = scroll;
      document.removeEventListener('keydown', teclado, true);
      if (anterior?.isConnected) anterior.focus();
    };
  }, [bloqueado]);
  return (
    <div className="overlay-backdrop caja-overlay">
      <section
        ref={raiz}
        className="modal caja-dialogo"
        role={rol}
        aria-modal="true"
        aria-labelledby={tituloId}
      >
        <div className="caja-entre caja-dialogo-cabecera">
          <h2 id={tituloId}>{titulo}</h2>
          <button
            type="button"
            className="icon-btn"
            aria-label="Cerrar"
            onClick={onCerrar}
            disabled={bloqueado}
          >
            <Icon name="close" />
          </button>
        </div>
        {children}
      </section>
    </div>
  );
}
