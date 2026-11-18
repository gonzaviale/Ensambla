import { useEffect, useState, type ReactNode } from 'react';
import { Icon } from './Icon';
import { Logo } from './Logo';
import { MenuDePerfil } from './MenuDePerfil';
import { SelectorDeNegocio } from './SelectorDeNegocio';

export type SeccionActiva =
  'home' | 'negocio' | 'caja' | 'templates' | 'zone' | 'avisos' | 'goals' | 'plans' | 'account';

type TopBarProps = {
  active?: SeccionActiva;
  nombre: string;
  /** La campana de avisos, que vive en el encabezado. */
  campana?: ReactNode;
  onInicio: () => void;
  onMiNegocio: () => void;
  onCaja: () => void;
  onZona: () => void;
  onAvisos: () => void;
  onObjetivos: () => void;
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
  onMiNegocio,
  onCaja,
  onZona,
  onAvisos,
  onObjetivos,
  onPlanes,
  onAyuda,
  onCuenta,
  onCerrarSesion,
  cerrandoSesion = false,
}: TopBarProps) {
  // En celular las secciones van en un menú hamburguesa al lado del usuario.
  const [menuAbierto, setMenuAbierto] = useState(false);
  useEffect(() => {
    if (!menuAbierto) return;
    const alTeclear = (evento: KeyboardEvent) => {
      if (evento.key === 'Escape') setMenuAbierto(false);
    };
    document.addEventListener('keydown', alTeclear);
    return () => document.removeEventListener('keydown', alTeclear);
  }, [menuAbierto]);
  // Elegir una sección cierra el menú.
  const ir = (accion: () => void) => () => {
    setMenuAbierto(false);
    accion();
  };

  return (
    <header className={`topbar ${menuAbierto ? 'menu-abierto' : ''}`.trim()}>
      <Logo onClick={ir(onInicio)} />
      <nav id="nav-principal" className="topnav" aria-label="Principal">
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
          className={active === 'negocio' ? 'active' : ''}
          aria-current={active === 'negocio' ? 'page' : undefined}
          onClick={ir(onMiNegocio)}
        >
          Mi negocio
        </button>
        <button
          type="button"
          className={active === 'caja' ? 'active' : ''}
          aria-current={active === 'caja' ? 'page' : undefined}
          onClick={ir(onCaja)}
        >
          Caja
        </button>
        <button
          type="button"
          className={active === 'avisos' ? 'active' : ''}
          aria-current={active === 'avisos' ? 'page' : undefined}
          onClick={ir(onAvisos)}
        >
          Avisos
        </button>
        <button
          type="button"
          className={active === 'goals' ? 'active' : ''}
          aria-current={active === 'goals' ? 'page' : undefined}
          onClick={ir(onObjetivos)}
        >
          Objetivos
        </button>
        <button type="button" className={active === 'zone' ? 'active' : ''} onClick={ir(onZona)}>
          Zona
        </button>
        <button
          type="button"
          className={active === 'plans' ? 'active' : ''}
          aria-current={active === 'plans' ? 'page' : undefined}
          onClick={ir(onPlanes)}
        >
          Planes
        </button>
        <button type="button" className="topnav-ayuda" onClick={ir(onAyuda)}>
          <Icon name="help" /> Ayuda
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
        <div className="topbar-cuenta">
          <SelectorDeNegocio />
          <MenuDePerfil
            nombre={nombre}
            enCuenta={active === 'account'}
            cerrando={cerrandoSesion}
            onConfiguracion={onCuenta}
            onCerrarSesion={onCerrarSesion}
          />
        </div>
        <button
          type="button"
          className="menu-movil-boton"
          aria-label={menuAbierto ? 'Cerrar el menú' : 'Abrir el menú'}
          aria-expanded={menuAbierto}
          aria-controls="nav-principal"
          onClick={() => setMenuAbierto((abierto) => !abierto)}
        >
          <Icon name={menuAbierto ? 'close' : 'menu'} />
        </button>
      </div>
    </header>
  );
}
