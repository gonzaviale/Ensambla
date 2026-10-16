import { Link } from '@tanstack/react-router';
import { HouseLineIcon } from '@phosphor-icons/react';
import { CandadoPlan } from '@/components/Plan/PlanGate';
import {
  EmptyState,
  ErrorState,
  PageHeading,
  Skeleton,
  Toast,
  buttonClasses,
} from '@/components/ui';
import { DesbloquearRespuesta } from '@/components/Inicio/DesbloquearRespuesta';
import { PrimerosPasos } from '@/components/Inicio/PrimerosPasos';
import { RespuestaGrande, RespuestaSinDatos } from '@/components/Inicio/RespuestaGrande';
import { prioridadDe } from '@/components/Respuestas/negocioUtil';
import { useSumarBloque } from '@/components/Respuestas/useSumarBloque';
import { useMiNegocio } from '@/hooks/negocio/useMiNegocio';
import { useRespuestas } from '@/hooks/negocio/useRespuestas';
import type { PiezaId, RespuestaVista } from '@/types/negocio';

const MAXIMO_RESPUESTAS = 3;

type Bloqueada = RespuestaVista & { resultado: { estado: 'bloqueada'; faltan: PiezaId[] } };
const estaBloqueada = (vista: RespuestaVista): vista is Bloqueada =>
  vista.resultado.estado === 'bloqueada';

// «Mi negocio»: poco, grande y claro. Hasta tres respuestas con plata, la que está más cerca de
// desbloquearse y unos primeros pasos que se van tildando solos.
export function InicioPage() {
  const negocio = useMiNegocio();
  const respuestas = useRespuestas();
  const sumar = useSumarBloque();

  if (negocio.cargando) {
    return (
      <div className="flex flex-col gap-6">
        <PageHeading kicker="Inicio" title="Mi negocio" lead="Estamos buscando tu sistema…" />
        <div
          role="status"
          aria-label="Cargando…"
          className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3"
        >
          <Skeleton className="h-48" />
          <Skeleton className="h-48" />
          <Skeleton className="h-48" />
        </div>
      </div>
    );
  }
  if (negocio.error) {
    return (
      <div className="flex flex-col gap-6">
        <PageHeading kicker="Inicio" title="Mi negocio" />
        <ErrorState message="Revisá tu conexión e intentá de nuevo." />
      </div>
    );
  }
  if (!negocio.hayNegocio) {
    return (
      <div className="flex flex-col gap-6">
        <PageHeading
          kicker="Inicio"
          title="Mi negocio"
          lead="Contanos cómo trabajás y Ensambla arma tu sistema."
        />
        <EmptyState
          icon={<HouseLineIcon aria-hidden size={34} />}
          title="Todavía no armaste tu sistema"
        >
          <p className="m-0">Contanos qué vendés y en un minuto lo tenés listo.</p>
          <Link
            to="/bienvenida"
            className={buttonClasses({ variant: 'solid', size: 'lg', className: 'mt-4' })}
          >
            Armemos tu sistema
          </Link>
        </EmptyState>
      </div>
    );
  }

  const vistas = [...respuestas.vistas].sort(
    (a, b) => prioridadDe(a.definicion.id) - prioridadDe(b.definicion.id),
  );
  const listas = vistas.filter((v) => v.resultado.estado === 'lista').slice(0, MAXIMO_RESPUESTAS);
  const sinDatos = vistas.find((v) => v.resultado.estado === 'sin-datos');
  const hayPlan = vistas.some((v) => v.resultado.estado === 'plan');

  // La bloqueada con menos piezas faltantes (a igual cantidad, la de mayor prioridad).
  const masCerca = vistas
    .filter(estaBloqueada)
    .reduce<Bloqueada | undefined>(
      (mejor, vista) =>
        !mejor || vista.resultado.faltan.length < mejor.resultado.faltan.length ? vista : mejor,
      undefined,
    );

  return (
    <div className="flex flex-col gap-8">
      <PageHeading
        kicker="Inicio"
        title={negocio.sistema?.nombre ?? 'Mi negocio'}
        lead="Esto es lo que tu negocio te cuenta hoy."
        actions={
          <Link to="/subir" className={buttonClasses({ variant: 'solid', size: 'lg' })}>
            Contá qué pasó
          </Link>
        }
      />

      <div data-recorrido="inicio-respuestas" className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {respuestas.cargando && listas.length === 0 ? (
          <Skeleton className="h-48" />
        ) : (
          <>
            {listas.map((vista) => (
              <RespuestaGrande key={vista.definicion.id} vista={vista} />
            ))}
            {listas.length < MAXIMO_RESPUESTAS && sinDatos && (
              <RespuestaSinDatos pregunta={sinDatos.definicion.pregunta} />
            )}
          </>
        )}
      </div>

      {masCerca && (
        <div data-recorrido="inicio-desbloquear">
          <DesbloquearRespuesta
            vista={masCerca}
            faltan={masCerca.resultado.faltan}
            tope={sumar.tope}
            sumando={sumar.sumando}
            onSumar={(id) => void sumar.sumar(id)}
          />
        </div>
      )}

      {hayPlan && (
        <CandadoPlan
          funcion="respuestas-completas"
          beneficio="Hay más respuestas esperando con los datos que ya cargaste."
        />
      )}

      <PrimerosPasos
        hayNegocio={negocio.hayNegocio}
        cargoDato={vistas.some((v) => v.resultado.estado === 'lista')}
        sumoBloque={negocio.piezasPresentes.length > 3}
      />

      {sumar.aviso && (
        <Toast tipo={sumar.aviso.tipo} mensaje={sumar.aviso.mensaje} onClose={sumar.cerrarAviso} />
      )}
    </div>
  );
}
