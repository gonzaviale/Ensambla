import { useState } from 'react';
import {
  ArrowDownIcon,
  ArrowUpIcon,
  ArrowsDownUpIcon,
  MagnifyingGlassIcon,
  PlusIcon,
  TableIcon,
} from '@phosphor-icons/react';
import { Button, EmptyState, ErrorState, InputField, Spinner } from '@/components/ui';
import { useRegistros } from '@/hooks/registros/queries';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import { cn } from '@/lib/cn';
import { SIN_DATO, formatearMomento, formatearValor } from '@/lib/valores';
import type { Bloque } from '@/types/proyecto';
import type { Registro } from '@/types/registro';

type TablaRegistrosProps = {
  bloque: Bloque;
  seleccionadoId?: string;
  onSeleccionar: (registro: Registro) => void;
  /** Abre la carga de un registro nuevo; se ofrece cuando el bloque todavía está vacío. */
  onCargar?: () => void;
};

type Columna = { id: string; nombre: string };

// Bloque sin registros: igual se ven las columnas (los datos del bloque) y cómo empezar.
function TablaVacia({
  bloque,
  columnas,
  onCargar,
}: {
  bloque: Bloque;
  columnas: Columna[];
  onCargar?: () => void;
}) {
  return (
    <div className="overflow-x-auto rounded-lg border border-line bg-surface">
      <table className="w-full min-w-max border-collapse text-left text-sm">
        <caption className="sr-only">Registros de {bloque.nombre}</caption>
        <thead>
          <tr className="border-b border-line">
            {columnas.map((columna) => (
              <th key={columna.id} scope="col" className="px-3 py-2 font-semibold">
                {columna.nombre}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          <tr>
            <td colSpan={columnas.length} className="px-3 py-8">
              <div className="sticky left-0 flex max-w-[calc(100vw-4rem)] flex-col items-start gap-3 sm:max-w-md">
                <p className="m-0 text-base font-semibold">
                  Todavía no cargaste nada en {bloque.nombre}.
                </p>
                <p className="m-0 text-[15px] text-soft">
                  Cada columna es un dato que vas a completar. Lo que cargues va a aparecer acá.
                </p>
                {onCargar && (
                  <Button variant="solid" onClick={onCargar}>
                    <PlusIcon aria-hidden size={20} />
                    Cargar el primer registro
                  </Button>
                )}
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  );
}

type Orden = { columna: string; direccion: 'asc' | 'desc' };

const ESPERA_BUSQUEDA_MS = 300;
const TAMANIO_PAGINA = 20;
const COLUMNA_CARGA = 'creadoEn';

const ariaSort = { asc: 'ascending', desc: 'descending' } as const;

function IconoOrden({ direccion }: { direccion?: Orden['direccion'] }) {
  if (direccion === 'asc') return <ArrowUpIcon aria-hidden size={14} />;
  if (direccion === 'desc') return <ArrowDownIcon aria-hidden size={14} />;
  return <ArrowsDownUpIcon aria-hidden size={14} className="text-muted" />;
}

export function TablaRegistros({
  bloque,
  seleccionadoId,
  onSeleccionar,
  onCargar,
}: TablaRegistrosProps) {
  const [texto, setTexto] = useState('');
  const [orden, setOrden] = useState<Orden>({ columna: COLUMNA_CARGA, direccion: 'desc' });
  const [pagina, setPagina] = useState(0);
  const busqueda = useDebouncedValue(texto.trim(), ESPERA_BUSQUEDA_MS);

  const campos = [...bloque.campos].sort((a, b) => a.posicion - b.posicion);
  const columnas: Columna[] = [
    ...campos.map((campo) => ({ id: campo.id, nombre: campo.nombre })),
    { id: COLUMNA_CARGA, nombre: 'Cargado' },
  ];

  const { data, isPending, isError, error, isFetching, refetch } = useRegistros(bloque.id, {
    q: busqueda,
    orden: orden.columna,
    direccion: orden.direccion,
    pagina,
    tamanio: TAMANIO_PAGINA,
  });

  function ordenarPor(columna: string) {
    setPagina(0);
    setOrden((actual) =>
      actual.columna === columna
        ? { columna, direccion: actual.direccion === 'asc' ? 'desc' : 'asc' }
        : { columna, direccion: 'asc' },
    );
  }

  function descripcion(registro: Registro) {
    const primero = campos.map((campo) => registro.valores[campo.id]).find((valor) => valor);
    return primero ? String(primero) : 'registro';
  }

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
            trailing={<MagnifyingGlassIcon aria-hidden size={16} className="mr-2 text-muted" />}
          />
        </div>
        <div className="flex items-center gap-3 text-[13px] text-soft">
          {isFetching && !isPending && <Spinner label="Actualizando…" />}
          {data && (
            <span>
              {data.totalElements === 1 ? '1 registro' : `${data.totalElements} registros`}
            </span>
          )}
        </div>
      </div>

      {isPending ? (
        <div className="flex justify-center py-12">
          <Spinner label="Cargando registros…" />
        </div>
      ) : isError ? (
        <ErrorState
          title="No pudimos cargar los registros"
          message={error.message}
          onRetry={() => void refetch()}
        />
      ) : data.content.length === 0 && !busqueda ? (
        <TablaVacia bloque={bloque} columnas={columnas} onCargar={onCargar} />
      ) : data.content.length === 0 ? (
        <EmptyState icon={<TableIcon aria-hidden size={32} />} title="No hay resultados">
          <p>Ningún registro coincide con «{busqueda}».</p>
        </EmptyState>
      ) : (
        <>
          <div className="overflow-x-auto rounded-lg border border-line bg-surface">
            <table className="w-full min-w-max border-collapse text-left text-sm">
              <caption className="sr-only">Registros de {bloque.nombre}</caption>
              <thead>
                <tr className="border-b border-line">
                  {columnas.map((columna) => {
                    const activa = orden.columna === columna.id;
                    return (
                      <th
                        key={columna.id}
                        scope="col"
                        aria-sort={activa ? ariaSort[orden.direccion] : 'none'}
                        className="px-3 py-2 font-semibold"
                      >
                        <button
                          type="button"
                          onClick={() => ordenarPor(columna.id)}
                          className="inline-flex cursor-pointer items-center gap-1.5 rounded-md px-1 py-0.5 hover:bg-fg/6 focus-visible:outline-2 focus-visible:outline-accent"
                        >
                          {columna.nombre}
                          <IconoOrden direccion={activa ? orden.direccion : undefined} />
                        </button>
                      </th>
                    );
                  })}
                  <th scope="col" className="px-3 py-2">
                    <span className="sr-only">Acciones</span>
                  </th>
                </tr>
              </thead>
              <tbody className={cn(isFetching && 'opacity-60')}>
                {data.content.map((registro) => (
                  <tr
                    key={registro.id}
                    aria-selected={registro.id === seleccionadoId || undefined}
                    className={cn(
                      'border-b border-line last:border-b-0',
                      registro.id === seleccionadoId && 'bg-accent/10',
                    )}
                  >
                    {campos.map((campo) => (
                      <td key={campo.id} className="px-3 py-2">
                        {formatearValor(campo.tipo, registro.valores[campo.id])}
                      </td>
                    ))}
                    <td className="px-3 py-2 whitespace-nowrap text-soft">
                      {formatearMomento(registro.creadoEn) || SIN_DATO}
                    </td>
                    <td className="px-3 py-2 text-right">
                      <Button
                        variant="ghost"
                        size="sm"
                        aria-label={`Ver ficha de ${descripcion(registro)}`}
                        onClick={() => onSeleccionar(registro)}
                      >
                        Ver
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <nav aria-label="Paginación" className="flex items-center justify-between gap-3">
            <Button
              size="sm"
              disabled={data.first}
              onClick={() => setPagina((actual) => Math.max(0, actual - 1))}
            >
              Anterior
            </Button>
            <span className="text-[13px] text-soft">
              Página {data.pageNumber + 1} de {data.totalPages}
            </span>
            <Button
              size="sm"
              disabled={data.last}
              onClick={() => setPagina((actual) => actual + 1)}
            >
              Siguiente
            </Button>
          </nav>
        </>
      )}
    </div>
  );
}
