import { useState } from 'react';
import { HandCoinsIcon, ReceiptIcon, ShoppingBagIcon } from '@phosphor-icons/react';
import { Button, InputField } from '@/components/ui';
import { pesos } from '@/lib/formato';
import type { FilaRelato } from '@/types/negocio';

export type FilaPorConfirmar = {
  id: string;
  fila: FilaRelato;
  /** Lo que la persona ve en el campo de monto (puede estar corregido). */
  monto: string;
};

const TIPOS = {
  ventas: { etiqueta: 'Venta', articulo: 'la venta', Icono: ShoppingBagIcon },
  gastos: { etiqueta: 'Gasto', articulo: 'el gasto', Icono: ReceiptIcon },
  fiados: { etiqueta: 'Fiado', articulo: 'el fiado', Icono: HandCoinsIcon },
} as const;

/** «Venta · 3 Velas · $ 4.500»: lo que se va a guardar, en una línea. */
function descripcionDeFila(fila: FilaRelato, monto: number | null): string {
  const { etiqueta } = TIPOS[fila.pieza];
  const detalle =
    fila.pieza === 'ventas'
      ? `${fila.valores.cantidad ?? 1} ${fila.valores.producto ?? ''}`.trim()
      : fila.pieza === 'gastos'
        ? (fila.valores.concepto ?? '')
        : (fila.valores.cliente ?? '');
  return [etiqueta, detalle, monto === null ? 'Falta el monto' : pesos(monto)]
    .filter(Boolean)
    .join(' · ');
}

type Props = {
  item: FilaPorConfirmar;
  /** El monto escrito como número, o `null` si no es válido. */
  montoValido: number | null;
  /** Esta fila va a una pieza que el sistema todavía no tiene. */
  sinPieza: boolean;
  onMonto: (monto: string) => void;
  onQuitar: () => void;
};

// Una cosa entendida: ícono + etiqueta, con «Corregir monto» y «Quitar». Nada se guarda hasta confirmar.
export function FilaEntendida({ item, montoValido, sinPieza, onMonto, onQuitar }: Props) {
  const [corrigiendo, setCorrigiendo] = useState(false);
  const { Icono, articulo } = TIPOS[item.fila.pieza];
  const descripcion = descripcionDeFila(item.fila, montoValido);
  const invalido = montoValido === null;

  return (
    <li className="flex flex-col gap-3 rounded-lg border border-line/60 bg-surface p-4 shadow-sm">
      <div className="flex items-start gap-3">
        <span className="flex size-12 shrink-0 items-center justify-center rounded-full bg-sand-100 text-fg">
          <Icono aria-hidden size={26} />
        </span>
        <div className="flex min-w-0 flex-1 flex-col gap-1">
          <p className="m-0 text-body font-semibold break-words">{descripcion}</p>
          {sinPieza && (
            <p className="m-0 text-hint text-soft">Todavía no tenés dónde guardar esto.</p>
          )}
        </div>
      </div>
      {(corrigiendo || invalido) && (
        <InputField
          label={`Monto de ${articulo}`}
          inputMode="decimal"
          value={item.monto}
          onChange={(event) => onMonto(event.target.value)}
          error={invalido ? 'Poné un monto mayor a cero, por ejemplo 4500.' : undefined}
        />
      )}
      <div className="flex flex-wrap gap-2">
        {!corrigiendo && !invalido && (
          <Button
            variant="ghost"
            size="compact"
            aria-label={`Corregir el monto: ${descripcion}`}
            onClick={() => setCorrigiendo(true)}
          >
            Corregir monto
          </Button>
        )}
        <Button
          variant="ghost"
          size="compact"
          aria-label={`Quitar: ${descripcion}`}
          onClick={onQuitar}
        >
          Quitar
        </Button>
      </div>
    </li>
  );
}
