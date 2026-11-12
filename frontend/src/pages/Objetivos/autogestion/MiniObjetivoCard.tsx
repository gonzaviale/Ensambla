import { ArrowsClockwiseIcon, CheckIcon, ClockIcon, XIcon } from '@phosphor-icons/react';
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
  onCambiar: () => void;
  onQuitar: () => void;
};

// Un mini-objetivo en curso: qué hacer, por qué, dónde está hoy frente a su meta y qué puede decidir la persona.
export function MiniObjetivoCard({
  mini,
  decidido,
  ocupado,
  onDecidir,
  onCambiar,
  onQuitar,
}: Props) {
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
        {decidido ? (
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
              <ClockIcon aria-hidden size={16} /> Ahora no
            </button>
          </>
        )}
      </div>

      <footer className="mini-objetivo-pie">
        <button
          type="button"
          className="objetivos-textual"
          disabled={ocupado}
          aria-label={`Cambiar «${mini.titulo}» por otro`}
          onClick={onCambiar}
        >
          <ArrowsClockwiseIcon aria-hidden size={16} /> Cambiar por otro
        </button>
        <button
          type="button"
          className="objetivos-textual quitar"
          disabled={ocupado}
          aria-label={`Quitar «${mini.titulo}»`}
          onClick={onQuitar}
        >
          <XIcon aria-hidden size={16} /> Quitar
        </button>
      </footer>
    </article>
  );
}
