import { useState } from 'react';
import { OpcionesEditor } from '@/components/Constructor/OpcionesEditor';
import { TipoDeDatoCards } from '@/components/Constructor/TipoDeDatoCards';
import { Button, ConfirmDialog, InputField, Sheet } from '@/components/ui';
import { errorDeColumna, type ColumnaRevision } from '@/lib/revisionLectura';
import { TIPOS_CAMPO, type TipoCampo } from '@/types/campo';

// Lo que se lee del cuaderno no puede conectarse con otra tabla ni calcularse: eso se arma desde Armar.
const TIPOS_DEL_CUADERNO = TIPOS_CAMPO.filter((tipo) => tipo !== 'RELACION' && tipo !== 'FORMULA');

type Props = {
  columna: ColumnaRevision;
  columnas: ColumnaRevision[];
  bloqueada: boolean;
  nombreTabla: string;
  onCambiar: (cambios: Partial<Pick<ColumnaRevision, 'nombre' | 'tipo' | 'opciones'>>) => void;
  onQuitar: () => void;
  onCerrar: () => void;
};

// Corrige una columna: cómo se llama, qué tipo de dato guarda y, si es una lista, sus opciones.
// Vive en una hoja anclada a la ventana (abajo en el celular, a la derecha en escritorio): se abre
// a la vista y devuelve el foco al botón que la abrió.
export function EditorColumna({
  columna,
  columnas,
  bloqueada,
  nombreTabla,
  onCambiar,
  onQuitar,
  onCerrar,
}: Props) {
  const [confirmando, setConfirmando] = useState(false);
  const error = errorDeColumna(columna, columnas);
  const errorDeOpciones = error && columna.tipo === 'OPCION' && /opci[oó]n/i.test(error);

  function cambiarTipo(tipo: TipoCampo) {
    const sinOpciones = tipo === 'OPCION' && columna.opciones.length === 0;
    onCambiar({ tipo, ...(sinOpciones ? { opciones: ['', ''] } : {}) });
  }

  return (
    <>
      <Sheet
        open
        onClose={onCerrar}
        title="Editar columna"
        description={`«${columna.nombre || 'sin nombre'}»`}
        footer={
          <>
            <Button variant="danger" onClick={() => setConfirmando(true)}>
              Quitar columna
            </Button>
            <Button variant="solid" onClick={onCerrar}>
              Listo
            </Button>
          </>
        }
      >
        <div className="flex flex-col gap-5">
          {bloqueada && (
            <p className="m-0 rounded-md bg-sand-100 p-3 text-hint text-soft">
              Esta columna ya existe en tu tabla «{nombreTabla}»: no se puede cambiar su nombre ni
              su tipo.
            </p>
          )}
          <InputField
            label="Nombre de la columna"
            data-autofocus
            value={columna.nombre}
            disabled={bloqueada}
            error={errorDeOpciones ? undefined : error}
            onChange={(e) => onCambiar({ nombre: e.target.value })}
          />
          <TipoDeDatoCards
            legend="¿Qué tipo de dato guarda?"
            tipos={TIPOS_DEL_CUADERNO}
            unaColumna
            valor={columna.tipo}
            disabled={bloqueada}
            onCambiar={cambiarTipo}
          />
          {columna.tipo === 'OPCION' && (
            <OpcionesEditor
              opciones={columna.opciones}
              disabled={bloqueada}
              error={errorDeOpciones ? error : undefined}
              onCambiar={(opciones) => onCambiar({ opciones })}
            />
          )}
        </div>
      </Sheet>

      <ConfirmDialog
        open={confirmando}
        titulo={`¿Quitar la columna «${columna.nombre}»?`}
        confirmar="Sí, quitar columna"
        conservar="Conservar columna"
        onConfirmar={onQuitar}
        onConservar={() => setConfirmando(false)}
      >
        <p className="m-0">Se pierden los datos de esta columna en todas las filas.</p>
      </ConfirmDialog>
    </>
  );
}
