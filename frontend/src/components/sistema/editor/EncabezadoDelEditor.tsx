import type { ReactNode } from 'react';
import type { ModoDelEditor } from '@/stores/sistemaStore';
import { Icon } from '../Icon';
import { IconBtn } from '../IconBtn';
import { Logo } from '../Logo';

type Props = {
  nombre: string;
  modo: ModoDelEditor;
  /** Hay cambios que todavía no llegaron al servidor. */
  guardando: boolean;
  puedeDeshacer: boolean;
  campana?: ReactNode;
  onInicio: () => void;
  onModo: (modo: ModoDelEditor) => void;
  onDeshacer: () => void;
};

// La barra de arriba del editor: migas, estado de guardado, Construir/Usar, deshacer y avisos.
export function EncabezadoDelEditor({
  nombre,
  modo,
  guardando,
  puedeDeshacer,
  campana,
  onInicio,
  onModo,
  onDeshacer,
}: Props) {
  return (
    <header className="topbar editor-topbar">
      <div className="row">
        <Logo onClick={onInicio} />
        <span className="editor-divider" />
        <button
          type="button"
          className="editor-crumb"
          data-accion="home"
          title="Volver a mis sistemas"
          onClick={onInicio}
        >
          Mis sistemas / <strong>{nombre}</strong>
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
            Usar sistema
          </button>
        </div>
        <IconBtn
          name="undo"
          label="Deshacer el último cambio"
          disabled={!puedeDeshacer}
          onClick={onDeshacer}
        />
        {campana}
      </div>
    </header>
  );
}
