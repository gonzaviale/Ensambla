import { Link, useNavigate } from '@tanstack/react-router';
import { BotonRecorrido } from '@/components/Recorrido/BotonRecorrido';
import { Button, Logo, Menu, buttonClasses } from '@/components/ui';
import { useCerrarSesion } from '@/hooks/auth/mutations';
import { useUsuario } from '@/hooks/auth/queries';
import { usePlanActual } from '@/hooks/negocio/usePlan';
import { useRecorrido } from '@/hooks/recorrido/useRecorrido';
import { cn } from '@/lib/cn';
import {
  SECCION_BLOQUES,
  SECCION_CUADERNO,
  SECCION_CUENTA,
  SECCION_INICIO,
  SECCION_PLAN,
  SECCION_PROYECTOS,
  SECCION_RESPUESTAS,
  SECCION_SUBIR_DATOS,
} from './secciones';

const activo = buttonClasses({
  variant: 'ghost',
  className: 'bg-accent-100 px-3 whitespace-nowrap',
});
const inactivo = buttonClasses({ variant: 'ghost', className: 'px-3 whitespace-nowrap' });

// Barra superior de la app: logo, cuatro secciones y «Más». Lo avanzado (armar tablas, stock,
// reportes) queda fuera del menú principal. Mismo ancho que <main>.
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
          to="/inicio"
          aria-label={`Ensambla, ir a ${SECCION_INICIO}`}
          className="text-fg no-underline"
        >
          <Logo />
        </Link>
        <nav aria-label="Principal" className="ml-2 hidden items-center gap-1 md:flex">
          <Link to="/inicio" className={inactivo} activeProps={{ className: activo }}>
            {SECCION_INICIO}
          </Link>
          <Link to="/subir" className={inactivo} activeProps={{ className: activo }}>
            {SECCION_SUBIR_DATOS}
          </Link>
          <Link to="/bloques" className={inactivo} activeProps={{ className: activo }}>
            {SECCION_BLOQUES}
          </Link>
          <Link to="/respuestas" className={inactivo} activeProps={{ className: activo }}>
            {SECCION_RESPUESTAS}
          </Link>
          <Menu
            label="Más secciones"
            items={[
              { label: SECCION_PLAN, onSelect: () => void navegar({ to: '/plan' }) },
              { label: SECCION_CUENTA, onSelect: () => void navegar({ to: '/cuenta' }) },
              { label: SECCION_CUADERNO, onSelect: () => void navegar({ to: '/cuaderno' }) },
              {
                label: `Avanzado: ${SECCION_PROYECTOS}`,
                onSelect: () => void navegar({ to: '/proyectos' }),
              },
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
            className="hidden max-w-48 truncate text-hint font-semibold text-soft no-underline hover:text-fg xl:inline"
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
