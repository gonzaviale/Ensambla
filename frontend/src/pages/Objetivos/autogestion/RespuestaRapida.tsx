import { useId, useState } from 'react';
import { CheckCircleIcon } from '@phosphor-icons/react';
import { leerErrorServidor } from '@/hooks/errores';
import { useResponderMiniObjetivo } from '@/hooks/objetivos/mutations';
import { HIZO, QUE_PASO, quePasoPosible } from '@/lib/respuestaManual';
import type { Hizo, MiniManual, QuePaso } from '@/types/objetivos';

const MAXIMO_DEL_COMENTARIO = 300;

type Props = { objetivoId: string; mini: MiniManual };

// «¿Lo hiciste?» y «¿Qué pasó?»: dos toques y, si quiere, una línea. Botones grandes para usar con el pulgar.
export function RespuestaRapida({ objetivoId, mini }: Props) {
  const responder = useResponderMiniObjetivo(objetivoId);
  const idComentario = useId();
  const [hizo, setHizo] = useState<Hizo | null>(null);
  const [quePaso, setQuePaso] = useState<QuePaso | null>(null);
  const [comentario, setComentario] = useState('');
  const error = leerErrorServidor(responder.error);

  if (responder.isSuccess) {
    return (
      <p className="mini-objetivo-decidido si" role="status">
        <CheckCircleIcon aria-hidden size={16} weight="bold" /> Listo, ya se lo contaste a Sami.
      </p>
    );
  }

  const elegirHizo = (nuevo: Hizo) => {
    setHizo(nuevo);
    // «No tuve tiempo» no combina con «lo hice»: si lo hiciste, tuviste tiempo.
    if (quePaso && !quePasoPosible(nuevo, quePaso)) setQuePaso(null);
  };

  return (
    <form
      className="respuesta-rapida"
      aria-label={`Contale a Sami cómo te fue con «${mini.titulo}»`}
      onSubmit={(evento) => {
        evento.preventDefault();
        if (!hizo || !quePaso || responder.isPending) return;
        responder.mutate({ miniId: mini.id, respuesta: { hizo, quePaso, comentario } });
      }}
    >
      <fieldset>
        <legend>¿Lo hiciste?</legend>
        <div className="respuesta-opciones">
          {HIZO.map((opcion) => (
            <button
              key={opcion.valor}
              type="button"
              aria-pressed={hizo === opcion.valor}
              onClick={() => elegirHizo(opcion.valor)}
            >
              {opcion.etiqueta}
            </button>
          ))}
        </div>
      </fieldset>

      {hizo && (
        <fieldset>
          <legend>¿Qué pasó?</legend>
          <div className="respuesta-opciones">
            {QUE_PASO.map((opcion) => (
              <button
                key={opcion.valor}
                type="button"
                aria-pressed={quePaso === opcion.valor}
                disabled={!quePasoPosible(hizo, opcion.valor)}
                onClick={() => setQuePaso(opcion.valor)}
              >
                {opcion.etiqueta}
              </button>
            ))}
          </div>
        </fieldset>
      )}

      {quePaso && (
        <div className="field">
          <label htmlFor={idComentario}>Contale un poco más (opcional)</label>
          <textarea
            id={idComentario}
            rows={2}
            maxLength={MAXIMO_DEL_COMENTARIO}
            value={comentario}
            onChange={(evento) => setComentario(evento.target.value)}
          />
        </div>
      )}

      {error && (
        <p className="input-hint" role="alert">
          {error.campos.comentario ?? error.mensaje}
        </p>
      )}
      <button
        type="submit"
        className="btn primary"
        disabled={!hizo || !quePaso || responder.isPending}
      >
        {responder.isPending ? 'Enviando…' : 'Contarle a Sami'}
      </button>
    </form>
  );
}
