import { Link } from '@tanstack/react-router';
import { Button, Logo, buttonClasses } from '@/components/ui';
import { useCerrarSesion } from '@/hooks/auth/mutations';
import { useUsuario } from '@/hooks/auth/queries';
import { SECCION_CUADERNO, SECCION_PROYECTOS } from './secciones';

// Barra superior de la app (T-033): logo, secciones principales y sesión. Mismo ancho que <main>.
export function Navbar() {
  const usuario = useUsuario();
  const { cerrarSesion, cerrando, error } = useCerrarSesion();

  return (
    <header className="sticky top-0 z-(--z-nav) border-b border-line bg-surface">
      <div className="mx-auto flex min-h-16 w-full max-w-300 items-center gap-3 px-4 md:px-8">
        <Link
          to="/proyectos"
          aria-label={`Ensambla, ir a ${SECCION_PROYECTOS}`}
          className="text-fg no-underline"
        >
          <Logo />
        </Link>
        <nav aria-label="Principal" className="ml-4 hidden items-center gap-1 md:flex">
          <Link
            to="/proyectos"
            activeOptions={{ exact: true }}
            className={buttonClasses({ variant: 'ghost' })}
            activeProps={{
              className: buttonClasses({ variant: 'ghost', className: 'bg-accent-100' }),
            }}
          >
            {SECCION_PROYECTOS}
          </Link>
          <Link
            to="/cuaderno"
            className={buttonClasses({ variant: 'ghost' })}
            activeProps={{
              className: buttonClasses({ variant: 'ghost', className: 'bg-accent-100' }),
            }}
          >
            {SECCION_CUADERNO}
          </Link>
        </nav>
        <div className="ml-auto flex items-center gap-3">
          <span className="hidden max-w-48 truncate text-hint text-soft lg:inline">
            {usuario.nombre}
          </span>
          {error && (
            <span role="alert" className="text-hint font-semibold text-bad">
              No pudimos cerrar sesión. Volvé a intentarlo.
            </span>
          )}
          <Button
            className="max-md:hidden"
            loading={cerrando}
            onClick={() => void cerrarSesion().catch(() => undefined)}
          >
            {cerrando ? 'Cerrando…' : 'Cerrar sesión'}
          </Button>
        </div>
      </div>
    </header>
  );
}
