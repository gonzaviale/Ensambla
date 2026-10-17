import { Link, useParams } from '@tanstack/react-router';
import { ChartBarIcon, HammerIcon, PackageIcon, TableIcon } from '@phosphor-icons/react';
import { SECCION_CONSTRUIR, SECCION_REPORTES, SECCION_STOCK, SECCION_USAR } from './secciones';

const itemClasses =
  'flex min-h-12 min-w-0 flex-1 items-center justify-center gap-2 rounded-md px-2 text-caption font-semibold text-soft no-underline hover:bg-surface hover:text-petrol data-[status=active]:bg-petrol data-[status=active]:text-white data-[status=active]:shadow-sm data-[status=active]:hover:bg-petrol-700 data-[status=active]:hover:text-white sm:flex-none sm:px-4 sm:text-base';

// Selector de modo dentro de un sistema: Armar (estructura) ↔ Cargar y ver (datos) ↔ Stock ↔ Reportes. Aparece en
// todas las pantallas del sistema, en celular y escritorio.
export function ModoProyecto() {
  const { proyectoId } = useParams({ strict: false });
  if (!proyectoId) return null;

  return (
    <nav
      aria-label="Modo del sistema"
      className="mb-6 grid w-full grid-cols-2 gap-1 rounded-lg border border-line bg-soft-surface p-1.5 sm:flex sm:w-fit"
    >
      <Link
        to="/proyectos/$proyectoId/usar"
        params={{ proyectoId }}
        className={itemClasses}
        activeProps={{ 'aria-current': 'page' }}
      >
        <TableIcon aria-hidden size={22} />
        {SECCION_USAR}
      </Link>
      <Link
        to="/proyectos/$proyectoId/stock"
        params={{ proyectoId }}
        className={itemClasses}
        activeProps={{ 'aria-current': 'page' }}
      >
        <PackageIcon aria-hidden size={22} />
        {SECCION_STOCK}
      </Link>
      <Link
        to="/proyectos/$proyectoId/reportes"
        params={{ proyectoId }}
        className={itemClasses}
        activeProps={{ 'aria-current': 'page' }}
      >
        <ChartBarIcon aria-hidden size={22} />
        {SECCION_REPORTES}
      </Link>
      <Link
        to="/proyectos/$proyectoId/construir"
        params={{ proyectoId }}
        className={itemClasses}
        activeProps={{ 'aria-current': 'page' }}
      >
        <HammerIcon aria-hidden size={22} />
        {SECCION_CONSTRUIR}
      </Link>
    </nav>
  );
}
