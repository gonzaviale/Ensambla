import { Link } from '@tanstack/react-router';
import { CaretDownIcon } from '@phosphor-icons/react';
import { BotonRecorrido } from '@/components/Recorrido/BotonRecorrido';
import { Panel, buttonClasses } from '@/components/ui';
import { useRecorrido } from '@/hooks/recorrido/useRecorrido';

const PREGUNTAS: Array<{ pregunta: string; respuesta: string }> = [
  {
    pregunta: '¿Qué pasa cuando me llega una lista nueva?',
    respuesta:
      'La subís (reenviada o con una foto) y te mostramos qué productos subieron o bajaron y qué precio te conviene poner. Si alguna línea no está clara, te preguntamos antes de seguir.',
  },
  {
    pregunta: '¿Ensambla cambia mis precios solo?',
    respuesta:
      'No. Los precios nuevos se proponen y vos los confirmás. Recién ahí quedan guardados, y después te los dejamos listos para cambiar las etiquetas.',
  },
  {
    pregunta: '¿Qué es un precio en riesgo?',
    respuesta:
      'Es un producto que, con el costo nuevo, te deja ganar menos que tu ganancia mínima. Te avisamos para que lo corrijas antes de vender de más barato de lo que te cuesta.',
  },
  {
    pregunta: '¿Cómo paso los cambios al mostrador?',
    respuesta:
      'En «Mostrador» ves la lista de lo que cambió. A medida que cambiás cada etiqueta, la tildás y queda anotado.',
  },
  {
    pregunta: '¿Qué pasa si se termina la prueba o cancelo?',
    respuesta:
      'Volvés al plan Gratis y no perdés nada de lo que cargaste. Sólo quedan bloqueadas algunas funciones hasta que quieras volver a activarlas.',
  },
];

// «Cómo funciona»: preguntas frecuentes en <details>, que se abren con teclado y sin JavaScript.
export function AyudaComoFunciona() {
  const { visto } = useRecorrido();

  return (
    <Panel
      aria-labelledby="cuenta-ayuda"
      data-recorrido="cuenta-ayuda"
      className="flex flex-col gap-4"
    >
      <h2 id="cuenta-ayuda" className="m-0 text-h3">
        Ayuda
      </h2>
      <div className="flex flex-col items-start gap-2">
        <p className="m-0 text-body text-soft">
          Un recorrido de unos 3 minutos por toda la app, con la pantalla real y sus datos de
          ejemplo. Lo podés dejar cuando quieras.
        </p>
        <BotonRecorrido
          variant="primary"
          etiqueta={visto ? 'Volver a empezar el recorrido guiado' : 'Empezar el recorrido guiado'}
        />
      </div>
      <div className="flex flex-col gap-2">
        {PREGUNTAS.map((item) => (
          <details
            key={item.pregunta}
            className="group rounded-lg border border-line/60 bg-canvas px-4"
          >
            <summary className="flex min-h-12 cursor-pointer list-none items-center justify-between gap-3 py-2 text-body font-semibold [&::-webkit-details-marker]:hidden">
              {item.pregunta}
              <CaretDownIcon
                aria-hidden
                size={20}
                weight="bold"
                className="shrink-0 transition-transform group-open:rotate-180"
              />
            </summary>
            <p className="m-0 pb-4 text-body text-soft">{item.respuesta}</p>
          </details>
        ))}
      </div>
      <Link
        to="/bienvenida"
        className={buttonClasses({ variant: 'primary', className: 'self-start' })}
      >
        Ver la bienvenida otra vez
      </Link>
    </Panel>
  );
}
