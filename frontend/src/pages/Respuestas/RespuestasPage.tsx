import { useState } from 'react';
import { Link } from '@tanstack/react-router';
import { CaretRightIcon, ChartBarIcon } from '@phosphor-icons/react';
import { CandadoPlan, EtiquetaPlan } from '@/components/Plan/PlanGate';
import {
  EmptyState,
  ErrorState,
  PageHeading,
  Panel,
  Skeleton,
  Toast,
  buttonClasses,
} from '@/components/ui';
import { BotonSumarPieza } from '@/components/Respuestas/BotonSumarPieza';
import { DescargaPlanilla } from '@/components/Respuestas/DescargaPlanilla';
import { DetalleRespuesta } from '@/components/Respuestas/DetalleRespuesta';
import { prioridadDe, unir } from '@/components/Respuestas/negocioUtil';
import { useSumarBloque } from '@/components/Respuestas/useSumarBloque';
import { useMiNegocio } from '@/hooks/negocio/useMiNegocio';
import { useRespuestas } from '@/hooks/negocio/useRespuestas';
import { pieza } from '@/lib/catalogo/piezas';
import type { RespuestaId, RespuestaVista } from '@/types/negocio';

// Todas las respuestas de tu negocio, en tres grupos: las que ya sabés, las que te faltan
// desbloquear y las que son del plan completo.
export function RespuestasPage() {
  const negocio = useMiNegocio();
  const respuestas = useRespuestas();
  const sumar = useSumarBloque();
  const [abierta, setAbierta] = useState<RespuestaId | null>(null);

  const encabezado = (
    <PageHeading
      kicker="Respuestas"
      title="Lo que tu negocio te cuenta"
      lead="Cada bloque de tu sistema responde algo. Tocá una respuesta para ver el detalle."
    />
  );

  if (negocio.cargando) {
    return (
      <div className="flex flex-col gap-6">
        {encabezado}
        <div role="status" aria-label="Cargando…" className="grid gap-3">
          <Skeleton className="h-20" />
          <Skeleton className="h-20" />
          <Skeleton className="h-20" />
        </div>
      </div>
    );
  }
  if (negocio.error) {
    return (
      <div className="flex flex-col gap-6">
        {encabezado}
        <ErrorState message="Revisá tu conexión e intentá de nuevo." />
      </div>
    );
  }
  if (!negocio.hayNegocio) {
    return (
      <div className="flex flex-col gap-6">
        {encabezado}
        <EmptyState icon={<ChartBarIcon aria-hidden size={34} />} title="Todavía no hay respuestas">
          <p className="m-0">Armá tu sistema y tus respuestas aparecen acá.</p>
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
  const listas = vistas.filter((v) => v.resultado.estado === 'lista');
  const sinDatos = vistas.filter((v) => v.resultado.estado === 'sin-datos');
  const bloqueadas = vistas.filter((v) => v.resultado.estado === 'bloqueada');
  const delPlan = vistas.filter((v) => v.resultado.estado === 'plan');
  const detalle = vistas.find((v) => v.definicion.id === abierta) ?? null;

  // La planilla es de la tabla de Ventas o, si no hay, de la primera que tenga el sistema.
  const tabla =
    negocio.presentes.get('ventas')?.bloque ??
    negocio.presentes.values().next().value?.bloque ??
    negocio.sistema?.bloques[0];

  return (
    <div className="flex flex-col gap-8">
      {encabezado}

      <div data-recorrido="respuestas-lista" className="flex flex-col gap-8">
        {listas.length > 0 && (
          <Grupo titulo="Lo que ya sabés">
            {listas.map((vista) => (
              <FilaLista key={vista.definicion.id} vista={vista} onAbrir={setAbierta} />
            ))}
          </Grupo>
        )}

        {sinDatos.length > 0 && (
          <Grupo titulo="Esperando tus datos">
            {sinDatos.map((vista) => (
              <li key={vista.definicion.id}>
                <Panel tight flat className="flex flex-wrap items-center justify-between gap-3">
                  <div className="min-w-0 flex-1 basis-56">
                    <p className="m-0 text-body font-semibold">{vista.definicion.pregunta}</p>
                    <p className="m-0 text-hint text-soft">Todavía no hay datos.</p>
                  </div>
                  <Link to="/subir" className={buttonClasses({ variant: 'primary' })}>
                    Contá qué pasó
                  </Link>
                </Panel>
              </li>
            ))}
          </Grupo>
        )}

        {bloqueadas.length > 0 && (
          <Grupo titulo="Para desbloquear">
            {sumar.tope && (
              <li>
                <CandadoPlan
                  funcion="bloques-ilimitados"
                  beneficio="Con el plan Gratis tenés hasta 3 bloques."
                />
              </li>
            )}
            {bloqueadas.map((vista) => (
              <FilaBloqueada
                key={vista.definicion.id}
                vista={vista}
                tope={sumar.tope}
                sumando={sumar.sumando}
                onSumar={(id) => void sumar.sumar(id)}
              />
            ))}
          </Grupo>
        )}

        {delPlan.length > 0 && (
          <Grupo titulo="Con el plan completo">
            <li>
              <CandadoPlan
                funcion="respuestas-completas"
                beneficio="Tenés estas respuestas esperando con los datos que ya cargaste."
              />
            </li>
            {delPlan.map((vista) => (
              <li key={vista.definicion.id}>
                <Panel tight flat className="flex flex-wrap items-center justify-between gap-3">
                  <p className="m-0 min-w-0 flex-1 basis-56 text-body font-semibold">
                    {vista.definicion.pregunta}
                  </p>
                  <EtiquetaPlan funcion="respuestas-completas" />
                </Panel>
              </li>
            ))}
          </Grupo>
        )}
      </div>

      {tabla && <DescargaPlanilla bloqueId={tabla.id} nombre={tabla.nombre} />}

      <DetalleRespuesta vista={detalle} onClose={() => setAbierta(null)} />

      {sumar.aviso && (
        <Toast tipo={sumar.aviso.tipo} mensaje={sumar.aviso.mensaje} onClose={sumar.cerrarAviso} />
      )}
    </div>
  );
}

function Grupo({ titulo, children }: { titulo: string; children: React.ReactNode }) {
  return (
    <section className="flex flex-col gap-3">
      <h2 className="m-0 text-h3">{titulo}</h2>
      <ul className="m-0 flex list-none flex-col gap-3 p-0">{children}</ul>
    </section>
  );
}

function FilaLista({
  vista,
  onAbrir,
}: {
  vista: RespuestaVista;
  onAbrir: (id: RespuestaId) => void;
}) {
  if (vista.resultado.estado !== 'lista') return null;
  return (
    <li>
      <button
        type="button"
        aria-haspopup="dialog"
        onClick={() => onAbrir(vista.definicion.id)}
        className="flex min-h-12 w-full cursor-pointer items-center justify-between gap-3 rounded-lg border border-line/60 bg-surface p-4 text-left shadow-sm transition-shadow hover:shadow-md"
      >
        <span className="flex min-w-0 flex-col gap-1">
          <span className="text-hint font-semibold text-soft">{vista.definicion.pregunta}</span>
          <span className="text-h3 font-bold break-words">{vista.resultado.titular}</span>
        </span>
        <CaretRightIcon aria-hidden size={24} className="shrink-0 text-soft" />
      </button>
    </li>
  );
}

function FilaBloqueada({
  vista,
  tope,
  sumando,
  onSumar,
}: {
  vista: RespuestaVista;
  tope: boolean;
  sumando: Parameters<typeof BotonSumarPieza>[0]['sumando'];
  onSumar: Parameters<typeof BotonSumarPieza>[0]['onSumar'];
}) {
  if (vista.resultado.estado !== 'bloqueada') return null;
  const faltan = vista.resultado.faltan;
  const primera = faltan[0];
  return (
    <li>
      <Panel tight className="flex flex-col items-start gap-2">
        <p className="m-0 text-body font-semibold">{vista.definicion.pregunta}</p>
        <p className="m-0 text-hint text-soft">{vista.definicion.ayuda}</p>
        <p className="m-0 text-hint">
          Te falta sumar <strong>{unir(faltan.map((id) => pieza(id).nombre))}</strong>.
        </p>
        {primera && !tope && <BotonSumarPieza id={primera} sumando={sumando} onSumar={onSumar} />}
      </Panel>
    </li>
  );
}
