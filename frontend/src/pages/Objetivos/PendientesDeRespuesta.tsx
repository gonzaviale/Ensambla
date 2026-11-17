import { useState } from 'react';
import { usePreguntasDeSami } from '@/hooks/objetivos/usePreguntasDeSami';
import type { RespuestaPendiente } from '@/types/objetivos';
import { MiniManualCard } from './autogestion/MiniManualCard';

// Lo que Sami espera saber, arriba de los objetivos. Es un recordatorio sin apuro: una pregunta por vez, sin
// contadores ni plazos, que la persona puede dejar para mañana.
export function PendientesDeRespuesta() {
  const { primera, otras, posponer } = usePreguntasDeSami();
  const [verOtras, setVerOtras] = useState(false);
  if (!primera) return null;
  return (
    <section className="objetivos-pendientes" aria-labelledby="objetivos-pendientes-titulo">
      <h2 id="objetivos-pendientes-titulo">Sami quiere saber cómo te fue</h2>
      <p className="objetivos-pendientes-calma">Cuando puedas. Si hoy no, te lo vuelve a preguntar mañana.</p>
      <div className="objetivos-pendientes-lista">
        <PreguntaDeSami pregunta={primera} onMasTarde={posponer} />
        {verOtras && otras.map((otra) => (
          <PreguntaDeSami key={otra.miniObjetivo.id} pregunta={otra} onMasTarde={posponer} />
        ))}
      </div>
      <BotonVerOtras cantidad={otras.length} abierto={verOtras} onAlternar={() => setVerOtras(!verOtras)} />
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
        className="objetivos-textual"
        aria-label={`Más tarde: ${miniObjetivo.titulo}`}
        onClick={() => onMasTarde(miniObjetivo.id)}
      >
        Más tarde
      </button>
    </div>
  );
}

function BotonVerOtras({
  cantidad,
  abierto,
  onAlternar,
}: {
  cantidad: number;
  abierto: boolean;
  onAlternar: () => void;
}) {
  if (cantidad === 0) return null;
  const texto = abierto ? 'Ver menos' : `Ver las otras ${cantidad}`;
  return (
    <button type="button" className="objetivos-textual" aria-expanded={abierto} onClick={onAlternar}>
      {texto}
    </button>
  );
}
