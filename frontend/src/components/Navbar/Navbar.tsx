import { Link, useNavigate } from '@tanstack/react-router';
import { BotonRecorrido } from '@/components/Recorrido/BotonRecorrido';
import { Button, Logo, Menu, buttonClasses } from '@/components/ui';
import { useCerrarSesion } from '@/hooks/auth/mutations';
import { useUsuario } from '@/hooks/auth/queries';
import { usePlanActual } from '@/hooks/red/useRed';
import { useRecorrido } from '@/hooks/recorrido/useRecorrido';
import { cn } from '@/lib/cn';
import {
  SECCION_CUADERNO,
  SECCION_CUENTA,
  SECCION_HOY,
  SECCION_LISTAS,
  SECCION_PLAN,
  SECCION_PRECIOS,
  SECCION_PROYECTOS,
  SECCION_RED,
} from './secciones';

const activo = buttonClasses({
  variant: 'ghost',
  className: 'bg-accent-100 px-3 whitespace-nowrap',
});
const inactivo = buttonClasses({ variant: 'ghost', className: 'px-3 whitespace-nowrap' });

// Barra superior de la app (T-033): logo, secciones principales y sesión. Mismo ancho que <main>.
export function Navbar() {
  const usuario = useUsuario();
  const plan = usePlanActual();
  const { cerrarSesion, cerrando, error } = useCerrarSesion();
  const navegar = useNavigate();
  const { iniciar: iniciarRecorrido } = useRecorrido();

  return (
    <header className="sticky top-0 z-(--z-nav) border-b border-line bg-surface">
      <div className="mx-auto flex min-h-16 w-full max-w-300 items-center gap-3 px-4 md:px-8">
        <Link
          to="/hoy"
          aria-label={`Ensambla, ir a ${SECCION_HOY}`}
          className="text-fg no-underline"
        >
          <Logo />
        </Link>
        <nav aria-label="Principal" className="ml-2 hidden items-center gap-1 md:flex">
          <Link to="/hoy" className={inactivo} activeProps={{ className: activo }}>
            {SECCION_HOY}
          </Link>
          <Link to="/listas" className={inactivo} activeProps={{ className: activo }}>
            {SECCION_LISTAS}
          </Link>
          <Link to="/precios" className={inactivo} activeProps={{ className: activo }}>
            {SECCION_PRECIOS}
          </Link>
          <Link to="/red" className={inactivo} activeProps={{ className: activo }}>
            {SECCION_RED}
          </Link>
          <Link
            to="/proyectos"
            activeOptions={{ exact: true }}
            className={cn(inactivo, 'max-xl:hidden')}
            activeProps={{ className: cn(activo, 'max-xl:hidden') }}
          >
            {SECCION_PROYECTOS}
          </Link>
          <Link
            to="/cuaderno"
            className={cn(inactivo, 'max-xl:hidden')}
            activeProps={{ className: cn(activo, 'max-xl:hidden') }}
          >
            {SECCION_CUADERNO}
          </Link>
          <Menu
            className="xl:hidden"
            label="Más secciones"
            items={[
              { label: SECCION_PROYECTOS, onSelect: () => void navegar({ to: '/proyectos' }) },
              { label: SECCION_CUADERNO, onSelect: () => void navegar({ to: '/cuaderno' }) },
              { label: SECCION_PLAN, onSelect: () => void navegar({ to: '/plan' }) },
              { label: SECCION_CUENTA, onSelect: () => void navegar({ to: '/cuenta' }) },
              { label: 'Recorrido guiado', onSelect: iniciarRecorrido },
            ]}
          />
        </nav>
        <div className="ml-auto flex items-center gap-2">
          <Link
            to="/plan"
            className={cn(
              'hidden min-h-12 items-center rounded-full px-3 text-hint font-semibold no-underline lg:inline-flex',
              plan.enPrueba ? 'bg-amber-100 text-amber-700' : 'bg-sand-100 text-soft',
            )}
          >
            {plan.enPrueba
              ? `Prueba: ${plan.diasDePrueba} d`
              : `${SECCION_PLAN}: ${plan.definicion.nombre}`}
          </Link>
          <Link
            to="/cuenta"
            className="hidden max-w-48 truncate text-hint font-semibold text-soft no-underline hover:text-fg lg:inline"
            aria-label={`${SECCION_CUENTA}: ${usuario.nombre}`}
          >
            {usuario.nombre}
          </Link>
          <BotonRecorrido presentacion="barra" className="max-xl:hidden" />
          {error && (
            <span role="alert" className="text-hint font-semibold text-bad">
              No pudimos cerrar sesión. Volvé a intentarlo.
            </span>
          )}
          <Button
            className="whitespace-nowrap max-md:hidden"
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
