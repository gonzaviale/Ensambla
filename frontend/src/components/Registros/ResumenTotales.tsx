import { useId } from 'react';
import { Panel } from '@/components/ui';
import { cn } from '@/lib/cn';
import { formatearValor } from '@/lib/valores';
import type { Campo } from '@/types/proyecto';
import type { TotalesRegistros } from '@/types/registro';

type ResumenTotalesProps = {
  campos: readonly Campo[];
  totales: TotalesRegistros;
  /** Atenúa el resumen mientras llegan los totales de una búsqueda nueva. */
  actualizando?: boolean;
};

// Los mismos totales que el pie de la tabla, para el celular: sin tabla no hay dónde apoyarlos.
export function ResumenTotales({ campos, totales, actualizando }: ResumenTotalesProps) {
  const titulo = useId();
  return (
    <Panel
      aria-labelledby={titulo}
      className={cn('flex flex-col gap-2 p-4', actualizando && 'opacity-60')}
    >
      <h3 id={titulo} className="m-0 text-h3">
        Totales
      </h3>
      <dl className="m-0 grid grid-cols-1 gap-2">
        <div className="flex flex-wrap gap-x-2">
          <dt className="text-hint text-soft">Filas:</dt>
          <dd className="m-0 font-semibold">{totales.cantidad}</dd>
        </div>
        {campos
          .filter((campo) => campo.tipo === 'NUMERO')
          .map((campo) => (
            <div key={campo.id} className="flex flex-wrap gap-x-2">
              <dt className="text-hint text-soft">{campo.nombre}:</dt>
              <dd className="m-0 font-semibold">
                {formatearValor('NUMERO', totales.sumas[campo.id])}
              </dd>
            </div>
          ))}
      </dl>
    </Panel>
  );
}
