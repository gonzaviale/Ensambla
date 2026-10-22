import { useState } from 'react';
import {
  camposDeLasTarjetas,
  camposSinDecidir,
  cantidadDeDudas,
  type AccionDeRevision,
  type FilaRevisada,
  type Revision,
} from '@/lib/sistema/revision';
import type { Bloque, ErroresFila, Sistema } from '@/lib/sistema/tipos';
import { Btn } from '../Btn';
import { Icon } from '../Icon';
import { CamposNuevosDetectados } from './CamposNuevosDetectados';
import { TarjetaDeRegistro } from './TarjetaDeRegistro';

type Props = {
  revision: Revision;
  destino: Bloque;
  sistema: Sistema | undefined;
  /** Lo que hay que corregir, por id de registro. Sólo se muestra al intentar incorporar. */
  errores: Record<string, ErroresFila>;
  onAccion: (accion: AccionDeRevision) => void;
};

type FilaQuitada = { fila: FilaRevisada; indice: number };

const textoDeDudas = (dudas: number) =>
  dudas === 0
    ? 'No quedan datos por revisar.'
    : dudas === 1
      ? 'Queda 1 dato por revisar.'
      : `Quedan ${dudas} datos por revisar.`;

// Lo que leyó la foto: las columnas nuevas que el bloque no tiene (si hay) y un registro por tarjeta.
export function RevisionDeLectura({ revision, destino, sistema, errores, onAccion }: Props) {
  const [quitada, setQuitada] = useState<FilaQuitada>();
  const campos = camposDeLasTarjetas(destino.fields, revision);
  const clavesPendientes = new Set(camposSinDecidir(revision).map((campo) => campo.clave));

  const quitar = (fila: FilaRevisada, indice: number) => {
    onAccion({ tipo: 'quitarFila', filaId: fila.id });
    setQuitada({ fila, indice });
  };

  const deshacer = () => {
    if (quitada) onAccion({ tipo: 'restaurarFila', fila: quitada.fila, indice: quitada.indice });
    setQuitada(undefined);
  };

  return (
    <>
      <p className="revision-estado" role="status">
        <span>
          <Icon name="note" />
          {revision.filas.length === 1 ? '1 registro' : `${revision.filas.length} registros`}
        </span>
        <span>{textoDeDudas(cantidadDeDudas(revision))}</span>
      </p>

      <CamposNuevosDetectados revision={revision} existentes={destino.fields} onAccion={onAccion} />

      {quitada && (
        <div className="revision-deshacer" role="status">
          <span>Quitaste el registro {quitada.indice + 1}.</span>
          <Btn cls="secondary small" onClick={deshacer}>
            <Icon name="undo" /> Deshacer
          </Btn>
        </div>
      )}

      {revision.filas.length ? (
        <div className="tarjetas-registro">
          {revision.filas.map((fila, indice) => (
            <TarjetaDeRegistro
              key={fila.id}
              numero={indice + 1}
              fila={fila}
              campos={campos}
              clavesPendientes={clavesPendientes}
              sistema={sistema}
              errores={errores[fila.id]}
              onEditar={(clave, valor) =>
                onAccion({ tipo: 'editar', filaId: fila.id, clave, valor })
              }
              onEstaBien={(clave) => onAccion({ tipo: 'estaBien', filaId: fila.id, clave })}
              onQuitar={() => quitar(fila, indice)}
            />
          ))}
        </div>
      ) : (
        <p className="revision-vacia">
          No queda ningún registro. Deshacé el último o volvé a la imagen.
        </p>
      )}
    </>
  );
}
