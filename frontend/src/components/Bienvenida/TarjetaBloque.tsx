import type { CSSProperties, ReactNode } from 'react';
import {
  ClipboardTextIcon,
  CookingPotIcon,
  HandCoinsIcon,
  PackageIcon,
  ReceiptIcon,
  WalletIcon,
} from '@phosphor-icons/react';
import { cn } from '@/lib/cn';
import type { PiezaId } from '@/types/negocio';

const ICONOS = {
  productos: PackageIcon,
  ventas: ReceiptIcon,
  gastos: WalletIcon,
  fiados: HandCoinsIcon,
  pedidos: ClipboardTextIcon,
  insumos: CookingPotIcon,
} as const;

type TarjetaBloqueProps = {
  id: PiezaId;
  nombre: string;
  /** Por qué se sumó, en una frase. */
  motivo?: string;
  /** Qué respuestas abre, en una frase. */
  detalle?: ReactNode;
  /** Botón de la tarjeta (sacar, sumar…). */
  accion?: ReactNode;
  /** Se ve apagada: queda para después. */
  apagada?: boolean;
  /** Orden de llegada: cada tarjeta se acomoda un poco después de la anterior. */
  orden?: number;
};

// Un bloque «armado»: la unidad de Ensambla. Se acomoda con un fundido corto (la regla global de
// `prefers-reduced-motion` lo deja quieto). Va dentro de una lista (`<ul>`).
export function TarjetaBloque({
  id,
  nombre,
  motivo,
  detalle,
  accion,
  apagada,
  orden = 0,
}: TarjetaBloqueProps) {
  const Icono = ICONOS[id];
  const estilo: CSSProperties = { animationDelay: `${orden * 90}ms` };
  return (
    <li
      style={estilo}
      className={cn(
        'flex animate-fade-in flex-col gap-3 rounded-lg border border-line/60 bg-surface p-4 shadow-sm [animation-fill-mode:backwards]',
        apagada && 'border-dashed bg-canvas opacity-80 shadow-none',
      )}
    >
      <div className="flex items-start gap-3">
        <span
          aria-hidden
          className="flex size-12 shrink-0 items-center justify-center rounded-lg bg-accent-100 text-accent-800"
        >
          <Icono size={28} weight="duotone" />
        </span>
        <div className="flex min-w-0 flex-1 flex-col gap-1">
          <h3 className="m-0 text-body font-bold break-words">{nombre}</h3>
          {motivo && <p className="m-0 text-hint text-soft">{motivo}</p>}
          {detalle && <p className="m-0 text-hint text-fg">{detalle}</p>}
        </div>
      </div>
      {accion && <div className="flex flex-wrap gap-2">{accion}</div>}
    </li>
  );
}
