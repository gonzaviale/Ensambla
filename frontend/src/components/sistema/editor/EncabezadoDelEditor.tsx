import { Icon } from '../Icon';
import { IconBtn } from '../IconBtn';

type Props = {
  nombre: string;
  /** Hay cambios que todavía no llegaron al servidor. */
  guardando: boolean;
  puedeDeshacer: boolean;
  /** Vuelve al Inicio, donde están el negocio y todas sus secciones. */
  onInicio: () => void;
  onDeshacer: () => void;
};

// Las herramientas de la sección, arriba del lienzo: migas, estado de guardado, deshacer.
// El navbar de arriba es el mismo de todas las pantallas (CabeceraComun).
export function EncabezadoDelEditor({
  nombre,
  guardando,
  puedeDeshacer,
  onInicio,
  onDeshacer,
}: Props) {
  return (
    <div className="editor-herramientas">
      <div className="row">
        <button
          type="button"
          className="editor-crumb"
          data-accion="home"
          title="Volver al inicio"
          onClick={onInicio}
        >
          Inicio / <strong>{nombre}</strong>
        </button>
        <span className="save-status">
          <Icon name="check-circle" />
          {guardando ? 'Guardando…' : 'Guardado'}
        </span>
      </div>
      <div className="row">
        <IconBtn
          name="undo"
          label="Deshacer el último cambio"
          disabled={!puedeDeshacer}
          onClick={onDeshacer}
        />
      </div>
    </div>
  );
}
