import { Link } from '@tanstack/react-router';
import { Button, Logo, buttonClasses } from '@/components/ui';
import { useCerrarSesion } from '@/hooks/auth/mutations';
import { useUsuario } from '@/hooks/auth/queries';

// Shell de la app (T-033): acá van la navegación, los breadcrumbs y el menú del usuario.
export function Navbar() {
  const usuario = useUsuario();
  const { cerrarSesion, cerrando } = useCerrarSesion();

  return (
    <header className="sticky top-0 z-40 flex h-16 items-center gap-3 border-b border-line bg-surface px-4 sm:px-6">
      <Link
        to="/proyectos"
        aria-label="Ensambla, ir a Mis proyectos"
        className="text-fg no-underline"
      >
        <Logo />
      </Link>
      <nav aria-label="Principal" className="ml-4 hidden items-center gap-1 md:flex">
        <Link to="/proyectos" className={buttonClasses({ variant: 'ghost' })}>
          Mis proyectos
        </Link>
      </nav>
      <div className="ml-auto flex items-center gap-3">
        <span className="hidden text-[13px] text-soft sm:inline">{usuario.nombre}</span>
        <Button size="sm" disabled={cerrando} onClick={() => void cerrarSesion()}>
          Cerrar sesión
        </Button>
      </div>
    </header>
  );
}
