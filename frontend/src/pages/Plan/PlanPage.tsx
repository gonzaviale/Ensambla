import { useState } from 'react';
import { FlaskIcon } from '@phosphor-icons/react';
import { ComparadorPlanes } from '@/components/Plan/ComparadorPlanes';
import { EstadoPlan } from '@/components/Plan/EstadoPlan';
import { FlujoSuscripcion } from '@/components/Plan/FlujoSuscripcion';
import { HistorialPagos } from '@/components/Plan/HistorialPagos';
import { PreguntasFrecuentes } from '@/components/Plan/PreguntasFrecuentes';
import { ValorGenerado } from '@/components/Plan/ValorGenerado';
import { Button, ConfirmDialog, PageHeading, Toast } from '@/components/ui';
import { useAccionesPlan, usePlanActual } from '@/hooks/negocio/usePlan';
import { fechaLarga } from '@/lib/formato';
import { NOMBRE_FUNCION, PLANES, planIncluye } from '@/lib/planes/planes';
import type { PeriodoPlan, PlanId } from '@/types/negocio';

// «Mi plan» (T-066 a T-070): cómo se sostiene Ensambla. Muestra qué desbloquea cada plan, compara
// sin presión y cubre todo el ciclo: prueba, pago simulado, cancelar y volver a activar.
export function PlanPage() {
  const plan = usePlanActual();
  const { cancelarSuscripcion, reactivarSuscripcion, simularFinDePrueba } = useAccionesPlan();
  const [flujo, setFlujo] = useState<{ plan: PlanId; periodo: PeriodoPlan } | null>(null);
  const [cancelando, setCancelando] = useState(false);
  const [aviso, setAviso] = useState<string | null>(null);

  const { suscripcion } = plan;
  const enPrueba = plan.enPrueba;
  const hasta = suscripcion.proximoCobro ?? suscripcion.activoHasta;
  const seguirHasta = suscripcion.estado === 'activa' && hasta ? fechaLarga(hasta) : null;
  const seVaAPerder = plan.definicion.funciones
    .filter((funcion) => !planIncluye('gratis', funcion))
    .map((funcion) => NOMBRE_FUNCION[funcion]);

  const confirmarCancelacion = () => {
    cancelarSuscripcion();
    setCancelando(false);
    setAviso(
      enPrueba
        ? 'Terminaste la prueba. Seguís con el plan Gratis y tus datos están intactos.'
        : 'Listo, cancelamos la suscripción. Seguís con tu plan hasta que termine lo ya pagado.',
    );
  };

  return (
    <div className="flex flex-col gap-8">
      <PageHeading
        title="Mi plan"
        lead="Pagás sólo si te sirve. Podés probar, cambiar o cancelar cuando quieras y volver al plan Gratis sin perder tus datos."
      />

      <EstadoPlan
        onCancelar={() => setCancelando(true)}
        onReactivar={() => {
          reactivarSuscripcion();
          setAviso('Volviste a activar tu suscripción: se renueva en la fecha de siempre.');
        }}
      />

      <ValorGenerado />

      <ComparadorPlanes
        onElegir={(elegido, periodo) => setFlujo({ plan: elegido, periodo })}
        onPasarAGratis={() => setCancelando(true)}
        onAviso={setAviso}
      />

      <HistorialPagos />

      <PreguntasFrecuentes />

      <section aria-labelledby="plan-demo">
        <details className="rounded-lg border border-dashed border-fg/40 bg-canvas">
          <summary className="flex min-h-12 cursor-pointer items-center gap-2 rounded-lg px-4 py-3 text-body font-semibold">
            <FlaskIcon aria-hidden size={22} />
            <span id="plan-demo">Sólo para la demo</span>
          </summary>
          <div className="flex flex-col items-start gap-3 px-4 pb-4">
            <p className="m-0 text-body text-soft">
              Para ver cómo queda Ensambla cuando la prueba se termina, podés adelantarla sin
              esperar 7 días. Vas a pasar al plan Gratis: tus datos quedan y algunas funciones
              quedan con candado.
            </p>
            <Button
              variant="secondary"
              disabled={!enPrueba}
              onClick={() => {
                simularFinDePrueba();
                setAviso('Adelantamos el final de la prueba: ahora estás en el plan Gratis.');
              }}
            >
              Adelantar el fin de la prueba
            </Button>
            {!enPrueba && (
              <p className="m-0 text-hint text-muted">
                Sólo se puede mientras hay una prueba en curso.
              </p>
            )}
          </div>
        </details>
      </section>

      {flujo && (
        <FlujoSuscripcion
          plan={flujo.plan}
          periodoInicial={flujo.periodo}
          onClose={() => setFlujo(null)}
        />
      )}

      <ConfirmDialog
        open={cancelando}
        titulo={enPrueba ? '¿Terminar la prueba ahora?' : '¿Cancelar tu suscripción?'}
        confirmar={enPrueba ? 'Sí, terminar la prueba' : 'Sí, cancelar'}
        conservar={enPrueba ? 'No, seguir probando' : 'No, mantener mi plan'}
        onConfirmar={confirmarCancelacion}
        onConservar={() => setCancelando(false)}
      >
        {enPrueba ? (
          <p className="m-0">
            Pasás hoy al plan Gratis y dejás de tener {PLANES['negocio-plus'].nombre}.
          </p>
        ) : seguirHasta ? (
          <p className="m-0">
            Seguís con el plan {plan.definicion.nombre} hasta el <strong>{seguirHasta}</strong>: lo
            que ya pagaste no se pierde.
          </p>
        ) : (
          <p className="m-0">Pasás al plan Gratis.</p>
        )}
        {seVaAPerder.length > 0 && (
          <div className="flex flex-col gap-1.5">
            <p className="m-0 font-semibold text-fg">Después de esa fecha se bloquea:</p>
            <ul className="m-0 flex flex-col gap-1 pl-5 text-hint">
              {seVaAPerder.map((nombre) => (
                <li key={nombre}>{nombre}</li>
              ))}
            </ul>
          </div>
        )}
        <p className="m-0 font-semibold text-fg">
          No borramos tus datos: vas a seguir con el plan Gratis.
        </p>
      </ConfirmDialog>

      {aviso && <Toast tipo="exito" mensaje={aviso} onClose={() => setAviso(null)} />}
    </div>
  );
}
