import { useId, type ReactNode } from 'react';
import { useDialogoCaja } from '@/hooks/caja/useDialogoCaja';
import { Icon } from '@/components/sistema/Icon';

export function DialogoCaja({
  titulo,
  onCerrar,
  children,
  bloqueado = false,
  rol = 'dialog',
  clase = '',
}: {
  titulo: string;
  onCerrar: () => void;
  children: ReactNode;
  bloqueado?: boolean;
  rol?: 'dialog' | 'alertdialog';
  clase?: string;
}) {
  const tituloId = useId();
  const raiz = useDialogoCaja(onCerrar, bloqueado);
  return (
    <div className="overlay-backdrop caja-overlay">
      <section
        ref={raiz}
        tabIndex={-1}
        className={`modal caja-dialogo ${clase}`}
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
