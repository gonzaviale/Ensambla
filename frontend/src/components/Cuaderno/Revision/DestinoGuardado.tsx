import { useId, type ReactNode } from 'react';
import { InputField, SelectField } from '@/components/ui';
import { controlOpcion, tarjetaOpcion } from '@/components/Constructor/opcionStyles';
import { useProyectosDisponibles } from '@/hooks/proyectos/queries';
import type { Destino } from '@/lib/revisionLectura';

type Props = {
  destino: Destino;
  /** Tabla del negocio que la IA reconoció como la misma que la del cuaderno; null si propone una nueva. */
  tablaExistente: { id: string; nombre: string } | null;
  /** Nombre de la tabla que se va a crear (no aplica al sumar filas a una existente). */
  nombreTabla: string;
  onCambiarNombreTabla: (nombre: string) => void;
  onCambiar: (destino: Destino) => void;
};

type OpcionProps = {
  name: string;
  titulo: string;
  consecuencia: string;
  checked: boolean;
  disabled?: boolean;
  onElegir: () => void;
  children?: ReactNode;
};

// Cada destino es una tarjeta que dice qué va a pasar si se elige; lo que hay que completar aparece
// dentro de la tarjeta elegida.
function Opcion({
  name,
  titulo,
  consecuencia,
  checked,
  disabled,
  onElegir,
  children,
}: OpcionProps) {
  const id = useId();
  return (
    <div className="flex flex-col gap-3">
      <label className={tarjetaOpcion + (disabled ? ' cursor-not-allowed opacity-60' : '')}>
        <input
          type="radio"
          name={name}
          checked={checked}
          disabled={disabled}
          onChange={onElegir}
          aria-labelledby={`${id}-titulo`}
          aria-describedby={`${id}-consecuencia`}
          className={controlOpcion}
        />
        <span className="flex min-w-0 flex-col">
          <span id={`${id}-titulo`} className="font-semibold">
            {titulo}
          </span>
          <span id={`${id}-consecuencia`} className="text-hint text-soft">
            {consecuencia}
          </span>
        </span>
      </label>
      {checked && children}
    </div>
  );
}

// Paso 2: adónde va lo que se revisó: una tabla que ya existe, un sistema nuevo o uno que ya tiene
// la persona. Acá también se le pone nombre a la tabla.
export function DestinoGuardado({
  destino,
  tablaExistente,
  nombreTabla,
  onCambiarNombreTabla,
  onCambiar,
}: Props) {
  const grupo = useId();
  const proyectos = useProyectosDisponibles();
  const sinProyectos = !proyectos.data || proyectos.data.length === 0;
  const nombreDeTabla = (
    <InputField
      label="Nombre de la tabla"
      value={nombreTabla}
      onChange={(e) => onCambiarNombreTabla(e.target.value)}
    />
  );

  return (
    <fieldset id="destino" className="m-0 flex min-w-0 flex-col gap-3 border-0 p-0">
      <legend className="mb-2 p-0 text-h3 font-bold">¿Dónde lo guardamos?</legend>

      {tablaExistente && (
        <Opcion
          name={grupo}
          titulo={`Sumar las filas a tu tabla «${tablaExistente.nombre}»`}
          consecuencia="La reconocimos porque se llama igual y tiene las mismas columnas."
          checked={destino.modo === 'bloque'}
          onElegir={() => onCambiar({ modo: 'bloque', bloqueId: tablaExistente.id })}
        />
      )}

      <Opcion
        name={grupo}
        titulo="Crear un sistema nuevo"
        consecuencia="Armamos un sistema nuevo con esta tabla adentro."
        checked={destino.modo === 'nuevo'}
        onElegir={() => onCambiar({ modo: 'nuevo', nombreProyecto: '' })}
      >
        {destino.modo === 'nuevo' && (
          <div className="flex flex-col gap-4 pl-1">
            <InputField
              label="Nombre del sistema"
              value={destino.nombreProyecto}
              required
              onChange={(e) => onCambiar({ modo: 'nuevo', nombreProyecto: e.target.value })}
            />
            {nombreDeTabla}
          </div>
        )}
      </Opcion>

      <Opcion
        name={grupo}
        titulo="Agregar una tabla a un sistema que ya tengo"
        consecuencia={
          sinProyectos
            ? 'Todavía no tenés sistemas: elegí crear uno nuevo.'
            : 'Esta tabla se suma a uno de tus sistemas, sin tocar lo que ya cargaste.'
        }
        checked={destino.modo === 'proyecto'}
        disabled={sinProyectos}
        onElegir={() => onCambiar({ modo: 'proyecto', proyectoId: proyectos.data?.[0]?.id ?? '' })}
      >
        {destino.modo === 'proyecto' && (
          <div className="flex flex-col gap-4 pl-1">
            <SelectField
              label="Sistema"
              value={destino.proyectoId}
              onChange={(e) => onCambiar({ modo: 'proyecto', proyectoId: e.target.value })}
            >
              {proyectos.data?.map((proyecto) => (
                <option key={proyecto.id} value={proyecto.id}>
                  {proyecto.nombre}
                </option>
              ))}
            </SelectField>
            {nombreDeTabla}
          </div>
        )}
      </Opcion>
    </fieldset>
  );
}
