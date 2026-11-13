import { ArrowCounterClockwiseIcon, CheckIcon, ClockIcon } from '@phosphor-icons/react';
import {
  ESTADO_DE_MINI,
  QUE_SE_MIDE,
  faltaParaLaMeta,
  partesDeMini,
} from '@/lib/objetivosAutogestion';
import type { DecisionDePaso, MiniObjetivo } from '@/types/objetivos';
import { Progreso } from '../piezas';

type Props = {
  mini: MiniObjetivo;
  /** Lo que la persona ya decidió esta semana sobre este mini, si decidió algo. */
  decidido?: DecisionDePaso;
  ocupado?: boolean;
  onDecidir: (tipo: DecisionDePaso) => void;
  /** Vuelve a mostrar «Lo hago» y «Dejarlo para después» en un mini que se dejó para después. */
  onRestaurar: () => void;
};

// Un mini-objetivo en curso: qué hacer, por qué, dónde está hoy frente a su meta y qué puede decidir la persona.
// Uno logrado queda en su lugar hasta el cierre del domingo, cuando Sami le sube la meta o propone otro.
export function MiniObjetivoCard({ mini, decidido, ocupado, onDecidir, onRestaurar }: Props) {
  const logrado = mini.estado === 'LOGRADO';
  const porcentaje = Math.round(mini.avance * 100);
  const idTitulo = `mini-${mini.id}`;
  const hoy = partesDeMini(mini.tipo, mini.valorActual);
  const meta = partesDeMini(mini.tipo, mini.meta);
  const falta = faltaParaLaMeta(mini);
  return (
    <article className="mini-objetivo" aria-labelledby={idTitulo}>
      <header>
        <span className="mini-objetivo-numero" aria-hidden>
          {mini.posicion}
        </span>
        <div>
          <p className="objetivos-etiqueta">{ESTADO_DE_MINI[mini.estado]}</p>
          <h3 id={idTitulo}>{mini.titulo}</h3>
        </div>
      </header>
      <p className="mini-objetivo-porque">{mini.justificacion}</p>

      <div className="mini-objetivo-medida">
        <dl>
          <div className="hoy">
            <dt>{QUE_SE_MIDE[mini.tipo]} hoy</dt>
            <dd>
              <strong>{hoy.numero}</strong> <span>{hoy.unidad}</span>
            </dd>
          </div>
          <div className="meta">
            <dt>Meta</dt>
            <dd>
              <strong>{meta.numero}</strong> <span>{meta.unidad}</span>
            </dd>
          </div>
        </dl>
        <Progreso valor={mini.avance} etiqueta={`Avance de «${mini.titulo}»: ${porcentaje} %`} />
        <p className="mini-objetivo-falta">
          {falta ? `Te faltan ${falta}` : '¡Llegaste a la meta!'}
        </p>
      </div>

      <div className="mini-objetivo-decision">
        {logrado ? (
          <p className="mini-objetivo-decidido si" role="status">
            <CheckIcon aria-hidden size={16} weight="bold" /> Meta alcanzada: el domingo Sami te
            sube la meta o te propone otro
          </p>
        ) : decidido ? (
          <p
            className={`mini-objetivo-decidido ${decidido === 'ACEPTADO' ? 'si' : 'despues'}`}
            role="status"
          >
            {decidido === 'ACEPTADO' ? (
              <>
                <CheckIcon aria-hidden size={16} weight="bold" /> Esta semana dijiste que lo hacés
              </>
            ) : (
              <>
                <ClockIcon aria-hidden size={16} weight="bold" /> Lo dejaste para después
                <button
                  type="button"
                  className="mini-objetivo-restaurar"
                  disabled={ocupado}
                  aria-label={`Restaurar «${mini.titulo}» para volver a decidir`}
                  onClick={onRestaurar}
                >
                  <ArrowCounterClockwiseIcon aria-hidden size={16} weight="bold" /> Restaurar
                </button>
              </>
            )}
          </p>
        ) : (
          <>
            <button
              type="button"
              className="btn primary"
              disabled={ocupado}
              onClick={() => onDecidir('ACEPTADO')}
            >
              <CheckIcon aria-hidden size={16} /> Lo hago
            </button>
            <button
              type="button"
              className="btn secondary"
              disabled={ocupado}
              onClick={() => onDecidir('IGNORADO')}
            >
              <ClockIcon aria-hidden size={16} /> Dejarlo para después
            </button>
          </>
        )}
      </div>
    </article>
  );
}
