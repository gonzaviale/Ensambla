import { useState } from 'react';
import {
  ArrowDownIcon,
  ArrowUpIcon,
  ArrowsDownUpIcon,
  MagnifyingGlassIcon,
  PlusIcon,
  TableIcon,
} from '@phosphor-icons/react';
import { FilaTotales } from '@/components/Registros/FilaTotales';
import { ResumenTotales } from '@/components/Registros/ResumenTotales';
import {
  Button,
  EmptyState,
  ErrorState,
  InputField,
  Panel,
  SelectField,
  Skeleton,
  Spinner,
} from '@/components/ui';
import { useRegistros } from '@/hooks/registros/queries';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import { useEsEscritorio } from '@/hooks/useMediaQuery';
import { cn } from '@/lib/cn';
import { SIN_DATO, formatearMomento, formatearValor } from '@/lib/valores';
import type { Bloque } from '@/types/proyecto';
import type { Registro } from '@/types/registro';

type TablaRegistrosProps = {
  bloque: Bloque;
  seleccionadoId?: string;
  onSeleccionar: (registro: Registro) => void;
  /** Abre la carga de una fila nueva; se ofrece cuando la tabla todavía está vacía. */
  onCargar?: () => void;
};

type Columna = { id: string; nombre: string };

// Cuántas columnas se ven en cada tarjeta del celular; el resto está en «Ver todo».
const COLUMNAS_EN_TARJETA = 3;

// Tabla sin filas: igual se ven las columnas y cómo empezar.
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
      <table className="w-full min-w-max border-collapse text-left text-base">
        <caption className="sr-only">Filas de {bloque.nombre}</caption>
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
              <div className="sticky left-3 flex max-w-xs flex-col items-start gap-3 sm:max-w-md">
                <p className="m-0 text-body font-semibold">
                  Todavía no cargaste nada en {bloque.nombre}.
                </p>
                <p className="m-0 text-hint text-soft">
                  Cada columna es un dato que vas a completar. Lo que cargues va a aparecer acá.
                </p>
                {onCargar && (
                  <Button variant="solid" onClick={onCargar}>
                    <PlusIcon aria-hidden size={20} />
                    Cargar la primera fila
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
  if (direccion === 'asc') return <ArrowUpIcon aria-hidden size={16} />;
  if (direccion === 'desc') return <ArrowDownIcon aria-hidden size={16} />;
  return <ArrowsDownUpIcon aria-hidden size={16} className="text-muted" />;
}

export function TablaRegistros({
  bloque,
  seleccionadoId,
  onSeleccionar,
  onCargar,
}: TablaRegistrosProps) {
  const esEscritorio = useEsEscritorio();
  const [texto, setTexto] = useState('');
  const [orden, setOrden] = useState<Orden>({ columna: COLUMNA_CARGA, direccion: 'desc' });
  const [pagina, setPagina] = useState(0);
  const busqueda = useDebouncedValue(texto.trim(), ESPERA_BUSQUEDA_MS);

  const campos = [...bloque.campos].sort((a, b) => a.posicion - b.posicion);
  const hayNumericos = campos.some((campo) => campo.tipo === 'NUMERO');
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
    return primero ? String(primero) : 'fila';
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
            trailing={<MagnifyingGlassIcon aria-hidden size={22} className="mr-3 text-muted" />}
          />
        </div>
        <div className="flex items-center gap-3 text-hint text-soft">
          {isFetching && !isPending && <Spinner label="Actualizando…" />}
          {data && (
            <span>{data.totalElements === 1 ? '1 fila' : `${data.totalElements} filas`}</span>
          )}
        </div>
      </div>

      {!esEscritorio && (
        <div className="flex flex-wrap items-end gap-3">
          <div className="min-w-0 flex-1 basis-48">
            <SelectField
              label="Ordenar por"
              value={orden.columna}
              onChange={(event) => {
                setPagina(0);
                setOrden((actual) => ({ ...actual, columna: event.target.value }));
              }}
            >
              {columnas.map((columna) => (
                <option key={columna.id} value={columna.id}>
                  {columna.nombre}
                </option>
              ))}
            </SelectField>
          </div>
          <Button
            onClick={() => {
              setPagina(0);
              setOrden((actual) => ({
                ...actual,
                direccion: actual.direccion === 'asc' ? 'desc' : 'asc',
              }));
            }}
          >
            <IconoOrden direccion={orden.direccion} />
            {orden.direccion === 'asc' ? 'Menor a mayor' : 'Mayor a menor'}
          </Button>
        </div>
      )}

      {isPending ? (
        <div role="status" aria-label="Cargando filas…" className="flex flex-col gap-2">
          <Skeleton className="h-12 w-full" />
          <Skeleton className="h-12 w-full" />
          <Skeleton className="h-12 w-full" />
          <span className="sr-only">Cargando filas…</span>
        </div>
      ) : isError ? (
        <ErrorState
          title="No pudimos cargar las filas"
          message={error.message}
          onRetry={() => void refetch()}
        />
      ) : data.content.length === 0 && !busqueda ? (
        <TablaVacia bloque={bloque} columnas={columnas} onCargar={onCargar} />
      ) : data.content.length === 0 ? (
        <EmptyState icon={<TableIcon aria-hidden size={32} />} title="No hay resultados">
          <p className="m-0">Ninguna fila coincide con «{busqueda}».</p>
        </EmptyState>
      ) : (
        <>
          {esEscritorio ? (
            <div className="max-h-[70dvh] overflow-auto rounded-lg border border-line bg-surface">
              <table className="w-full min-w-max border-collapse text-left text-base">
                <caption className="sr-only">Filas de {bloque.nombre}</caption>
                <thead className="sticky top-0 z-10 bg-surface shadow-[0_1px_0_var(--color-line)]">
                  <tr>
                    {columnas.map((columna, indice) => {
                      const activa = orden.columna === columna.id;
                      return (
                        <th
                          key={columna.id}
                          scope="col"
                          aria-sort={activa ? ariaSort[orden.direccion] : 'none'}
                          className={cn(
                            'px-2 py-1 font-semibold',
                            indice === 0 && 'sticky left-0 z-20 bg-surface',
                          )}
                        >
                          <button
                            type="button"
                            onClick={() => ordenarPor(columna.id)}
                            className="inline-flex min-h-11 cursor-pointer items-center gap-1.5 rounded-md px-2 hover:bg-fg/6"
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
                  {data.content.map((registro) => {
                    const elegido = registro.id === seleccionadoId;
                    return (
                      <tr
                        key={registro.id}
                        data-seleccionada={elegido || undefined}
                        className={cn(
                          'border-b border-line last:border-b-0',
                          elegido && 'bg-accent-100',
                        )}
                      >
                        {campos.map((campo, indice) => (
                          <td
                            key={campo.id}
                            className={cn(
                              'px-3 py-2',
                              indice === 0 && 'sticky left-0 z-10 bg-inherit font-semibold',
                              indice === 0 && !elegido && 'bg-surface',
                            )}
                          >
                            {formatearValor(campo.tipo, registro.valores[campo.id])}
                          </td>
                        ))}
                        <td className="px-3 py-2 whitespace-nowrap text-soft">
                          {formatearMomento(registro.creadoEn) || SIN_DATO}
                        </td>
                        <td className="px-3 py-1 text-right">
                          <Button
                            variant="ghost"
                            size="compact"
                            aria-label={`Ver ficha de ${descripcion(registro)}`}
                            aria-pressed={elegido}
                            onClick={() => onSeleccionar(registro)}
                          >
                            Ver
                          </Button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
                {hayNumericos && (
                  <FilaTotales campos={campos} totales={data.totales} actualizando={isFetching} />
                )}
              </table>
            </div>
          ) : (
            <ul
              aria-label={`Filas de ${bloque.nombre}`}
              className={cn('m-0 flex list-none flex-col gap-3 p-0', isFetching && 'opacity-60')}
            >
              {data.content.map((registro) => {
                const principales = campos.slice(0, COLUMNAS_EN_TARJETA);
                const [primero, ...resto] = principales;
                return (
                  <li key={registro.id}>
                    <Panel
                      className={cn(
                        'flex flex-col gap-3 p-4',
                        registro.id === seleccionadoId && 'border-accent-700 bg-accent-100',
                      )}
                    >
                      {primero && (
                        <h3 className="m-0 text-h3 break-words">
                          {formatearValor(primero.tipo, registro.valores[primero.id])}
                        </h3>
                      )}
                      <dl className="m-0 grid grid-cols-1 gap-2">
                        {resto.map((campo) => (
                          <div key={campo.id} className="flex flex-wrap gap-x-2">
                            <dt className="text-hint text-soft">{campo.nombre}:</dt>
                            <dd className="m-0 font-semibold break-words">
                              {formatearValor(campo.tipo, registro.valores[campo.id])}
                            </dd>
                          </div>
                        ))}
                        <div className="flex flex-wrap gap-x-2 text-caption text-muted">
                          <dt>Cargado:</dt>
                          <dd className="m-0">{formatearMomento(registro.creadoEn) || SIN_DATO}</dd>
                        </div>
                      </dl>
                      <Button
                        className="self-start"
                        aria-label={`Ver ficha de ${descripcion(registro)}`}
                        onClick={() => onSeleccionar(registro)}
                      >
                        Ver todo
                      </Button>
                    </Panel>
                  </li>
                );
              })}
            </ul>
          )}

          {!esEscritorio && hayNumericos && (
            <ResumenTotales campos={campos} totales={data.totales} actualizando={isFetching} />
          )}

          <nav aria-label="Paginación" className="flex items-center justify-between gap-3">
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
