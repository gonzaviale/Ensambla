import { Link, useRouterState } from '@tanstack/react-router';
import { Logo, buttonClasses } from '@/components/ui';
import { LandingLinks, LandingMenu } from './LandingMenu';

export function PublicHeader() {
  const ruta = useRouterState({ select: (s) => s.location.pathname });
  const enLanding = ruta === '/';
  // En el ingreso y el registro el formulario ya está en pantalla: el "Entrar" sobra.
  const enAcceso = ruta === '/login' || ruta === '/registro';

  return (
    <header className="brand-header sticky top-0 z-(--z-nav) border-b border-line bg-canvas">
      <div className="mx-auto flex min-h-18 w-full max-w-330 items-center gap-3 px-4 md:px-8">
        <Link to="/" aria-label="Ensambla, ir al inicio" className="text-fg no-underline">
          <Logo />
        </Link>
        {enLanding && <LandingLinks />}
        <nav aria-label="Cuenta" className="ml-auto flex items-center gap-2">
          {/* En 320 px no entran dos botones junto al logo: se colapsan en un único "Entrar". */}
          {!enAcceso && (
            <Link
              to="/login"
              className={buttonClasses({ variant: 'primary', className: 'sm:hidden' })}
            >
              Entrar
            </Link>
          )}
          <Link
            to="/login"
            className={buttonClasses({ variant: 'ghost', className: 'max-sm:hidden' })}
          >
            Ingresar
          </Link>
          <Link
            to="/registro"
            className={buttonClasses({
              variant: 'solid',
              className: 'text-white hover:text-white max-sm:hidden',
            })}
          >
            Crear cuenta
          </Link>
        </nav>
        {enLanding && <LandingMenu />}
      </div>
    </header>
  );
}
