import { Link, useParams } from '@tanstack/react-router';
import { HammerIcon, TableIcon } from '@phosphor-icons/react';
import { cn } from '@/lib/cn';
import { SECCION_CONSTRUIR, SECCION_USAR } from './secciones';

const itemClasses =
  'flex min-h-12 flex-1 items-center justify-center gap-2 rounded-md px-4 text-base font-semibold text-soft no-underline hover:text-fg sm:flex-none';
const activeClasses = 'bg-surface text-fg shadow-sm';

// Selector de modo dentro de un sistema: Armar (estructura) ↔ Cargar y ver (datos). Aparece en
// ambas pantallas del sistema, en celular y escritorio.
export function ModoProyecto() {
  const { proyectoId } = useParams({ strict: false });
  if (!proyectoId) return null;

  return (
    <nav
      aria-label="Modo del sistema"
      className="mb-6 flex w-full gap-1 rounded-lg bg-fg/8 p-1 sm:w-fit"
    >
      <Link
        to="/proyectos/$proyectoId/usar"
        params={{ proyectoId }}
        className={itemClasses}
        activeProps={{ className: cn(itemClasses, activeClasses), 'aria-current': 'page' }}
      >
        <TableIcon aria-hidden size={22} />
        {SECCION_USAR}
      </Link>
      <Link
        to="/proyectos/$proyectoId/construir"
        params={{ proyectoId }}
        className={itemClasses}
        activeProps={{ className: cn(itemClasses, activeClasses), 'aria-current': 'page' }}
      >
        <HammerIcon aria-hidden size={22} />
        {SECCION_CONSTRUIR}
      </Link>
    </nav>
  );
}
