import { CheckCircleIcon } from '@phosphor-icons/react';
import { ESTADO_DE_MINI } from '@/lib/objetivosAutogestion';
import type { MiniManual } from '@/types/objetivos';
import { RespuestaRapida } from './RespuestaRapida';

type Props = {
  mini: MiniManual;
  objetivoId: string;
  /** Si Sami todavía espera que la persona cuente cómo le fue con este manual. */
  pendiente: boolean;
  /** El objetivo al que pertenece, cuando la tarjeta se ve fuera de él (en la lista de pendientes). */
  objetivoTexto?: string;
};

// Un mini-objetivo manual: no tiene números, es una acción que la persona hace y después le cuenta a Sami con dos
// toques. Si todavía no contó cómo le fue se ve sin alarma, como algo para hacer cuando pueda.
export function MiniManualCard({ mini, objetivoId, pendiente, objetivoTexto }: Props) {
  const idTitulo = `mini-${mini.id}`;
  const enCurso = mini.estado === 'EN_CURSO';
  return (
    <article
      className="mini-objetivo mini-manual"
      data-pendiente={enCurso && pendiente ? 'true' : undefined}
      aria-labelledby={idTitulo}
    >
      <header>
        <span className="mini-objetivo-numero" aria-hidden>
          {mini.posicion}
        </span>
        <div>
          <p className="objetivos-etiqueta">
            {objetivoTexto ??
              (enCurso && pendiente ? 'Para contarle a Sami' : ESTADO_DE_MINI[mini.estado])}
          </p>
          <h3 id={idTitulo}>{mini.titulo}</h3>
        </div>
      </header>
      {mini.justificacion && <p className="mini-objetivo-porque">{mini.justificacion}</p>}

      {enCurso &&
        (pendiente ? (
          <RespuestaRapida objetivoId={objetivoId} mini={mini} />
        ) : (
          <p className="mini-objetivo-decidido si" role="status">
            <CheckCircleIcon aria-hidden size={16} weight="bold" /> Ya se lo contaste a Sami. El
            domingo arma el próximo.
          </p>
        ))}
    </article>
  );
}
