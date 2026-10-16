import { Link } from '@tanstack/react-router';
import { CaretDownIcon } from '@phosphor-icons/react';
import { BotonRecorrido } from '@/components/Recorrido/BotonRecorrido';
import { Panel, buttonClasses } from '@/components/ui';
import { useRecorrido } from '@/hooks/recorrido/useRecorrido';

const PREGUNTAS: Array<{ pregunta: string; respuesta: string }> = [
  {
    pregunta: '¿Qué es un bloque?',
    respuesta:
      'Es una pieza de tu sistema, por ejemplo «Ventas», «Clientes» o «Stock». Cada bloque guarda un tipo de dato de tu negocio y vos nunca armás tablas ni cálculos: Ensambla lo hace por vos.',
  },
  {
    pregunta: '¿Cómo se arma mi sistema?',
    respuesta:
      'Le contás a Ensambla qué hacés, con tus palabras. Con eso elige los bloques que te sirven y los arma. Después podés sumar más bloques cuando quieras, de a uno.',
  },
  {
    pregunta: '¿Qué desbloquea cada bloque?',
    respuesta:
      'Cada bloque responde preguntas con plata: cuánto te quedó, quién te debe, qué te falta. En «Respuestas» ves qué bloque necesita cada una y cuáles ya están listas.',
  },
  {
    pregunta: '¿Tengo que sacar fotos?',
    respuesta:
      'No. Podés contar qué pasó escribiendo, como si le mandaras un mensaje a alguien. La foto del cuaderno es un atajo opcional para traer tu historia de una vez.',
  },
  {
    pregunta: '¿Qué pasa con mis datos si vuelvo al plan Gratis?',
    respuesta:
      'No perdés nada. Lo que cargaste queda guardado: sólo se bloquean algunas funciones (más bloques, respuestas completas, comparativas) hasta que quieras volver a activarlas.',
  },
];

// Ayuda: la bienvenida, el recorrido guiado y preguntas frecuentes en <details> (andan con teclado).
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
          Un recorrido de unos 3 minutos por toda la app, sobre la pantalla real. Lo podés dejar
          cuando quieras.
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
        className={buttonClasses({ variant: 'secondary', className: 'self-start' })}
      >
        Ver la bienvenida otra vez
      </Link>
    </Panel>
  );
}
