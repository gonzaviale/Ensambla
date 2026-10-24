import { Link } from '@tanstack/react-router';
import type { AccesoContenido } from '@/types/contenidoPlan';

export function ContenidoPremium({
  acceso,
  recurso,
  compacto = false,
}: {
  acceso?: Pick<AccesoContenido, 'usadas' | 'limite' | 'bloqueadas' | 'finPeriodo'> | null;
  recurso: 'avisos' | 'recomendaciones';
  /** Más chico, para ir al pie de una lista sin quitarle lugar. */
  compacto?: boolean;
}) {
  if (!acceso || acceso.limite === null) return null;
  return (
    <section
      className={`contenido-premium ${compacto ? 'compacto' : ''}`.trim()}
      aria-label={`Tu acceso a ${recurso}`}
    >
      <div>
        <span className="tag outline">Tu plan Gratuito</span>
        <p>
          Usaste{' '}
          <strong>
            {acceso.usadas} de {acceso.limite}
          </strong>{' '}
          {recurso} de este mes.
          {recurso === 'recomendaciones' ? ' El cupo se comparte entre Mi zona y Otra zona.' : ''}
        </p>
        <small>
          Se renueva el{' '}
          {new Date(acceso.finPeriodo).toLocaleDateString('es-AR', {
            day: 'numeric',
            month: 'long',
            timeZone: 'UTC',
          })}
          .
        </small>
      </div>
      {acceso.bloqueadas > 0 ? (
        <div className="contenido-premium-extra">
          <div className="contenido-premium-muestra" aria-hidden="true">
            <i />
            <i />
            <i />
          </div>
          <strong>
            Hay {acceso.bloqueadas}{' '}
            {acceso.bloqueadas === 1 ? (recurso === 'avisos' ? 'aviso' : 'recomendación') : recurso}{' '}
            más para explorar
          </strong>
          <p>
            Con Premium podés ver todos los detalles y encontrar más oportunidades para tu negocio.
          </p>
          <Link to="/planes" className="btn primary">
            Conocer Premium
          </Link>
        </div>
      ) : (
        <Link to="/planes" className="text-link">
          Descubrí todo lo que incluye Premium
        </Link>
      )}
    </section>
  );
}
