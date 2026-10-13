import { useState } from 'react';
import { MagnifyingGlassIcon, PackageIcon } from '@phosphor-icons/react';
import {
  Badge,
  Button,
  EmptyState,
  ErrorState,
  InputField,
  Skeleton,
  Spinner,
} from '@/components/ui';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import { useStock } from '@/hooks/stock/queries';
import { cn } from '@/lib/cn';
import { formatearCantidad } from '@/lib/valores';
import type { Bloque } from '@/types/proyecto';

const TAMANIO_PAGINA = 20;
const ESPERA_BUSQUEDA_MS = 300;

// Cuánto hay de cada producto (cada fila de la tabla elegida): la suma de sus entradas menos sus salidas.
export function TablaStock({ bloque }: { bloque: Bloque }) {
  const [texto, setTexto] = useState('');
  const [pagina, setPagina] = useState(0);
  const busqueda = useDebouncedValue(texto.trim(), ESPERA_BUSQUEDA_MS);
  const { data, isPending, isError, error, isFetching, refetch } = useStock(bloque.id, {
    q: busqueda,
    pagina,
    tamanio: TAMANIO_PAGINA,
  });

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div className="w-full max-w-sm">
          <InputField
            label={`Buscar en ${bloque.nombre}`}
            type="search"
            placeholder="Escribí para filtrar"
            value={texto}
            onChange={(event) => {
              setTexto(event.target.value);
              setPagina(0);
            }}
            trailing={<MagnifyingGlassIcon aria-hidden size={22} className="mr-3 text-muted" />}
          />
        </div>
        {isFetching && !isPending && <Spinner label="Actualizando…" />}
      </div>

      {isPending ? (
        <div role="status" aria-label="Cargando el stock…" className="flex flex-col gap-2">
          <Skeleton className="h-12 w-full" />
          <Skeleton className="h-12 w-full" />
          <span className="sr-only">Cargando el stock…</span>
        </div>
      ) : isError ? (
        <ErrorState
          title="No pudimos cargar el stock"
          message={error.message}
          onRetry={() => void refetch()}
        />
      ) : data.content.length === 0 ? (
        <EmptyState
          icon={<PackageIcon aria-hidden size={32} />}
          title="No hay productos para mostrar"
        >
          <p className="m-0">
            {busqueda
              ? `Ningún producto coincide con «${busqueda}».`
              : `Cargá filas en ${bloque.nombre} para llevar su stock.`}
          </p>
        </EmptyState>
      ) : (
        <>
          <div className="overflow-auto rounded-lg border border-line bg-surface">
            <table className="w-full border-collapse text-left text-base">
              <caption className="sr-only">Stock de {bloque.nombre}</caption>
              <thead className="bg-surface shadow-[0_1px_0_var(--color-line)]">
                <tr>
                  <th scope="col" className="px-3 py-3 font-semibold">
                    Producto
                  </th>
                  <th scope="col" className="px-3 py-3 text-right font-semibold">
                    Stock
                  </th>
                </tr>
              </thead>
              <tbody className={cn(isFetching && 'opacity-60')}>
                {data.content.map((producto) => (
                  <tr key={producto.registroId} className="border-b border-line last:border-b-0">
                    <th scope="row" className="px-3 py-3 font-semibold break-words">
                      {producto.producto}
                    </th>
                    <td className="px-3 py-3 text-right whitespace-nowrap">
                      {producto.stock === 0 ? (
                        <Badge tone="neutral">Sin stock</Badge>
                      ) : (
                        <span className="font-semibold">{formatearCantidad(producto.stock)}</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <nav
            aria-label="Paginación del stock"
            className="flex items-center justify-between gap-3"
          >
            <Button
              disabled={data.first}
              onClick={() => setPagina((actual) => Math.max(0, actual - 1))}
            >
              Anterior
            </Button>
            <span className="text-hint text-soft">
              Página {data.pageNumber + 1} de {data.totalPages}
            </span>
            <Button disabled={data.last} onClick={() => setPagina((actual) => actual + 1)}>
              Siguiente
            </Button>
          </nav>
        </>
      )}
    </div>
  );
}
