import { useState } from 'react';
import { CaretLeftIcon, CaretRightIcon } from '@phosphor-icons/react';
import { usePreguntasDeSami } from '@/hooks/objetivos/usePreguntasDeSami';
import type { RespuestaPendiente } from '@/types/objetivos';
import { MiniManualCard } from './autogestion/MiniManualCard';

// Lo que Sami espera saber, arriba de los objetivos. Es un recordatorio sin apuro: una pregunta por página, siempre en
// el mismo lugar, que se pasa con las flechas, y que la persona puede dejar para mañana.
export function PendientesDeRespuesta() {
  const { preguntas, posponer } = usePreguntasDeSami();
  const [indice, setIndice] = useState(0);
  const actual = Math.min(indice, preguntas.length - 1);
  const pregunta = preguntas[actual];
  if (!pregunta) return null;
  return (
    <section className="objetivos-pendientes" aria-labelledby="objetivos-pendientes-titulo">
      <h2 id="objetivos-pendientes-titulo">Sami quiere saber cómo te fue</h2>
      <p className="objetivos-pendientes-calma">Respondé cuando puedas. Si hoy no podés, Sami vuelve a preguntar mañana.</p>
      <PreguntaDeSami key={pregunta.miniObjetivo.id} pregunta={pregunta} onMasTarde={posponer} />
      <PaginadorDePreguntas total={preguntas.length} actual={actual} onIr={setIndice} />
    </section>
  );
}

function PreguntaDeSami({
  pregunta,
  onMasTarde,
}: {
  pregunta: RespuestaPendiente;
  onMasTarde: (miniId: string) => void;
}) {
  const { objetivoId, objetivoTexto, miniObjetivo } = pregunta;
  return (
    <div className="objetivos-pregunta">
      <MiniManualCard mini={miniObjetivo} objetivoId={objetivoId} objetivoTexto={objetivoTexto} pendiente />
      <button
        type="button"
        className="btn ghost"
        aria-label={`Más tarde: ${miniObjetivo.titulo}`}
        onClick={() => onMasTarde(miniObjetivo.id)}
      >
        Más tarde
      </button>
    </div>
  );
}

type PaginadorProps = { total: number; actual: number; onIr: (indice: number) => void };

function PaginadorDePreguntas({ total, actual, onIr }: PaginadorProps) {
  if (total < 2) return null;
  return (
    <nav className="objetivos-paginador" aria-label="Preguntas de Sami">
      <button type="button" aria-label="Pregunta anterior" disabled={actual === 0} onClick={() => onIr(actual - 1)}>
        <CaretLeftIcon aria-hidden size={18} weight="bold" />
      </button>
      <span>
        {actual + 1} de {total}
      </span>
      <button
        type="button"
        aria-label="Pregunta siguiente"
        disabled={actual === total - 1}
        onClick={() => onIr(actual + 1)}
      >
        <CaretRightIcon aria-hidden size={18} weight="bold" />
      </button>
    </nav>
  );
}
