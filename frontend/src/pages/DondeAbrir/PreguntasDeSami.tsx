import { ArrowLeftIcon } from '@phosphor-icons/react';
import { Sami } from '@/components/sistema/Sami';
import { useConversacionConSami } from '@/hooks/dondeAbrir/useConversacionConSami';
import { ayudaDeLaOpcion, tituloDeLaRespuesta, type Pregunta } from '@/lib/preguntasDeSami';
import type { RespuestasDeSami } from '@/lib/respuestasDeSami';

type Props = {
  respuestas: RespuestasDeSami;
  onResponder: (respuestas: RespuestasDeSami) => void;
  onTerminar: () => void;
};

type Conversacion = ReturnType<typeof useConversacionConSami>;

function LoQueYaMeContaste({
  preguntas,
  respuestas,
}: {
  preguntas: Pregunta[];
  respuestas: RespuestasDeSami;
}) {
  return (
    <ol className="donde-charla" aria-label="Lo que ya me contaste">
      {preguntas.map((pregunta) => (
        <li key={pregunta.clave}>
          <span className="charla-etiqueta">{pregunta.etiqueta}</span>
          <span className="charla-respuesta">
            {tituloDeLaRespuesta(pregunta, respuestas[pregunta.clave])}
          </span>
        </li>
      ))}
    </ol>
  );
}

function SamiPregunta({
  pregunta,
  conversacion,
}: {
  pregunta: Pregunta;
  conversacion: Conversacion;
}) {
  return (
    <div className="donde-sami-dice">
      <div className="donde-sami-cara" aria-hidden="true">
        <Sami pose="think" />
      </div>
      <div className="donde-globo">
        <small>
          Pregunta {conversacion.numero} de {conversacion.total}
        </small>
        <h2 id="donde-pregunta-titulo">{pregunta.texto}</h2>
      </div>
    </div>
  );
}

function OpcionesDeLaPregunta({
  pregunta,
  conversacion,
}: {
  pregunta: Pregunta;
  conversacion: Conversacion;
}) {
  return (
    <div className="donde-opciones-grandes" role="group" aria-labelledby="donde-pregunta-titulo">
      {pregunta.opciones.map((opcion) => (
        <button
          key={opcion.valor}
          type="button"
          className="donde-opcion"
          onClick={() => conversacion.responder(pregunta, opcion.valor)}
        >
          <strong>{opcion.titulo}</strong>
          <span>{ayudaDeLaOpcion(opcion, conversacion.localidad)}</span>
        </button>
      ))}
    </div>
  );
}

function VolverALaAnterior({ conversacion }: { conversacion: Conversacion }) {
  if (!conversacion.puedeVolver) return null;
  return (
    <button type="button" className="donde-volver" onClick={conversacion.volver}>
      <ArrowLeftIcon aria-hidden size={14} /> Volver a la pregunta anterior
    </button>
  );
}

function PreguntaActual({
  pregunta,
  conversacion,
}: {
  pregunta: Pregunta;
  conversacion: Conversacion;
}) {
  return (
    <div className="donde-pregunta-actual">
      <SamiPregunta pregunta={pregunta} conversacion={conversacion} />
      <OpcionesDeLaPregunta pregunta={pregunta} conversacion={conversacion} />
      <VolverALaAnterior conversacion={conversacion} />
    </div>
  );
}

// Sami pregunta de a una. Lo ya respondido queda arriba como fichas con lo que elegiste; al contestar la última, muestra las zonas.
export function PreguntasDeSami(props: Props) {
  const conversacion = useConversacionConSami(props);
  const { actual, anteriores } = conversacion;
  return (
    <section className="donde-paso" aria-labelledby="donde-pregunta-titulo">
      <LoQueYaMeContaste preguntas={anteriores} respuestas={props.respuestas} />
      {actual && <PreguntaActual pregunta={actual} conversacion={conversacion} />}
    </section>
  );
}
