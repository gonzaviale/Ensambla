import type { ModoDelEditor } from '@/stores/sistemaStore';
import { Icon } from '../Icon';
import { IconBtn } from '../IconBtn';

type Props = {
  nombre: string;
  modo: ModoDelEditor;
  /** Hay cambios que todavía no llegaron al servidor. */
  guardando: boolean;
  puedeDeshacer: boolean;
  /** Vuelve a la lista de cuadernos. */
  onCuadernos: () => void;
  onModo: (modo: ModoDelEditor) => void;
  onDeshacer: () => void;
};

// Las herramientas del cuaderno, arriba del lienzo: migas, estado de guardado, Construir/Usar y deshacer.
// El navbar de arriba es el mismo de todas las pantallas (CabeceraComun).
export function EncabezadoDelEditor({
  nombre,
  modo,
  guardando,
  puedeDeshacer,
  onCuadernos,
  onModo,
  onDeshacer,
}: Props) {
  return (
    <div className="editor-herramientas">
      <div className="row">
        <button
          type="button"
          className="editor-crumb"
          data-accion="home"
          title="Volver a mis cuadernos"
          onClick={onCuadernos}
        >
          Mis cuadernos / <strong>{nombre}</strong>
        </button>
        <span className="save-status">
          <Icon name="check-circle" />
          {guardando ? 'Guardando…' : 'Guardado'}
        </span>
      </div>
      <div className="row">
        <div className="mode-switch" aria-label="Modo de trabajo">
          <button
            type="button"
            className={modo === 'build' ? 'active' : ''}
            data-accion="mode"
            data-tour="buildMode"
            aria-pressed={modo === 'build'}
            onClick={() => onModo('build')}
          >
            <Icon name="settings" />
            Construir
          </button>
          <button
            type="button"
            className={modo === 'use' ? 'active' : ''}
            data-accion="mode"
            data-tour="useMode"
            aria-pressed={modo === 'use'}
            onClick={() => onModo('use')}
          >
            <Icon name="eye" />
            Usar cuaderno
          </button>
        </div>
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
