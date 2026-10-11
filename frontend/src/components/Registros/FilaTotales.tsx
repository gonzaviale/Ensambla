import { cn } from '@/lib/cn';
import { formatearValor } from '@/lib/valores';
import type { Campo } from '@/types/proyecto';
import type { TotalesRegistros } from '@/types/registro';

type FilaTotalesProps = {
  /** Los campos de las columnas de la tabla, en el mismo orden. */
  campos: readonly Campo[];
  totales: TotalesRegistros;
  /** Atenúa la fila mientras llegan los totales de una búsqueda nueva. */
  actualizando?: boolean;
};

// Pie de la tabla: cuántas filas hay y cuánto suma cada columna numérica, de toda la búsqueda y no
// sólo de la página que se ve.
export function FilaTotales({ campos, totales, actualizando }: FilaTotalesProps) {
  const filas = totales.cantidad === 1 ? '1 fila' : `${totales.cantidad} filas`;
  return (
    <tfoot
      className={cn(
        'sticky bottom-0 z-10 bg-surface shadow-[0_-1px_0_var(--color-line)]',
        actualizando && 'opacity-60',
      )}
    >
      <tr>
        {campos.map((campo, indice) => {
          const suma = campo.tipo === 'NUMERO' ? totales.sumas[campo.id] : undefined;
          const total = suma === undefined ? null : formatearValor('NUMERO', suma);
          if (indice === 0) {
            return (
              <th
                key={campo.id}
                scope="row"
                className="sticky left-0 z-20 bg-surface px-3 py-2 text-left font-semibold"
              >
                <span className="block">Totales de {filas}</span>
                {total && <span className="block">{total}</span>}
              </th>
            );
          }
          return (
            <td key={campo.id} className="px-3 py-2 font-semibold">
              {total}
            </td>
          );
        })}
        <td colSpan={2} />
      </tr>
    </tfoot>
  );
}
