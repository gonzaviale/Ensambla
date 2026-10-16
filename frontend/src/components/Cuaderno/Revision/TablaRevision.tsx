import { PencilSimpleIcon, TrashIcon } from '@phosphor-icons/react';
import { Button } from '@/components/ui';
import { cn } from '@/lib/cn';
import {
  claveErrorServidor,
  errorDeCelda,
  errorDeColumna,
  esDudosa,
  idCelda,
  type AccionRevision,
  type BorradorRevision,
  type PosicionCelda,
} from '@/lib/revisionLectura';
import { AvisoCelda } from './AvisoCelda';
import { avisoDeCelda } from './avisosRevision';
import type { Conexion } from '@/hooks/lotes/useConexiones';
import { CeldaRevision } from './CeldaRevision';

type Props = {
  borrador: BorradorRevision;
  /** Por clave de columna: a qué tabla apunta cada columna de conexión. */
  conexiones: Record<string, Conexion>;
  columnaEditando?: string;
  /** Errores que devolvió el servidor, por `${filaId}:${clave}`. */
  erroresServidor: Record<string, string>;
  onAccion: (accion: AccionRevision) => void;
  onEditarColumna: (clave: string) => void;
  onEnfocarCelda: (posicion: PosicionCelda, pagina: number | null) => void;
  onQuitarFila: (filaId: string) => void;
};

// Escritorio (>= lg): los datos leídos en una tabla, cada celda se edita en el lugar. Los avisos de
// duda o error van en una columna de estado para que las celdas no cambien de alto, y la primera
// columna (renglón y hoja) queda fija al desplazar hacia los costados.
export function TablaRevision({
  borrador,
  conexiones,
  columnaEditando,
  erroresServidor,
  onAccion,
  onEditarColumna,
  onEnfocarCelda,
  onQuitarFila,
}: Props) {
  const { columnas, filas } = borrador;

  return (
    <div className="max-h-[70dvh] overflow-auto rounded-xl border border-line/70 bg-surface">
      <table className="w-full border-collapse text-left text-hint">
        <caption className="sr-only">{borrador.nombreBloque}</caption>
        <thead className="sticky top-0 z-10 bg-petrol-100 shadow-[0_1px_0_var(--color-line)]">
          <tr>
            <th scope="col" className="sticky left-0 z-20 bg-petrol-100 px-3 py-3 text-petrol">
              Renglón
            </th>
            {columnas.map((columna) => (
              <th
                key={columna.clave}
                scope="col"
                aria-invalid={errorDeColumna(columna, columnas) ? true : undefined}
                className={cn(
                  'min-w-36 px-2 py-3 align-bottom font-semibold text-petrol',
                  columnaEditando === columna.clave && 'bg-blue-100',
                )}
              >
                <div className="flex flex-col items-start gap-1">
                  <span className="break-words">{columna.nombre || 'Sin nombre'}</span>
                  <Button
                    variant="ghost"
                    size="compact"
                    aria-label={`Editar la columna «${columna.nombre}»`}
                    onClick={() => onEditarColumna(columna.clave)}
                  >
                    <PencilSimpleIcon aria-hidden size={18} />
                    Editar
                  </Button>
                </div>
              </th>
            ))}
            <th scope="col" className="min-w-52 px-3 py-2 text-soft">
              Para revisar
            </th>
            <th scope="col" className="px-2 py-2">
              <span className="sr-only">Acciones de la fila</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {filas.map((fila, indice) => {
            const numero = indice + 1;
            const avisos = columnas.flatMap((columna) => {
              const celda = fila.celdas[columna.clave];
              const aviso =
                celda &&
                avisoDeCelda(
                  columna,
                  celda,
                  erroresServidor[claveErrorServidor(fila.id, columna.clave)],
                );
              return aviso ? [{ columna, aviso }] : [];
            });
            return (
              <tr key={fila.id} className="border-t border-line/70 align-top hover:bg-cream">
                <th
                  scope="row"
                  className="sticky left-0 z-10 bg-surface px-3 py-3 text-left font-semibold whitespace-nowrap"
                >
                  {numero}
                  <span className="block text-caption font-normal text-muted">
                    {fila.pagina ? `Hoja ${fila.pagina}` : 'A mano'}
                  </span>
                </th>
                {columnas.map((columna) => {
                  const celda = fila.celdas[columna.clave];
                  const posicion = { filaId: fila.id, clave: columna.clave };
                  const conAviso = avisos.some((a) => a.columna.clave === columna.clave);
                  const errorServidor = erroresServidor[claveErrorServidor(fila.id, columna.clave)];
                  return (
                    <td key={columna.clave} className="px-2 py-2">
                      {celda && (
                        <CeldaRevision
                          id={idCelda(posicion)}
                          columna={columna}
                          celda={celda}
                          numeroFila={numero}
                          tamano="compacta"
                          conexion={conexiones[columna.clave]}
                          invalida={errorDeCelda(columna, celda) !== undefined || !!errorServidor}
                          dudosa={esDudosa(celda)}
                          avisoId={conAviso ? `aviso-${idCelda(posicion)}` : undefined}
                          onCambiar={(texto) =>
                            onAccion({ tipo: 'editarCelda', ...posicion, texto })
                          }
                          onEnfocar={() => onEnfocarCelda(posicion, fila.pagina)}
                        />
                      )}
                    </td>
                  );
                })}
                <td className="px-3 py-2">
                  <ul className="m-0 flex list-none flex-col gap-2 p-0">
                    {avisos.map(({ columna, aviso }) => {
                      const posicion = { filaId: fila.id, clave: columna.clave };
                      return (
                        <li key={columna.clave}>
                          <AvisoCelda
                            id={`aviso-${idCelda(posicion)}`}
                            aviso={aviso}
                            prefijo={columna.nombre}
                            donde={`${columna.nombre}, fila ${numero}`}
                            onEstaBien={() => onAccion({ tipo: 'marcarRevisada', ...posicion })}
                          />
                        </li>
                      );
                    })}
                  </ul>
                </td>
                <td className="px-2 py-2">
                  <Button
                    variant="ghost"
                    size="compact"
                    aria-label={`Quitar la fila ${numero}`}
                    onClick={() => onQuitarFila(fila.id)}
                  >
                    <TrashIcon aria-hidden size={18} />
                    Quitar
                  </Button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
