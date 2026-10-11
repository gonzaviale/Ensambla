import { useQuery } from '@tanstack/react-query';
import { Link, useParams, useRouterState } from '@tanstack/react-router';
import { proyectoQuery } from '@/hooks/proyectos/queries';
import { SECCION_CONSTRUIR, SECCION_CUADERNO, SECCION_PROYECTOS, SECCION_USAR } from './secciones';

// Ruta de migas de pan: Mis proyectos > {proyecto} > Construir | Usar, o Mis proyectos > Subir foto.
// En Mis proyectos no se muestra.
export function Breadcrumbs() {
  const { proyectoId } = useParams({ strict: false });
  const pathname = useRouterState({ select: (state) => state.location.pathname });
  const { data: proyecto } = useQuery({
    ...proyectoQuery(proyectoId ?? ''),
    enabled: !!proyectoId,
  });

  if (pathname === '/cuaderno' || pathname.startsWith('/cuaderno/')) {
    return (
      <nav aria-label="Ruta de navegación" className="mb-4 text-[15px] text-soft">
        <ol className="m-0 flex list-none flex-wrap items-center gap-x-2 gap-y-1 p-0">
          <li>
            <Link to="/proyectos">{SECCION_PROYECTOS}</Link>
          </li>
          <li aria-hidden>›</li>
          <li aria-current="page" className="font-semibold text-fg">
            {SECCION_CUADERNO}
          </li>
        </ol>
      </nav>
    );
  }
  if (!proyectoId) return null;
  const modo = pathname.endsWith('/usar') ? SECCION_USAR : SECCION_CONSTRUIR;

  return (
    <nav aria-label="Ruta de navegación" className="mb-4 text-[15px] text-soft">
      <ol className="m-0 flex list-none flex-wrap items-center gap-x-2 gap-y-1 p-0">
        <li>
          <Link to="/proyectos">{SECCION_PROYECTOS}</Link>
        </li>
        <li aria-hidden>›</li>
        <li>
          <Link to="/proyectos/$proyectoId/usar" params={{ proyectoId }}>
            {proyecto?.nombre ?? 'Proyecto'}
          </Link>
        </li>
        <li aria-hidden>›</li>
        <li aria-current="page" className="font-semibold text-fg">
          {modo}
        </li>
      </ol>
    </nav>
  );
}
