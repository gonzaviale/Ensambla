import { Link } from '@tanstack/react-router';
import { ClockIcon, WarningIcon } from '@phosphor-icons/react';
import { Badge, Panel, buttonClasses } from '@/components/ui';
import { useProveedor, useResumenLista } from '@/hooks/red/useRed';
import { haceCuanto } from '@/lib/fechas';
import { plural, porcentaje } from '@/lib/red/formato';
import type { ListaProveedor } from '@/types/red';
import { NOMBRE_ORIGEN_CORTO, useRarosDeLista } from './analisis';

// Una lista que llegó y espera revisión: qué proveedor, cuándo y cuánto cambió, con un solo botón.
export function TarjetaLista({ lista }: { lista: ListaProveedor }) {
  const proveedor = useProveedor(lista.proveedorId);
  const resumen = useResumenLista(lista.id);
  const raros = useRarosDeLista(lista);
  const nombre = proveedor?.nombre ?? 'Tu proveedor';

  return (
    <Panel aria-labelledby={`lista-${lista.id}`} interactive className="flex w-full flex-col gap-4">
      <div className="flex flex-col gap-1">
        <h3 id={`lista-${lista.id}`} className="m-0 text-h3 break-words">
          {nombre}
        </h3>
        <p className="m-0 flex flex-wrap items-center gap-x-2 text-hint text-soft">
          <ClockIcon aria-hidden size={18} />
          Llegó {haceCuanto(lista.recibidaEl)} · por {NOMBRE_ORIGEN_CORTO[lista.origen]}
        </p>
      </div>

      <ul className="m-0 flex list-none flex-col gap-1.5 p-0 text-body">
        <li>
          {resumen && resumen.suben > 0 ? (
            <>
              <strong>{plural(resumen.suben, 'producto sube', 'productos suben')}</strong>,{' '}
              {porcentaje(resumen.aumentoPromedio, { decimales: 1 })} en promedio
            </>
          ) : (
            'Ningún producto sube de precio'
          )}
        </li>
        {resumen && resumen.pendientes > 0 && (
          <li>
            <strong>{plural(resumen.pendientes, 'línea', 'líneas')}</strong> para revisar
          </li>
        )}
      </ul>

      {raros.length > 0 && (
        <Badge tone="warn" icon={<WarningIcon size={16} weight="fill" />} className="self-start">
          {plural(raros.length, 'cambio raro', 'cambios raros')}
        </Badge>
      )}

      <Link
        to="/listas/$listaId"
        params={{ listaId: lista.id }}
        aria-label={`Revisar la lista de ${nombre}`}
        className={buttonClasses({ variant: 'primary', className: 'mt-auto self-start' })}
      >
        Revisar
      </Link>
    </Panel>
  );
}
