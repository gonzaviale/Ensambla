import { ArrowClockwiseIcon, TrashIcon } from '@phosphor-icons/react';
import { Button } from '@/components/ui';
import type { Rotacion } from '@/lib/imagen';

type MiniaturaPaginaProps = {
  numero: number;
  url: string;
  rotacion: Rotacion;
  deshabilitada?: boolean;
  onGirar: () => void;
  onQuitar: () => void;
};

export function MiniaturaPagina({
  numero,
  url,
  rotacion,
  deshabilitada,
  onGirar,
  onQuitar,
}: MiniaturaPaginaProps) {
  return (
    <li className="flex flex-col gap-2 rounded-lg border border-line bg-surface p-2">
      <div className="flex aspect-square items-center justify-center overflow-hidden rounded-md bg-canvas">
        <img
          src={url}
          alt={`Página ${numero}`}
          style={{ transform: `rotate(${rotacion}deg)` }}
          className="max-h-full max-w-full object-contain transition-transform"
        />
      </div>
      <p className="m-0 text-center text-hint font-semibold">Página {numero}</p>
      <div className="grid grid-cols-2 gap-2">
        <Button aria-label={`Girar página ${numero}`} disabled={deshabilitada} onClick={onGirar}>
          <ArrowClockwiseIcon aria-hidden size={20} />
          Girar
        </Button>
        <Button aria-label={`Quitar página ${numero}`} disabled={deshabilitada} onClick={onQuitar}>
          <TrashIcon aria-hidden size={20} />
          Quitar
        </Button>
      </div>
    </li>
  );
}
