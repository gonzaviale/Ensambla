import type { ReactNode } from 'react';
import { Button } from './Button';
import { Sheet } from './Sheet';

type ConfirmDialogProps = {
  open: boolean;
  titulo: string;
  children: ReactNode;
  confirmar: string;
  conservar: string;
  ocupado?: boolean;
  error?: string;
  tono?: 'peligro' | 'normal';
  onConfirmar: () => void;
  onConservar: () => void;
};

// Confirmación modal: nombra el elemento y lo que se pierde, y ofrece dos salidas con verbos claros.
// El foco empieza en la salida segura ("conservar").
export function ConfirmDialog({
  open,
  titulo,
  children,
  confirmar,
  conservar,
  ocupado = false,
  error,
  tono = 'peligro',
  onConfirmar,
  onConservar,
}: ConfirmDialogProps) {
  return (
    <Sheet
      open={open}
      placement="center"
      role="alertdialog"
      title={titulo}
      locked={ocupado}
      onClose={onConservar}
      footer={
        <>
          <Button data-autofocus onClick={onConservar} disabled={ocupado}>
            {conservar}
          </Button>
          <Button
            variant={tono === 'peligro' ? 'danger-solid' : 'solid'}
            onClick={onConfirmar}
            loading={ocupado}
          >
            {confirmar}
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-3 text-body text-soft">
        {children}
        {error && (
          <p role="alert" className="m-0 font-semibold text-bad">
            {error}
          </p>
        )}
      </div>
    </Sheet>
  );
}
