import { PlusIcon } from '@phosphor-icons/react';
import { Button } from '@/components/ui';
import { pieza } from '@/lib/catalogo/piezas';
import type { PiezaId } from '@/types/negocio';

type Props = {
  id: PiezaId;
  sumando: PiezaId | null;
  onSumar: (id: PiezaId) => void;
  variant?: 'solid' | 'primary';
};

// «Sumar Gastos»: suma la pieza que falta. Una sola se suma a la vez.
export function BotonSumarPieza({ id, sumando, onSumar, variant = 'primary' }: Props) {
  return (
    <Button
      variant={variant}
      loading={sumando === id}
      disabled={sumando !== null && sumando !== id}
      onClick={() => onSumar(id)}
    >
      <PlusIcon aria-hidden size={20} weight="bold" />
      Sumar {pieza(id).nombre}
    </Button>
  );
}
