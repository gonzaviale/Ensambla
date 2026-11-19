import { useId, useState } from 'react';
import { PencilSimpleIcon, XIcon } from '@phosphor-icons/react';
import {
  MAXIMO_DE_LA_JUSTIFICACION,
  MAXIMO_DEL_TITULO,
  problemaDelManual,
  type MiniEditable,
} from '@/lib/objetivosSami';
import { partesDeMini } from '@/lib/objetivosAutogestion';

type Props = {
  mini: MiniEditable;
  posicion: number;
  onCambiar: (cambios: Partial<Pick<MiniEditable, 'titulo' | 'justificacion'>>) => void;
  onQuitar: () => void;
};

// Un mini-objetivo que Sami propone antes de guardar. El medido sólo se puede quitar: sus números los calcula el
// servidor. El manual se puede retocar.
export function MiniPropuestoCard({ mini, posicion, onCambiar, onQuitar }: Props) {
  const [editando, setEditando] = useState(false);
  const id = useId();
  const { propuesto } = mini;
  const manual = propuesto.modo === 'MANUAL';
  const problema = problemaDelManual(mini);
  const hoy =
    propuesto.tipo && propuesto.valorInicial !== null
      ? partesDeMini(propuesto.tipo, propuesto.valorInicial)
      : null;
  const meta =
    propuesto.tipo && propuesto.meta !== null ? partesDeMini(propuesto.tipo, propuesto.meta) : null;

  return (
    <li className="sami-mini" data-modo={manual ? 'manual' : 'medido'}>
      <div className="sami-mini-cabecera">
        <span className="mini-objetivo-numero" aria-hidden>
          {posicion}
        </span>
        <p className="objetivos-etiqueta">{manual ? 'Me lo contás vos' : 'Lo mide Sami'}</p>
        <button
          type="button"
          className="objetivos-textual"
          aria-label={`Quitar «${mini.titulo}»`}
          onClick={onQuitar}
        >
          <XIcon aria-hidden size={16} /> Quitar
        </button>
      </div>

      {editando ? (
        <div className="sami-mini-edicion">
          <div className="field">
            <label htmlFor={`${id}-titulo`}>Qué vas a hacer</label>
            <input
              id={`${id}-titulo`}
              value={mini.titulo}
              maxLength={MAXIMO_DEL_TITULO}
              onChange={(evento) => onCambiar({ titulo: evento.target.value })}
            />
          </div>
          <div className="field">
            <label htmlFor={`${id}-porque`}>Para qué</label>
            <textarea
              id={`${id}-porque`}
              rows={2}
              value={mini.justificacion}
              maxLength={MAXIMO_DE_LA_JUSTIFICACION}
              onChange={(evento) => onCambiar({ justificacion: evento.target.value })}
            />
          </div>
          {problema && (
            <p className="input-hint" role="alert">
              {problema}
            </p>
          )}
          <button
            type="button"
            className="btn secondary"
            disabled={problema !== null}
            onClick={() => setEditando(false)}
          >
            Listo
          </button>
        </div>
      ) : (
        <>
          <h4>{mini.titulo}</h4>
          <p className="mini-objetivo-porque">{mini.justificacion}</p>
          {hoy && meta && (
            <p className="sami-mini-numeros">
              Hoy <strong>{hoy.numero}</strong> {hoy.unidad} · Meta <strong>{meta.numero}</strong>{' '}
              {meta.unidad}
            </p>
          )}
          {manual && (
            <button
              type="button"
              className="objetivos-textual"
              aria-label={`Editar «${mini.titulo}»`}
              onClick={() => setEditando(true)}
            >
              <PencilSimpleIcon aria-hidden size={16} /> Editar
            </button>
          )}
        </>
      )}
    </li>
  );
}
