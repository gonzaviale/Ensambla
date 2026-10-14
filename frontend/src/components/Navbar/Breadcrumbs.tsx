import { useQuery } from '@tanstack/react-query';
import { Link, useParams, useRouterState } from '@tanstack/react-router';
import { proyectoQuery } from '@/hooks/proyectos/queries';
import {
  SECCION_CONSTRUIR,
  SECCION_CUADERNO,
  SECCION_PROYECTOS,
  SECCION_REPORTES,
  SECCION_REVISAR,
  SECCION_STOCK,
  SECCION_USAR,
} from './secciones';

const olClasses =
  'm-0 flex list-none flex-wrap items-center gap-x-1 gap-y-0 p-0 text-hint text-soft';
// Cada miga es un área de toque de 48 px aunque el texto sea chico.
const enlaceClasses = 'inline-flex min-h-12 items-center px-1 font-semibold';

// Ruta de migas de pan: Mis sistemas › {sistema} › Armar | Cargar y ver | Stock | Reportes, o Mis sistemas › Subir foto
// › Revisar cuaderno. En Mis sistemas no se muestra.
export function Breadcrumbs() {
  const { proyectoId } = useParams({ strict: false });
  const pathname = useRouterState({ select: (state) => state.location.pathname });
  const { data: proyecto } = useQuery({
    ...proyectoQuery(proyectoId ?? ''),
    enabled: !!proyectoId,
  });

  if (pathname === '/cuaderno' || pathname.startsWith('/cuaderno/')) {
    const revisando = pathname !== '/cuaderno';
    return (
      <nav aria-label="Ruta de navegación" className="mb-2">
        <ol className={olClasses}>
          <li>
            <Link to="/proyectos" className={enlaceClasses}>
              {SECCION_PROYECTOS}
            </Link>
          </li>
          <li aria-hidden>›</li>
          <li aria-current={revisando ? undefined : 'page'}>
            {revisando ? (
              <Link to="/cuaderno" className={enlaceClasses}>
                {SECCION_CUADERNO}
              </Link>
            ) : (
              <span className="font-bold text-fg">{SECCION_CUADERNO}</span>
            )}
          </li>
          {revisando && (
            <>
              <li aria-hidden>›</li>
              <li aria-current="page" className="font-bold text-fg">
                {SECCION_REVISAR}
              </li>
            </>
          )}
        </ol>
      </nav>
    );
  }
  if (!proyectoId) return null;
  const modo = pathname.endsWith('/usar')
    ? SECCION_USAR
    : pathname.endsWith('/stock')
      ? SECCION_STOCK
      : pathname.endsWith('/reportes')
        ? SECCION_REPORTES
        : SECCION_CONSTRUIR;

  return (
    <nav aria-label="Ruta de navegación" className="mb-2">
      <ol className={olClasses}>
        <li>
          <Link to="/proyectos" className={enlaceClasses}>
            {SECCION_PROYECTOS}
          </Link>
        </li>
        <li aria-hidden>›</li>
        <li className="min-w-0">
          <Link
            to="/proyectos/$proyectoId/usar"
            params={{ proyectoId }}
            className={`${enlaceClasses} max-w-[16rem] truncate`}
          >
            {proyecto?.nombre ?? 'Sistema'}
          </Link>
        </li>
        <li aria-hidden>›</li>
        <li aria-current="page" className="font-bold text-fg">
          {modo}
        </li>
      </ol>
    </nav>
  );
}
