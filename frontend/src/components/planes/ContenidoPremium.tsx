import { Link } from '@tanstack/react-router';
import type { AccesoContenido } from '@/types/contenidoPlan';

export function ContenidoPremium({
  acceso,
  recurso,
}: {
  acceso?: Pick<AccesoContenido, 'usadas' | 'limite' | 'bloqueadas' | 'finPeriodo'> | null;
  recurso: 'avisos' | 'recomendaciones';
}) {
  if (!acceso || acceso.limite === null) return null;
  return (
    <section className="contenido-premium" aria-label={`Tu acceso a ${recurso}`}>
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
          <strong>
            Hay {acceso.bloqueadas}{' '}
            {acceso.bloqueadas === 1 ? (recurso === 'avisos' ? 'aviso' : 'recomendación') : recurso}{' '}
            más para explorar
          </strong>
          <p>Alcanzaste el cupo de este mes. Podés seguir usando tus tablas y datos.</p>
          <Link to="/planes" className="text-link">
            Ver opciones del plan
          </Link>
        </div>
      ) : null}
    </section>
  );
}
