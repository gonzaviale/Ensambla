import { useId } from 'react';
import { InputField } from '@/components/ui';
import { SelectField } from '@/components/ui/SelectField';
import { useProyectosDisponibles } from '@/hooks/proyectos/queries';
import type { Destino } from '@/lib/revisionLectura';

type Props = {
  destino: Destino;
  /** Tabla del negocio que la IA reconoció como la misma que la del cuaderno; null si propone una nueva. */
  tablaExistente: { id: string; nombre: string } | null;
  onCambiar: (destino: Destino) => void;
};

const radio = 'mt-1 size-4 accent-[var(--color-accent)]';

// Adónde va lo que se revisó: una tabla que ya existe, un proyecto nuevo o uno que ya tiene la persona.
export function DestinoGuardado({ destino, tablaExistente, onCambiar }: Props) {
  const grupo = useId();
  const proyectos = useProyectosDisponibles();
  const sinProyectos = !proyectos.data || proyectos.data.length === 0;

  return (
    <fieldset className="m-0 flex flex-col gap-3 rounded-lg border border-line p-4">
      <legend className="px-1 text-hint font-semibold">¿Dónde lo guardamos?</legend>

      {tablaExistente && (
        <label className="flex items-start gap-2">
          <input
            type="radio"
            name={grupo}
            className={radio}
            checked={destino.modo === 'bloque'}
            onChange={() => onCambiar({ modo: 'bloque', bloqueId: tablaExistente.id })}
          />
          <span>
            Sumar las filas a tu tabla «{tablaExistente.nombre}»
            <span className="block text-hint text-soft">
              La reconocimos porque se llama igual y tiene las mismas columnas.
            </span>
          </span>
        </label>
      )}

      <label className="flex items-start gap-2">
        <input
          type="radio"
          name={grupo}
          className={radio}
          checked={destino.modo === 'nuevo'}
          onChange={() => onCambiar({ modo: 'nuevo', nombreProyecto: '' })}
        />
        <span>Crear un proyecto nuevo</span>
      </label>
      {destino.modo === 'nuevo' && (
        <InputField
          label="Nombre del proyecto"
          value={destino.nombreProyecto}
          onChange={(e) => onCambiar({ modo: 'nuevo', nombreProyecto: e.target.value })}
        />
      )}

      <label className="flex items-start gap-2">
        <input
          type="radio"
          name={grupo}
          className={radio}
          checked={destino.modo === 'proyecto'}
          disabled={sinProyectos}
          onChange={() =>
            onCambiar({ modo: 'proyecto', proyectoId: proyectos.data?.[0]?.id ?? '' })
          }
        />
        <span>Agregar una tabla a un proyecto que ya tengo</span>
      </label>
      {destino.modo === 'proyecto' && (
        <SelectField
          label="Proyecto"
          value={destino.proyectoId}
          onChange={(e) => onCambiar({ modo: 'proyecto', proyectoId: e.target.value })}
        >
          {proyectos.data?.map((proyecto) => (
            <option key={proyecto.id} value={proyecto.id}>
              {proyecto.nombre}
            </option>
          ))}
        </SelectField>
      )}
    </fieldset>
  );
}
