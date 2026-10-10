import { Link, useParams } from '@tanstack/react-router';
import { FolderIcon, HammerIcon, TableIcon } from '@phosphor-icons/react';
import { cn } from '@/lib/cn';
import { SECCION_CONSTRUIR, SECCION_PROYECTOS, SECCION_USAR } from './secciones';

const itemClasses =
  'flex min-h-16 flex-1 flex-col items-center justify-center gap-0.5 px-2 text-[15px] font-semibold text-soft no-underline hover:text-fg';
const activeClasses = 'text-accent-800 border-t-4 border-accent-700';

// Barra inferior del celular (oculta desde md): las mismas secciones que la barra superior.
export function BottomNav() {
  const { proyectoId } = useParams({ strict: false });

  return (
    <nav
      aria-label="Secciones"
      className="fixed inset-x-0 bottom-0 z-40 flex border-t border-line bg-surface md:hidden"
    >
      <Link
        to="/proyectos"
        activeOptions={{ exact: true }}
        className={itemClasses}
        activeProps={{ className: cn(itemClasses, activeClasses) }}
      >
        <FolderIcon aria-hidden size={26} />
        {SECCION_PROYECTOS}
      </Link>
      {proyectoId && (
        <>
          <Link
            to="/proyectos/$proyectoId/construir"
            params={{ proyectoId }}
            className={itemClasses}
            activeProps={{ className: cn(itemClasses, activeClasses) }}
          >
            <HammerIcon aria-hidden size={26} />
            {SECCION_CONSTRUIR}
          </Link>
          <Link
            to="/proyectos/$proyectoId/usar"
            params={{ proyectoId }}
            className={itemClasses}
            activeProps={{ className: cn(itemClasses, activeClasses) }}
          >
            <TableIcon aria-hidden size={26} />
            {SECCION_USAR}
          </Link>
        </>
      )}
    </nav>
  );
}
