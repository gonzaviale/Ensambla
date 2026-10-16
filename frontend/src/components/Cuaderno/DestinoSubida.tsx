import { useState } from 'react';
import { SelectField } from '@/components/ui';
import { useBloques } from '@/hooks/bloques/queries';
import { useProyectosDisponibles } from '@/hooks/proyectos/queries';

type Props = {
  /** Tabla elegida; undefined si la persona deja que la IA arme una nueva con lo que lea. */
  bloqueId: string | undefined;
  onCambiar: (bloqueId: string | undefined) => void;
  disabled?: boolean;
};

type TablasProps = Props & { proyectoId: string };

// Las tablas del sistema elegido. Vive aparte para pedirlas recién cuando hay un sistema.
function SelectorTabla({ proyectoId, bloqueId, onCambiar, disabled }: TablasProps) {
  const bloques = useBloques(proyectoId);
  return (
    <SelectField
      label="Tabla"
      value={bloqueId ?? ''}
      disabled={disabled || bloques.isPending}
      error={bloques.isError ? 'No pudimos cargar las tablas. Probá de nuevo.' : undefined}
      hint="Si no elegís una tabla, armamos una nueva con lo que leamos."
      onChange={(e) => onCambiar(e.target.value || undefined)}
    >
      <option value="">Elegí una tabla</option>
      {bloques.data?.map((bloque) => (
        <option key={bloque.id} value={bloque.id}>
          {bloque.nombre}
        </option>
      ))}
    </SelectField>
  );
}

// Antes de subir las fotos: ¿son filas de una tabla que ya tengo? Así la IA no tiene que adivinar
// a cuál sumarlas. Es opcional: sin elegir nada, propone una tabla nueva como siempre.
export function DestinoSubida({ bloqueId, onCambiar, disabled }: Props) {
  const proyectos = useProyectosDisponibles();
  const [proyectoId, setProyectoId] = useState('');

  if (!proyectos.data || proyectos.data.length === 0) return null;

  return (
    <section
      aria-labelledby="destino-subida-titulo"
      className="flex flex-col gap-4 rounded-2xl border border-line/70 bg-surface p-5 sm:p-6"
    >
      <h2 id="destino-subida-titulo" className="m-0 text-h3 text-petrol">
        ¿Dónde van estas hojas?
      </h2>
      <div className="grid gap-4 sm:grid-cols-2">
        <SelectField
          label="Sistema"
          value={proyectoId}
          disabled={disabled}
          onChange={(e) => {
            setProyectoId(e.target.value);
            onCambiar(undefined);
          }}
        >
          <option value="">Una tabla nueva</option>
          {proyectos.data.map((proyecto) => (
            <option key={proyecto.id} value={proyecto.id}>
              {proyecto.nombre}
            </option>
          ))}
        </SelectField>
        {proyectoId && (
          <SelectorTabla
            proyectoId={proyectoId}
            bloqueId={bloqueId}
            onCambiar={onCambiar}
            disabled={disabled}
          />
        )}
      </div>
    </section>
  );
}
