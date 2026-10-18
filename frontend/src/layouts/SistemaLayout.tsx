import { Outlet } from '@tanstack/react-router';
import { IconSprite } from '@/components/sistema/IconSprite';
import { useUsuarioOpcional } from '@/hooks/auth/queries';
import '@/styles/sistema/index.css';

// Todo lo que está adentro de `.mk-root` usa las hojas del mockup; la landing y el acceso quedan afuera.
// Mismo armazón que el index.html del mockup: sprite de íconos, enlace de salto, #app, #overlay y #toast.
export function SistemaLayout() {
  const usuario = useUsuarioOpcional();
  // Sesión cerrada: el guard de la ruta destino redirige en un instante.
  if (!usuario) return null;

  return (
    <div className="mk-root">
      <IconSprite />
      <a className="skip-link" href="#main">
        Ir al contenido
      </a>
      <div id="app">
        <Outlet />
      </div>
      <div id="overlay" />
      <div id="toast" className="toast" role="status" aria-live="polite" />
    </div>
  );
}
