import { PencilSimpleIcon, PlusIcon, TrashIcon } from '@phosphor-icons/react';
import { Button } from '@/components/ui';
import { cn } from '@/lib/cn';
import {
  claveErrorServidor,
  errorDeColumna,
  idCelda,
  type AccionRevision,
  type BorradorRevision,
  type PosicionCelda,
} from '@/lib/revisionLectura';
import { CeldaRevision } from './CeldaRevision';

type Props = {
  borrador: BorradorRevision;
  columnaEditando?: string;
  /** Errores que devolvió el servidor, por `${filaId}:${clave}`. */
  erroresServidor: Record<string, string>;
  onAccion: (accion: AccionRevision) => void;
  onEditarColumna: (clave: string) => void;
  onEnfocarCelda: (posicion: PosicionCelda, pagina: number | null) => void;
};

// Los datos leídos, listos para corregir: cada celda se edita en el lugar.
export function TablaRevision({
  borrador,
  columnaEditando,
  erroresServidor,
  onAccion,
  onEditarColumna,
  onEnfocarCelda,
}: Props) {
  const { columnas, filas } = borrador;

  return (
    <div className="flex flex-col gap-3">
      <div className="overflow-x-auto">
        <table className="w-full border-collapse text-left text-[14px]">
          <caption className="sr-only">{borrador.nombreBloque}</caption>
          <thead>
            <tr>
              <th scope="col" className="px-2 py-1.5 text-soft">
                Pág.
              </th>
              {columnas.map((columna) => (
                <th
                  key={columna.clave}
                  scope="col"
                  aria-invalid={errorDeColumna(columna, columnas) ? true : undefined}
                  className={cn(
                    'px-2 py-1.5 align-bottom font-semibold text-soft',
                    columnaEditando === columna.clave && 'bg-accent/10',
                  )}
                >
                  <span className="flex items-center gap-1">
                    {columna.nombre || 'Sin nombre'}
                    <Button
                      size="sm"
                      variant="ghost"
                      aria-label={`Editar la columna «${columna.nombre}»`}
                      onClick={() => onEditarColumna(columna.clave)}
                    >
                      <PencilSimpleIcon aria-hidden size={14} />
                    </Button>
                  </span>
                </th>
              ))}
              <th scope="col" className="px-2 py-1.5">
                <span className="sr-only">Acciones de la fila</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {filas.map((fila, indice) => (
              <tr key={fila.id} className="border-t border-line align-top">
                <td className="px-2 py-2 text-soft">{fila.pagina ?? '—'}</td>
                {columnas.map((columna) => {
                  const celda = fila.celdas[columna.clave];
                  const posicion = { filaId: fila.id, clave: columna.clave };
                  return (
                    <td key={columna.clave} className="px-2 py-1.5">
                      {celda && (
                        <CeldaRevision
                          id={idCelda(posicion)}
                          columna={columna}
                          celda={celda}
                          numeroFila={indice + 1}
                          errorServidor={erroresServidor[claveErrorServidor(fila.id, columna.clave)]}
                          onCambiar={(texto) =>
                            onAccion({ tipo: 'editarCelda', ...posicion, texto })
                          }
                          onEnfocar={() => onEnfocarCelda(posicion, fila.pagina)}
                        />
                      )}
                    </td>
                  );
                })}
                <td className="px-2 py-1.5">
                  <Button
                    size="sm"
                    variant="ghost"
                    aria-label={`Quitar la fila ${indice + 1}`}
                    onClick={() => onAccion({ tipo: 'quitarFila', filaId: fila.id })}
                  >
                    <TrashIcon aria-hidden size={16} />
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {filas.length === 0 && (
        <p className="m-0 text-soft">
          No quedan filas. Agregá una a mano o volvé a subir las fotos.
        </p>
      )}

      <div className="flex flex-wrap gap-2">
        <Button
          size="sm"
          disabled={columnas.length === 0}
          onClick={() => onAccion({ tipo: 'agregarFila', id: crypto.randomUUID() })}
        >
          <PlusIcon aria-hidden size={16} />
          Agregar fila
        </Button>
        <Button size="sm" onClick={() => onAccion({ tipo: 'agregarColumna' })}>
          <PlusIcon aria-hidden size={16} />
          Agregar columna
        </Button>
      </div>
    </div>
  );
}
