import { useEffect, useId, useRef, type ReactNode } from 'react';
import { Icon } from '@/components/sistema/Icon';

export function DialogoLector({
  titulo,
  onCerrar,
  children,
}: {
  titulo: string;
  onCerrar: () => void;
  children: ReactNode;
}) {
  const raiz = useRef<HTMLDialogElement>(null);
  const tituloId = useId();
  useEffect(() => {
    const dialogo = raiz.current;
    const anterior = document.activeElement as HTMLElement | null;
    const overflow = document.documentElement.style.overflow;
    document.documentElement.style.overflow = 'hidden';
    dialogo?.showModal();
    dialogo?.querySelector<HTMLInputElement>('input')?.focus({ preventScroll: true });
    return () => {
      dialogo?.close();
      document.documentElement.style.overflow = overflow;
      if (anterior?.isConnected) anterior.focus();
    };
  }, []);
  return (
    <dialog
      ref={raiz}
      className="escaneo-dialogo-lector"
      aria-labelledby={tituloId}
      onCancel={(e) => {
        e.preventDefault();
        onCerrar();
      }}
    >
      <header>
        <h2 id={tituloId}>{titulo}</h2>
        <button type="button" className="icon-btn" aria-label="Cerrar lector" onClick={onCerrar}>
          <Icon name="close" />
        </button>
      </header>
      {children}
    </dialog>
  );
}
