import { useState } from 'react';
import { SelectorRelacion } from '@/components/FormRenderer/SelectorRelacion';
import { Button, Sheet } from '@/components/ui';
import type { Conexion } from '@/hooks/lotes/useConexiones';
import { useEtiquetaDeFila } from '@/hooks/registros/queries';
import { cn } from '@/lib/cn';
import type { CeldaRevision, ColumnaRevision } from '@/lib/revisionLectura';

type Props = {
  id: string;
  columna: ColumnaRevision;
  celda: CeldaRevision;
  numeroFila: number;
  conexion?: Conexion;
  invalida: boolean;
  dudosa: boolean;
  avisoId?: string;
  tamano: 'compacta' | 'normal';
  onCambiar: (texto: string) => void;
  onEnfocar: () => void;
};

// Una columna que se conecta con otra tabla: la celda muestra el nombre de la fila elegida y al
// tocarla se abre el mismo buscador que usa el formulario de registros. El valor que se guarda es el
// id de la fila; la lectura lo completa sola cuando el nombre escrito coincide con una sola fila.
export function CeldaConexion({
  id,
  columna,
  celda,
  numeroFila,
  conexion,
  invalida,
  dudosa,
  avisoId,
  tamano,
  onCambiar,
  onEnfocar,
}: Props) {
  const [eligiendo, setEligiendo] = useState(false);
  const valor = celda.texto.trim();
  // Hasta saber a qué tabla apunta la columna no se pide la etiqueta de la fila.
  const etiqueta = useEtiquetaDeFila(
    conexion?.campo.bloqueRelacionadoId ?? '',
    conexion ? valor : '',
  );
  const texto = !valor
    ? 'Elegir fila…'
    : !conexion || etiqueta.isPending
      ? 'Cargando…'
      : (etiqueta.data ?? 'Fila eliminada');

  return (
    <>
      <button
        id={id}
        type="button"
        disabled={!conexion}
        aria-label={`${columna.nombre}, fila ${numeroFila}: ${valor ? texto : 'sin elegir'}`}
        aria-invalid={invalida ? true : undefined}
        aria-describedby={avisoId}
        onFocus={onEnfocar}
        onClick={() => setEligiendo(true)}
        className={cn(
          'w-full cursor-pointer rounded-md border bg-surface px-3 text-left text-fg hover:border-fg disabled:cursor-not-allowed',
          tamano === 'compacta' ? 'min-h-10 py-1.5 text-base' : 'min-h-12 py-2.5 text-body',
          !valor && 'text-soft',
          invalida
            ? 'border-2 border-bad'
            : dudosa
              ? 'border-2 border-warn bg-amber-100'
              : 'border-fg/55',
        )}
      >
        {texto}
      </button>
      {eligiendo && conexion && (
        <Sheet
          open
          onClose={() => setEligiendo(false)}
          title={`Elegir ${columna.nombre}`}
          description={`Fila ${numeroFila} de lo que leímos`}
          footer={
            <Button variant="solid" onClick={() => setEligiendo(false)}>
              Listo
            </Button>
          }
        >
          <SelectorRelacion
            campo={conexion.campo}
            nombreTabla={conexion.nombreTabla}
            valor={valor}
            onChange={onCambiar}
          />
        </Sheet>
      )}
    </>
  );
}
