import type { ReactNode } from 'react';
import { Icon } from './Icon';
import { Logo } from './Logo';

export type SeccionActiva = 'home' | 'templates' | 'zone';

type TopBarProps = {
  active?: SeccionActiva;
  nombre: string;
  sistemas: number;
  /** La campana de avisos, que vive en el encabezado. */
  campana?: ReactNode;
  onInicio: () => void;
  onSistemas: () => void;
  onPlantillas: () => void;
  onZona: () => void;
  onAyuda: () => void;
  onPerfil: () => void;
};

// Cabecera común de inicio, plantillas, taller y zona. Igual al mockup; «Proveedores cerca» pasa a «Mi zona».
export function TopBar({
  active = 'home',
  nombre,
  sistemas,
  campana,
  onInicio,
  onSistemas,
  onPlantillas,
  onZona,
  onAyuda,
  onPerfil,
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
        <button type="button" onClick={onSistemas}>
          Mi Negocio{sistemas ? ` · ${sistemas}` : ''}
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
      </nav>
      <div className="row topbar-right">
        {campana}
        <button type="button" className="help-trigger" onClick={onAyuda}>
          <Icon name="help" />
          <span>¿Necesitás una mano?</span>
        </button>
        <button
          type="button"
          className="row profile"
          aria-label="Cambiar tu nombre"
          onClick={onPerfil}
        >
          <span>
            <span className="profile-name">{nombre}</span>
            <span className="profile-label">Tu espacio personal</span>
          </span>
          <span className="avatar">{nombre.slice(0, 2).toUpperCase()}</span>
        </button>
      </div>
    </header>
  );
}
