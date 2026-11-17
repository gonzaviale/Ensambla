import { useId } from 'react';
import type { RespuestaPorPasos } from '@/hooks/objetivos/useRespuestaPorPasos';
import { HIZO, QUE_PASO, quePasoPosible } from '@/lib/respuestaManual';
import type { QuePaso } from '@/types/objetivos';

const MAXIMO_DEL_COMENTARIO = 300;

type Opcion<T extends string> = { valor: T; etiqueta: string };

type OpcionesProps<T extends string> = {
  opciones: readonly Opcion<T>[];
  elegida: T | null;
  noDisponible?: (valor: T) => boolean;
  onElegir: (valor: T) => void;
};

function Opciones<T extends string>({ opciones, elegida, noDisponible, onElegir }: OpcionesProps<T>) {
  return (
    <div className="respuesta-opciones">
      {opciones.map(({ valor, etiqueta }) => (
        <button
          key={valor}
          type="button"
          aria-pressed={elegida === valor}
          disabled={noDisponible?.(valor)}
          onClick={() => onElegir(valor)}
        >
          {etiqueta}
        </button>
      ))}
    </div>
  );
}

export function PaginaHizo({ respuesta }: { respuesta: RespuestaPorPasos }) {
  return (
    <fieldset>
      <legend>¿Lo hiciste?</legend>
      <Opciones opciones={HIZO} elegida={respuesta.hizo} onElegir={respuesta.elegirHizo} />
    </fieldset>
  );
}

export function PaginaQuePaso({ respuesta }: { respuesta: RespuestaPorPasos }) {
  const noDisponible = (quePaso: QuePaso) =>
    respuesta.hizo !== null && !quePasoPosible(respuesta.hizo, quePaso);
  return (
    <fieldset>
      <legend>¿Qué pasó?</legend>
      <Opciones
        opciones={QUE_PASO}
        elegida={respuesta.quePaso}
        noDisponible={noDisponible}
        onElegir={respuesta.elegirQuePaso}
      />
    </fieldset>
  );
}

export function PaginaComentario({ respuesta }: { respuesta: RespuestaPorPasos }) {
  const idComentario = useId();
  return (
    <div className="field">
      <label htmlFor={idComentario}>Contale un poco más (opcional)</label>
      <textarea
        id={idComentario}
        rows={3}
        maxLength={MAXIMO_DEL_COMENTARIO}
        value={respuesta.comentario}
        onChange={(evento) => respuesta.cambiarComentario(evento.target.value)}
      />
    </div>
  );
}

