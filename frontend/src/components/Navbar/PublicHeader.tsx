import { Link, useRouterState } from '@tanstack/react-router';
import { Logo, buttonClasses } from '@/components/ui';
import { subirArriba } from '@/hooks/landing/subirArriba';
import { LandingLinks, LandingMenu } from './LandingMenu';

export function PublicHeader() {
  const ruta = useRouterState({ select: (s) => s.location.pathname });
  const enLanding = ruta === '/';
  // En el ingreso y el registro el formulario ya está en pantalla: el "Entrar" sobra.
  const enAcceso = ruta === '/login' || ruta === '/registro';

  return (
    <header className="sticky top-0 z-(--z-nav) border-b border-line bg-surface">
      <div className="mx-auto flex h-16 w-full max-w-300 items-center gap-2 px-4 sm:gap-3 md:px-8">
        <Link
          to="/"
          aria-label="Ensambla, ir al inicio"
          className="text-fg no-underline"
          // Estando ya en la landing el router no navega: subimos al principio a mano.
          onClick={(e) => {
            if (!enLanding || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) {
              return;
            }
            e.preventDefault();
            subirArriba();
          }}
        >
          <Logo />
        </Link>
        {enLanding && <LandingLinks />}
        <nav aria-label="Cuenta" className="ml-auto flex items-center gap-2">
          {ruta !== '/planes' && (
            <Link
              to="/planes"
              className={buttonClasses({
                variant: 'ghost',
                className: 'hidden px-2 sm:inline-flex',
              })}
            >
              Planes
            </Link>
          )}
          {enLanding ? (
            <Link
              to="/login"
              className={buttonClasses({ variant: 'secondary', className: 'px-3' })}
            >
              Ingresar
            </Link>
          ) : (
            <>
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
                  className: 'font-extrabold text-white hover:text-white max-sm:hidden',
                })}
              >
                Crear cuenta
              </Link>
            </>
          )}
        </nav>
        {enLanding && <LandingMenu />}
      </div>
    </header>
  );
}
