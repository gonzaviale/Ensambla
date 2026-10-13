import { DownloadSimpleIcon, FileXlsIcon } from '@phosphor-icons/react';
import { Button, EmptyState, ErrorState, Skeleton, Spinner } from '@/components/ui';
import { useDescargarReporte } from '@/hooks/reportes/mutations';
import { useReporte } from '@/hooks/reportes/queries';
import { leerErrorServidor } from '@/hooks/errores';
import { useEtiquetas } from '@/hooks/registros/queries';
import { cn } from '@/lib/cn';
import { formatearMomento, formatearValor, SIN_DATO } from '@/lib/valores';
import type { Bloque } from '@/types/proyecto';
import type { FiltroReporte } from '@/types/reporte';

type Props = {
  bloque: Bloque;
  /** Los filtros aplicados: los mismos para lo que se ve, los totales y el archivo que se descarga. */
  filtro: FiltroReporte;
};

const formatoNumero = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 2 });

// Las primeras filas del reporte, los totales de todas las que cumplen el filtro y la descarga en XLSX.
export function ResultadoReporte({ bloque, filtro }: Props) {
  const { data, isPending, isError, error, isFetching, refetch } = useReporte(bloque.id, filtro);
  const descargar = useDescargarReporte(bloque.id);
  const campos = [...bloque.campos].sort((a, b) => a.posicion - b.posicion);
  const numericos = campos.filter((campo) => campo.tipo === 'NUMERO');
  const etiquetaDe = useEtiquetas(campos, data?.filas ?? []);
  const errorDescarga = leerErrorServidor(descargar.error);

  if (isPending) {
    return (
      <div role="status" aria-label="Armando el reporte…" className="flex flex-col gap-2">
        <Skeleton className="h-16 w-full" />
        <Skeleton className="h-12 w-full" />
        <span className="sr-only">Armando el reporte…</span>
      </div>
    );
  }
  if (isError) {
    return (
      <ErrorState
        title="No pudimos armar el reporte"
        message={error.message}
        onRetry={() => void refetch()}
      />
    );
  }

  const cantidad = data.totales.cantidad;

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="m-0 text-lead font-semibold">
          {cantidad === 1 ? '1 fila' : `${cantidad} filas`} en el reporte
        </p>
        <div className="flex items-center gap-3">
          {isFetching && <Spinner label="Actualizando…" />}
          <Button
            variant="solid"
            loading={descargar.isPending}
            disabled={cantidad === 0}
            onClick={() => descargar.mutate(filtro)}
          >
            {!descargar.isPending && <DownloadSimpleIcon aria-hidden size={22} />}
            {descargar.isPending ? 'Preparando el archivo…' : 'Descargar Excel (XLSX)'}
          </Button>
        </div>
      </div>

      {errorDescarga && (
        <p role="alert" className="m-0 font-semibold text-bad">
          {errorDescarga.mensaje}
        </p>
      )}

      {numericos.length > 0 && (
        <dl
          aria-label="Totales del reporte"
          className="m-0 grid gap-3 sm:grid-cols-2 lg:grid-cols-4"
        >
          {numericos.map((campo) => (
            <div
              key={campo.id}
              className="flex flex-col gap-0.5 rounded-lg border border-line bg-accent-100 p-4"
            >
              <dt className="text-hint text-soft">Total de {campo.nombre}</dt>
              <dd className="m-0 text-h3 font-bold">
                {formatoNumero.format(data.totales.sumas[campo.id] ?? 0)}
              </dd>
            </div>
          ))}
        </dl>
      )}

      {cantidad === 0 ? (
        <EmptyState
          icon={<FileXlsIcon aria-hidden size={32} />}
          title="No hay filas para este reporte"
        >
          <p className="m-0">Ninguna fila cumple los filtros elegidos. Probá con otros.</p>
        </EmptyState>
      ) : (
        <>
          {data.truncado && (
            <p role="status" className="m-0 text-hint text-soft">
              Se muestran las primeras {data.maximoFilas} filas de {cantidad}. Los totales y el
              archivo incluyen todas.
            </p>
          )}
          <div className="overflow-auto rounded-lg border border-line bg-surface">
            <table className="w-full min-w-max border-collapse text-left text-base">
              <caption className="sr-only">Filas del reporte de {bloque.nombre}</caption>
              <thead className="bg-surface shadow-[0_1px_0_var(--color-line)]">
                <tr>
                  {campos.map((campo) => (
                    <th key={campo.id} scope="col" className="px-3 py-3 font-semibold">
                      {campo.nombre}
                    </th>
                  ))}
                  <th scope="col" className="px-3 py-3 font-semibold">
                    Cargado
                  </th>
                </tr>
              </thead>
              <tbody className={cn(isFetching && 'opacity-60')}>
                {data.filas.map((fila) => (
                  <tr key={fila.id} className="border-b border-line last:border-b-0">
                    {campos.map((campo) => (
                      <td key={campo.id} className="px-3 py-2">
                        {formatearValor(
                          campo.tipo,
                          fila.valores[campo.id],
                          etiquetaDe(campo, fila.valores[campo.id]),
                        )}
                      </td>
                    ))}
                    <td className="px-3 py-2 whitespace-nowrap text-soft">
                      {formatearMomento(fila.creadoEn) || SIN_DATO}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}
