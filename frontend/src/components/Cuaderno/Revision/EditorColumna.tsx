import { useState } from 'react';
import { ConfirmarEliminacion } from '@/components/Constructor/ConfirmarEliminacion';
import { TIPOS_DE_DATO } from '@/components/Constructor/tiposDeDato';
import { Button, InputField, Panel, TextareaField } from '@/components/ui';
import { SelectField } from '@/components/ui/SelectField';
import { errorDeColumna, type ColumnaRevision } from '@/lib/revisionLectura';
import { TIPOS_CAMPO, type TipoCampo } from '@/types/campo';

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

  return (
    <Panel
      flat
      aria-label={`Editar la columna «${columna.nombre}»`}
      className="flex flex-col gap-3"
    >
      <h3 className="m-0 text-hint">Columna «{columna.nombre || 'sin nombre'}»</h3>
      {bloqueada && (
        <p className="m-0 text-hint text-soft">
          Esta columna ya existe en tu tabla «{nombreTabla}»: no se puede cambiar su nombre ni su
          tipo.
        </p>
      )}
      <InputField
        label="Nombre de la columna"
        value={columna.nombre}
        disabled={bloqueada}
        error={errorDeOpciones ? undefined : error}
        onChange={(e) => onCambiar({ nombre: e.target.value })}
      />
      <SelectField
        label="Qué tipo de dato guarda"
        value={columna.tipo}
        disabled={bloqueada}
        hint={TIPOS_DE_DATO[columna.tipo].ayuda}
        onChange={(e) => onCambiar({ tipo: e.target.value as TipoCampo })}
      >
        {TIPOS_CAMPO.map((tipo) => (
          <option key={tipo} value={tipo}>
            {TIPOS_DE_DATO[tipo].nombre}
          </option>
        ))}
      </SelectField>
      {columna.tipo === 'OPCION' && (
        <TextareaField
          label="Opciones (una por renglón)"
          className="min-h-24"
          disabled={bloqueada}
          value={columna.opciones.join('\n')}
          error={errorDeOpciones ? error : undefined}
          onChange={(e) => onCambiar({ opciones: e.target.value.split('\n') })}
        />
      )}

      {confirmando ? (
        <ConfirmarEliminacion
          titulo={`¿Quitar la columna «${columna.nombre}»?`}
          confirmar="Sí, quitar columna"
          conservar="Conservar columna"
          ocupado={false}
          onConfirmar={onQuitar}
          onConservar={() => setConfirmando(false)}
        >
          Se pierden los datos de esta columna en todas las filas.
        </ConfirmarEliminacion>
      ) : (
        <div className="flex flex-wrap gap-2">
          <Button variant="primary" onClick={onCerrar}>
            Listo
          </Button>
          <Button variant="ghost" onClick={() => setConfirmando(true)}>
            Quitar columna
          </Button>
        </div>
      )}
    </Panel>
  );
}
