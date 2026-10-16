import { CaretDownIcon } from '@phosphor-icons/react';
import { DIAS_DE_PRUEBA, MESES_PAGOS_ANUAL } from '@/lib/planes/planes';

const PREGUNTAS: { pregunta: string; respuesta: string }[] = [
  {
    pregunta: '¿Qué pasa con mis datos si vuelvo al plan Gratis?',
    respuesta:
      'No se borra nada: tus bloques, tus cargas y tu historial quedan como están. Sólo se bloquean las funciones del plan pago (con un candado y la invitación a volver) y las respuestas que necesitan el plan completo. El plan Gratis incluye 3 bloques y 2 cargas de historia por mes.',
  },
  {
    pregunta: '¿Cuándo se cobra?',
    respuesta: `Durante la prueba no se cobra nada y no hace falta cargar un medio de pago. Cuando elegís un plan, el cobro es el día que lo activás y se renueva cada mes (o cada año, pagando ${MESES_PAGOS_ANUAL} meses y usando 12). Antes de cada renovación podés cancelar.`,
  },
  {
    pregunta: '¿Cómo cancelo?',
    respuesta:
      'Desde esta pantalla, en «Tu plan hoy», con el botón «Cancelar suscripción». Te pedimos confirmar y seguís con lo que ya pagaste hasta la fecha del próximo cobro. Si cambiás de idea, lo volvés a activar.',
  },
  {
    pregunta: '¿Por qué la prueba es del plan completo?',
    respuesta: `Porque lo que querés saber es si Ensambla te sirve de verdad, y eso se nota con todas las respuestas y todos los bloques habilitados. Son ${DIAS_DE_PRUEBA} días, una sola vez, y al terminar volvés al plan Gratis sin perder nada.`,
  },
];

export function PreguntasFrecuentes() {
  return (
    <section aria-labelledby="plan-faq" className="flex flex-col gap-4">
      <h2 id="plan-faq" className="m-0 text-h3">
        Preguntas frecuentes
      </h2>
      <div className="flex flex-col gap-3">
        {PREGUNTAS.map((item) => (
          <details
            key={item.pregunta}
            className="group rounded-lg border border-line/60 bg-surface shadow-sm"
          >
            <summary className="flex min-h-12 cursor-pointer list-none items-center justify-between gap-3 rounded-lg px-4 py-3 text-body font-semibold [&::-webkit-details-marker]:hidden">
              {item.pregunta}
              <CaretDownIcon
                aria-hidden
                size={22}
                className="shrink-0 transition-transform group-open:rotate-180"
              />
            </summary>
            <p className="m-0 px-4 pb-4 text-body text-soft">{item.respuesta}</p>
          </details>
        ))}
      </div>
    </section>
  );
}
