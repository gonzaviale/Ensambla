import { useState } from 'react';
import { CaretDownIcon, ImageIcon, TrashIcon } from '@phosphor-icons/react';
import { Badge, Button, Panel } from '@/components/ui';
import { cn } from '@/lib/cn';
import {
  claveErrorServidor,
  errorDeCelda,
  esDudosa,
  idCelda,
  type AccionRevision,
  type BorradorRevision,
  type FilaRevision,
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
  erroresServidor: Record<string, string>;
  onAccion: (accion: AccionRevision) => void;
  onEnfocarCelda: (posicion: PosicionCelda, pagina: number | null) => void;
  onQuitarFila: (filaId: string) => void;
  onVerFoto: (pagina: number | null) => void;
};

type Grupo = { clave: string; titulo: string; filas: FilaRevision[] };

// Las filas, agrupadas por hoja del cuaderno (las cargadas a mano van aparte).
function agruparPorHoja(filas: FilaRevision[]): Grupo[] {
  const grupos = new Map<string, Grupo>();
  for (const fila of filas) {
    const clave = fila.pagina === null ? 'mano' : `hoja-${fila.pagina}`;
    const grupo = grupos.get(clave) ?? {
      clave,
      titulo: fila.pagina === null ? 'Agregadas a mano' : `Hoja ${fila.pagina}`,
      filas: [],
    };
    grupo.filas.push(fila);
    grupos.set(clave, grupo);
  }
  return [...grupos.values()];
}

// Celular y tablet (< lg): una tarjeta por renglón, con cada dato apilado, su etiqueta a la vista y
// el aviso de duda justo debajo. Las hojas se pliegan para que una lista larga no abrume.
export function FilasTarjetas({
  borrador,
  conexiones,
  erroresServidor,
  onAccion,
  onEnfocarCelda,
  onQuitarFila,
  onVerFoto,
}: Props) {
  const { columnas, filas } = borrador;
  const grupos = agruparPorHoja(filas);
  const hayMuchas = filas.length > 8;
  const [cerradas, setCerradas] = useState<Record<string, boolean>>({});

  const tienePendientes = (grupo: Grupo) =>
    grupo.filas.some((fila) =>
      columnas.some((columna) => {
        const celda = fila.celdas[columna.clave];
        return (
          celda &&
          (esDudosa(celda) ||
            errorDeCelda(columna, celda) !== undefined ||
            erroresServidor[claveErrorServidor(fila.id, columna.clave)] !== undefined)
        );
      }),
    );

  return (
    <div className="flex flex-col gap-4">
      {grupos.map((grupo) => {
        const pendientes = tienePendientes(grupo);
        // Con muchas filas, sólo se abren por defecto las hojas con algo para revisar.
        const abierta = !(cerradas[grupo.clave] ?? (hayMuchas && !pendientes));
        const panelId = `grupo-${grupo.clave}`;
        return (
          <section key={grupo.clave} aria-label={grupo.titulo} className="flex flex-col gap-3">
            <button
              type="button"
              aria-expanded={abierta}
              aria-controls={panelId}
              onClick={() => setCerradas((c) => ({ ...c, [grupo.clave]: abierta }))}
              className="flex min-h-12 w-full cursor-pointer items-center gap-3 rounded-md bg-fg/6 px-3 text-left text-base font-bold text-fg hover:bg-fg/10"
            >
              <CaretDownIcon
                aria-hidden
                size={20}
                weight="bold"
                className={cn('shrink-0 transition-transform', !abierta && '-rotate-90')}
              />
              <span className="flex-1">
                {grupo.titulo}
                <span className="font-normal text-soft">
                  {' '}
                  · {grupo.filas.length === 1 ? '1 renglón' : `${grupo.filas.length} renglones`}
                </span>
              </span>
              {pendientes && <Badge tone="warn">Para revisar</Badge>}
            </button>
            {abierta && (
              <ol id={panelId} className="m-0 flex list-none flex-col gap-3 p-0">
                {grupo.filas.map((fila) => {
                  const numero = filas.indexOf(fila) + 1;
                  return (
                    <li key={fila.id}>
                      <Panel className="flex flex-col gap-4 p-4">
                        <div className="flex flex-wrap items-center justify-between gap-2">
                          <h3 className="m-0 text-lead">
                            Renglón {numero}
                            <span className="text-soft">
                              {' '}
                              · {fila.pagina ? `Hoja ${fila.pagina}` : 'a mano'}
                            </span>
                          </h3>
                          {fila.pagina !== null && (
                            <Button size="compact" onClick={() => onVerFoto(fila.pagina)}>
                              <ImageIcon aria-hidden size={18} />
                              Ver en la foto
                              <span className="sr-only"> (renglón {numero})</span>
                            </Button>
                          )}
                        </div>
                        {columnas.map((columna) => {
                          const celda = fila.celdas[columna.clave];
                          if (!celda) return null;
                          const posicion = { filaId: fila.id, clave: columna.clave };
                          const errorServidor =
                            erroresServidor[claveErrorServidor(fila.id, columna.clave)];
                          const aviso = avisoDeCelda(columna, celda, errorServidor);
                          const id = idCelda(posicion);
                          return (
                            <div key={columna.clave} className="flex min-w-0 flex-col gap-1.5">
                              <label htmlFor={id} className="text-base font-semibold">
                                {columna.nombre}
                              </label>
                              <CeldaRevision
                                id={id}
                                columna={columna}
                                celda={celda}
                                numeroFila={numero}
                                conexion={conexiones[columna.clave]}
                                invalida={
                                  errorDeCelda(columna, celda) !== undefined || !!errorServidor
                                }
                                dudosa={esDudosa(celda)}
                                avisoId={aviso ? `aviso-${id}` : undefined}
                                onCambiar={(texto) =>
                                  onAccion({ tipo: 'editarCelda', ...posicion, texto })
                                }
                                onEnfocar={() => onEnfocarCelda(posicion, fila.pagina)}
                              />
                              {aviso && (
                                <AvisoCelda
                                  id={`aviso-${id}`}
                                  aviso={aviso}
                                  donde={`${columna.nombre}, fila ${numero}`}
                                  onEstaBien={() =>
                                    onAccion({ tipo: 'marcarRevisada', ...posicion })
                                  }
                                />
                              )}
                            </div>
                          );
                        })}
                        <Button
                          variant="danger"
                          className="self-start"
                          aria-label={`Quitar la fila ${numero}`}
                          onClick={() => onQuitarFila(fila.id)}
                        >
                          <TrashIcon aria-hidden size={20} />
                          Quitar fila
                        </Button>
                      </Panel>
                    </li>
                  );
                })}
              </ol>
            )}
          </section>
        );
      })}
    </div>
  );
}
