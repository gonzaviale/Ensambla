import { Link } from '@tanstack/react-router';
import { usePlanes } from '@/hooks/planes/queries';
import type { Planes } from '@/types/planes';
import { Btn } from '../sistema/Btn';

const fecha = (valor: string) =>
  new Intl.DateTimeFormat('es-AR', { dateStyle: 'long' }).format(new Date(valor));

const cantidad = (valor: number, limite: number | null) =>
  `${valor.toLocaleString('es-AR')} de ${limite === null ? 'sin límite' : limite.toLocaleString('es-AR')}`;

function ResumenDelPlan({ datos }: { datos: Planes }) {
  const { suscripcion, uso, prueba } = datos;
  const actual = datos.planes.find((plan) => plan.codigo === suscripcion.codigoPlanActual);
  const estado = suscripcion.pruebaActiva
    ? `Prueba de Premium activa${prueba ? ` hasta el ${fecha(prueba.finEn)}` : ''}`
    : suscripcion.codigoPlanActual === 'PREMIUM'
      ? `Premium${suscripcion.finEn ? ` · vigente hasta el ${fecha(suscripcion.finEn)}` : ''}`
      : 'Sin costo';

  return (
    <>
      <p className="cuenta-plan-actual">
        <strong>{actual?.nombre ?? 'Tu plan'}</strong>
        <span>{estado}</span>
      </p>
      <dl className="cuenta-datos">
        <div>
          <dt>Sistemas</dt>
          <dd>{cantidad(uso.proyectos, actual?.limiteProyectos ?? null)}</dd>
        </div>
        <div>
          <dt>Registros</dt>
          <dd>{cantidad(uso.registros, actual?.limiteRegistros ?? null)}</dd>
        </div>
        <div>
          <dt>Páginas leídas con IA</dt>
          <dd>{cantidad(uso.paginasIa, uso.limitePaginasIa)}</dd>
        </div>
      </dl>
    </>
  );
}

// El plan de la persona, cuánto usó de lo que incluye y el camino a los planes.
export function SeccionSuscripcion() {
  const planes = usePlanes();

  return (
    <section className="cuenta-tarjeta" aria-labelledby="cuenta-plan-titulo">
      <h2 id="cuenta-plan-titulo">Tu suscripción</h2>
      {planes.isPending ? (
        <p role="status">Consultando tu plan…</p>
      ) : planes.isError || !planes.data ? (
        <div role="alert">
          <p>{planes.mensajeError}</p>
          <Btn cls="secondary small" onClick={() => void planes.refetch()}>
            Reintentar
          </Btn>
        </div>
      ) : (
        <ResumenDelPlan datos={planes.data} />
      )}
      <div className="cuenta-acciones">
        <Link className="btn primary" to="/planes">
          Ver planes y beneficios
        </Link>
      </div>
    </section>
  );
}
