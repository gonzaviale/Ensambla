import type { ReactNode } from 'react';
import { Icon } from '@/components/sistema/Icon';
import { useDialogoLector } from '@/hooks/escaneo/useDialogoLector';

export function DialogoLector({
  titulo,
  onCerrar,
  children,
}: {
  titulo: string;
  onCerrar: () => void;
  children: ReactNode;
}) {
  const { raiz, tituloId, cancelar } = useDialogoLector(onCerrar);
  return (
    <dialog
      ref={raiz}
      className="escaneo-dialogo-lector"
      aria-labelledby={tituloId}
      onCancel={cancelar}
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
