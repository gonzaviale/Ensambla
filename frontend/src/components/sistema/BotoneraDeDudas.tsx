import { useState, type FormEvent } from 'react';
import type { OpcionesDeSami } from '@/types/samiTaller';
import { Icon } from './Icon';

type BotoneraDeDudasProps = {
  dudas: OpcionesDeSami[];
  /** Cuántas dudas ya se respondieron o saltearon: la que se ve es `dudas[respondidas]`. */
  respondidas: number;
  pensando: boolean;
  /** Responde la duda a la vista; `null` la saltea. */
  onResponder: (respuesta: string | null) => void;
  /** Con una sola duda, dejar la botonera y escribir en el cuadro de siempre. */
  onEscribirOtraCosa: () => void;
};

// Las dudas de Sami con respuestas rápidas. Con varias se muestran de a una («Duda 1 de 3»): al elegir una opción, al
// escribir otra respuesta o al saltearla, pasa a la siguiente, y recién al terminar viajan todas juntas a Sami. Con
// una sola es la botonera de siempre.
export function BotoneraDeDudas({
  dudas,
  respondidas,
  pensando,
  onResponder,
  onEscribirOtraCosa,
}: BotoneraDeDudasProps) {
  const [escribiendo, setEscribiendo] = useState(false);
  const [escrito, setEscrito] = useState('');
  const duda = dudas[respondidas];
  if (!duda) return null;
  const varias = dudas.length > 1;

  const responder = (respuesta: string | null) => {
    setEscribiendo(false);
    setEscrito('');
    onResponder(respuesta);
  };
  const enviarEscrito = (evento: FormEvent) => {
    evento.preventDefault();
    if (escrito.trim()) responder(escrito.trim());
  };

  return (
    <div className="conversation-examples" role="group" aria-label="Respuestas rápidas">
      {varias && (
        <small className="duda-progreso">
          Duda {respondidas + 1} de {dudas.length}
        </small>
      )}
      <span>{duda.pregunta}</span>
      {duda.opciones.map((opcion) => (
        <button key={opcion} type="button" disabled={pensando} onClick={() => responder(opcion)}>
          {opcion}
          <Icon name="arrow" />
        </button>
      ))}
      {varias && escribiendo ? (
        <form className="duda-otra" onSubmit={enviarEscrito}>
          <input
            aria-label="Tu respuesta a esta duda"
            autoFocus
            maxLength={200}
            value={escrito}
            onChange={(evento) => setEscrito(evento.target.value)}
          />
          <button type="submit" disabled={pensando || !escrito.trim()}>
            Responder
          </button>
        </form>
      ) : (
        <button
          type="button"
          data-accion="escribir-otra-cosa"
          disabled={pensando}
          onClick={varias ? () => setEscribiendo(true) : onEscribirOtraCosa}
        >
          <Icon name="edit" />
          {varias ? 'Escribir otra respuesta…' : 'Escribir otra cosa…'}
        </button>
      )}
      {varias && (
        <button
          type="button"
          data-accion="saltear-duda"
          disabled={pensando}
          onClick={() => responder(null)}
        >
          Saltear esta duda
        </button>
      )}
    </div>
  );
}
