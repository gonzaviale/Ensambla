import { useEffect, useId, useRef, useState, type KeyboardEvent } from 'react';
import { Icon } from './Icon';

type MenuDePerfilProps = {
  nombre: string;
  /** Si se está en «Tu cuenta», para marcar el avatar. */
  enCuenta: boolean;
  cerrando: boolean;
  onConfiguracion: () => void;
  onCerrarSesion: () => void;
};

// El avatar de la cabecera: despliega un menú con la configuración de la cuenta y la salida.
export function MenuDePerfil({
  nombre,
  enCuenta,
  cerrando,
  onConfiguracion,
  onCerrarSesion,
}: MenuDePerfilProps) {
  const [abierto, setAbierto] = useState(false);
  const raiz = useRef<HTMLDivElement>(null);
  const disparador = useRef<HTMLButtonElement>(null);
  const idDelMenu = useId();

  // Se cierra con Esc (devolviendo el foco al avatar) o al tocar o tabular fuera del menú.
  useEffect(() => {
    if (!abierto) return;
    const opciones = () => [
      ...(raiz.current?.querySelectorAll<HTMLElement>('[role="menuitem"]') ?? []),
    ];
    opciones()[0]?.focus();
    const alTocar = (evento: PointerEvent) => {
      if (!raiz.current?.contains(evento.target as Node)) setAbierto(false);
    };
    const alTeclear = (evento: globalThis.KeyboardEvent) => {
      if (evento.key !== 'Escape') return;
      setAbierto(false);
      disparador.current?.focus();
    };
    document.addEventListener('pointerdown', alTocar);
    document.addEventListener('keydown', alTeclear);
    return () => {
      document.removeEventListener('pointerdown', alTocar);
      document.removeEventListener('keydown', alTeclear);
    };
  }, [abierto]);

  const moverFoco = (evento: KeyboardEvent<HTMLDivElement>) => {
    if (evento.key === 'Tab') {
      setAbierto(false);
      return;
    }
    const sentido = { ArrowDown: 1, ArrowUp: -1 }[evento.key];
    if (!sentido) return;
    evento.preventDefault();
    const opciones = [...evento.currentTarget.querySelectorAll<HTMLElement>('[role="menuitem"]')];
    const actual = opciones.indexOf(document.activeElement as HTMLElement);
    opciones.at((actual + sentido) % opciones.length)?.focus();
  };

  const elegir = (accion: () => void) => () => {
    setAbierto(false);
    accion();
  };

  return (
    <div className="perfil-menu" ref={raiz}>
      <button
        ref={disparador}
        type="button"
        className="row profile"
        aria-label="Menú de tu cuenta"
        aria-haspopup="menu"
        aria-expanded={abierto}
        aria-controls={abierto ? idDelMenu : undefined}
        aria-current={enCuenta ? 'page' : undefined}
        onClick={() => setAbierto((actual) => !actual)}
      >
        <span>
          <span className="profile-name">{nombre}</span>
          <span className="profile-label">Tu espacio personal</span>
        </span>
        <span className="avatar">{nombre.slice(0, 2).toUpperCase()}</span>
      </button>
      {abierto && (
        <div id={idDelMenu} className="perfil-menu-lista" role="menu" onKeyDown={moverFoco}>
          <button type="button" role="menuitem" onClick={elegir(onConfiguracion)}>
            <Icon name="settings" /> Configuración
          </button>
          <button
            type="button"
            role="menuitem"
            disabled={cerrando}
            onClick={elegir(onCerrarSesion)}
          >
            <Icon name="back" /> Cerrar sesión
          </button>
        </div>
      )}
    </div>
  );
}
