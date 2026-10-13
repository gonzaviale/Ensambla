import { ArrowDownIcon, ArrowUpIcon, ArrowsLeftRightIcon } from '@phosphor-icons/react';
import { EmptyState, ErrorState, Skeleton } from '@/components/ui';
import { useMovimientos } from '@/hooks/stock/queries';
import { cn } from '@/lib/cn';
import { formatearCantidad } from '@/lib/valores';
import type { Bloque } from '@/types/proyecto';

const formatoFecha = (iso: string) => {
  const [anio, mes, dia] = iso.split('-');
  return anio && mes && dia ? `${dia}/${mes}/${anio}` : iso;
};

// Los últimos movimientos de la tabla, los más nuevos primero.
export function ListaMovimientos({ bloque }: { bloque: Bloque }) {
  const { data, isPending, isError, error, refetch } = useMovimientos(bloque.id);

  if (isPending) {
    return (
      <div role="status" aria-label="Cargando los movimientos…" className="flex flex-col gap-2">
        <Skeleton className="h-12 w-full" />
        <span className="sr-only">Cargando los movimientos…</span>
      </div>
    );
  }
  if (isError) {
    return (
      <ErrorState
        title="No pudimos cargar los movimientos"
        message={error.message}
        onRetry={() => void refetch()}
      />
    );
  }
  if (data.length === 0) {
    return (
      <EmptyState
        icon={<ArrowsLeftRightIcon aria-hidden size={32} />}
        title="Todavía no hay movimientos"
      >
        <p className="m-0">Cuando registres una entrada o una salida, la vas a ver acá.</p>
      </EmptyState>
    );
  }

  return (
    <ul aria-label="Últimos movimientos" className="m-0 flex list-none flex-col gap-2 p-0">
      {data.map((movimiento) => {
        const entrada = movimiento.tipo === 'ENTRADA';
        const Icono = entrada ? ArrowDownIcon : ArrowUpIcon;
        return (
          <li
            key={movimiento.id}
            className="flex flex-wrap items-center gap-x-4 gap-y-1 rounded-lg border border-line bg-surface px-4 py-3"
          >
            <span
              className={cn(
                'inline-flex items-center gap-1.5 font-semibold',
                entrada ? 'text-ok' : 'text-bad',
              )}
            >
              <Icono aria-hidden size={20} weight="bold" />
              {entrada ? 'Entrada' : 'Salida'} de {formatearCantidad(movimiento.cantidad)}
            </span>
            <span className="min-w-0 font-semibold break-words">{movimiento.producto}</span>
            <span className="text-hint text-soft">{formatoFecha(movimiento.fecha)}</span>
            {movimiento.nota && <span className="text-hint text-soft">{movimiento.nota}</span>}
          </li>
        );
      })}
    </ul>
  );
}
