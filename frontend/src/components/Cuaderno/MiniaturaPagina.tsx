import { ArrowClockwiseIcon, TrashIcon, WarningCircleIcon } from '@phosphor-icons/react';
import { Badge, Button, Spinner } from '@/components/ui';
import type { Rotacion } from '@/lib/imagen';

export type EstadoHoja = 'pendiente' | 'preparando' | 'lista' | 'error';

type MiniaturaPaginaProps = {
  numero: number;
  url: string;
  rotacion: Rotacion;
  estado?: EstadoHoja;
  deshabilitada?: boolean;
  onGirar: () => void;
  onQuitar: () => void;
};

export function MiniaturaPagina({
  numero,
  url,
  rotacion,
  estado = 'pendiente',
  deshabilitada,
  onGirar,
  onQuitar,
}: MiniaturaPaginaProps) {
  return (
    <li className="flex min-w-0 flex-col gap-3 rounded-2xl border border-line/70 bg-surface p-3 shadow-sm">
      <div className="flex aspect-square items-center justify-center overflow-hidden rounded-xl bg-cream">
        <img
          src={url}
          alt={`Hoja ${numero}`}
          style={{ transform: `rotate(${rotacion}deg)` }}
          className="max-h-full max-w-full object-contain transition-transform"
        />
      </div>
      <p className="m-0 text-center text-hint font-bold text-petrol">Hoja {numero}</p>
      {estado === 'preparando' && <Spinner label="Preparando…" />}
      {estado === 'error' && (
        <Badge tone="bad" icon={<WarningCircleIcon size={16} weight="fill" />}>
          No se pudo preparar. Sacala de nuevo.
        </Badge>
      )}
      <div className="grid grid-cols-2 gap-2">
        <Button
          aria-label={`Girar hoja ${numero}`}
          disabled={deshabilitada}
          onClick={onGirar}
          className="px-2"
        >
          <ArrowClockwiseIcon aria-hidden size={22} />
          Girar
        </Button>
        <Button
          aria-label={`Quitar hoja ${numero}`}
          disabled={deshabilitada}
          onClick={onQuitar}
          className="px-2"
        >
          <TrashIcon aria-hidden size={22} />
          Quitar
        </Button>
      </div>
    </li>
  );
}
