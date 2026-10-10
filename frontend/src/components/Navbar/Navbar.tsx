import { Link } from '@tanstack/react-router';
import { Button, Logo, buttonClasses } from '@/components/ui';
import { useCerrarSesion } from '@/hooks/auth/mutations';
import { useUsuario } from '@/hooks/auth/queries';
import { SECCION_PROYECTOS } from './secciones';

// Barra superior de la app (T-033): logo, sección principal y menú del usuario.
export function Navbar() {
  const usuario = useUsuario();
  const { cerrarSesion, cerrando, error } = useCerrarSesion();

  return (
    <header className="sticky top-0 z-40 flex min-h-16 items-center gap-3 border-b border-line bg-surface px-4 sm:px-6">
      <Link
        to="/proyectos"
        aria-label={`Ensambla, ir a ${SECCION_PROYECTOS}`}
        className="text-fg no-underline"
      >
        <Logo />
      </Link>
      <nav aria-label="Principal" className="ml-4 hidden items-center gap-1 md:flex">
        <Link to="/proyectos" className={buttonClasses({ variant: 'ghost' })}>
          {SECCION_PROYECTOS}
        </Link>
      </nav>
      <div className="ml-auto flex items-center gap-3">
        <span className="hidden text-[15px] text-soft sm:inline">{usuario.nombre}</span>
        {error && (
          <span role="alert" className="text-[15px] text-bad">
            No pudimos cerrar sesión. Volvé a intentarlo.
          </span>
        )}
        <Button
          size="sm"
          disabled={cerrando}
          onClick={() => void cerrarSesion().catch(() => undefined)}
        >
          {cerrando ? 'Cerrando…' : 'Cerrar sesión'}
        </Button>
      </div>
    </header>
  );
}
