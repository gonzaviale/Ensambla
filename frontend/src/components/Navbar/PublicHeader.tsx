import { Link, useRouterState } from '@tanstack/react-router';
import { Logo, buttonClasses } from '@/components/ui';
import { LandingLinks, LandingMenu } from './LandingMenu';

export function PublicHeader() {
  const enLanding = useRouterState({ select: (s) => s.location.pathname === '/' });

  return (
    <header className="sticky top-0 z-(--z-nav) border-b border-line bg-surface">
      <div className="mx-auto flex h-16 w-full max-w-300 items-center gap-3 px-4 md:px-8">
        <Link to="/" aria-label="Ensambla, ir al inicio" className="text-fg no-underline">
          <Logo />
        </Link>
        {enLanding && <LandingLinks />}
        <nav aria-label="Cuenta" className="ml-auto flex items-center gap-2">
          {/* En 320 px no entran dos botones junto al logo: se colapsan en un único "Entrar". */}
          <Link
            to="/login"
            className={buttonClasses({ variant: 'primary', className: 'sm:hidden' })}
          >
            Entrar
          </Link>
          <Link
            to="/login"
            className={buttonClasses({ variant: 'ghost', className: 'max-sm:hidden' })}
          >
            Ingresar
          </Link>
          <Link
            to="/registro"
            className={buttonClasses({ variant: 'solid', className: 'max-sm:hidden' })}
          >
            Crear cuenta
          </Link>
        </nav>
        {enLanding && <LandingMenu />}
      </div>
    </header>
  );
}
