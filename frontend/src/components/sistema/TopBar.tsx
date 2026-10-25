import type { ReactNode } from 'react';
import { Icon } from './Icon';
import { Logo } from './Logo';
import { MenuDePerfil } from './MenuDePerfil';

export type SeccionActiva =
  'home' | 'sistemas' | 'templates' | 'zone' | 'avisos' | 'plans' | 'account';

type TopBarProps = {
  active?: SeccionActiva;
  nombre: string;
  /** La campana de avisos, que vive en el encabezado. */
  campana?: ReactNode;
  onInicio: () => void;
  onSistemas: () => void;
  onPlantillas: () => void;
  onZona: () => void;
  onAvisos: () => void;
  onPlanes: () => void;
  onAyuda: () => void;
  /** «Configuración», en el menú del avatar. */
  onCuenta: () => void;
  onCerrarSesion: () => void;
  cerrandoSesion?: boolean;
};

// Cabecera común de inicio, plantillas, taller y zona. Igual al mockup; «Proveedores cerca» pasa a «Mi zona».
export function TopBar({
  active = 'home',
  nombre,
  campana,
  onInicio,
  onSistemas,
  onPlantillas,
  onZona,
  onAvisos,
  onPlanes,
  onAyuda,
  onCuenta,
  onCerrarSesion,
  cerrandoSesion = false,
}: TopBarProps) {
  return (
    <header className="topbar">
      <Logo onClick={onInicio} />
      <nav className="topnav" aria-label="Principal">
        <button
          type="button"
          className={active === 'home' ? 'active' : ''}
          data-accion="home"
          onClick={onInicio}
        >
          Inicio
        </button>
        <button
          type="button"
          className={active === 'sistemas' ? 'active' : ''}
          onClick={onSistemas}
        >
          Mis cuadernos
        </button>
        <button
          type="button"
          className={active === 'avisos' ? 'active' : ''}
          aria-current={active === 'avisos' ? 'page' : undefined}
          onClick={onAvisos}
        >
          Avisos
        </button>
        <button
          type="button"
          className={active === 'templates' ? 'active' : ''}
          onClick={onPlantillas}
        >
          Plantillas
        </button>
        <button type="button" className={active === 'zone' ? 'active' : ''} onClick={onZona}>
          Mi zona
        </button>
        <button
          type="button"
          className={active === 'plans' ? 'active' : ''}
          aria-current={active === 'plans' ? 'page' : undefined}
          onClick={onPlanes}
        >
          Planes
        </button>
      </nav>
      <div className="row topbar-right">
        {campana}
        <button
          type="button"
          className="help-trigger"
          aria-label="¿Necesitás una mano?"
          title="¿Necesitás una mano?"
          onClick={onAyuda}
        >
          <Icon name="help" />
        </button>
        <MenuDePerfil
          nombre={nombre}
          enCuenta={active === 'account'}
          cerrando={cerrandoSesion}
          onConfiguracion={onCuenta}
          onCerrarSesion={onCerrarSesion}
        />
      </div>
    </header>
  );
}
