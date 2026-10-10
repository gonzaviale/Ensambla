import { useEffect, useRef, type ReactNode } from 'react';
import { Button } from '@/components/ui';

type Props = {
  titulo: string;
  children: ReactNode;
  confirmar: string;
  conservar: string;
  ocupado: boolean;
  onConfirmar: () => void;
  onConservar: () => void;
};

// Confirmación en el lugar (sin ventana emergente): explica qué se pierde y ofrece las dos salidas
// con verbos claros. Al abrirse lleva el foco al aviso para que los lectores de pantalla lo lean.
export function ConfirmarEliminacion({
  titulo,
  children,
  confirmar,
  conservar,
  ocupado,
  onConfirmar,
  onConservar,
}: Props) {
  const aviso = useRef<HTMLDivElement>(null);
  useEffect(() => {
    aviso.current?.focus();
  }, []);

  return (
    <div
      ref={aviso}
      role="alertdialog"
      aria-label={titulo}
      tabIndex={-1}
      className="flex flex-col gap-3 rounded-md border-2 border-bad/40 bg-surface p-4 focus:outline-none focus-visible:outline-2"
      onKeyDown={(event) => {
        if (event.key === 'Escape' && !ocupado) onConservar();
      }}
    >
      <p className="m-0 font-semibold">{titulo}</p>
      <div className="text-soft">{children}</div>
      <div className="flex flex-wrap gap-2">
        <Button
          className="border-bad text-bad hover:bg-bad/10 hover:text-bad"
          onClick={onConfirmar}
          disabled={ocupado}
        >
          {ocupado ? 'Eliminando…' : confirmar}
        </Button>
        <Button onClick={onConservar} disabled={ocupado}>
          {conservar}
        </Button>
      </div>
    </div>
  );
}
