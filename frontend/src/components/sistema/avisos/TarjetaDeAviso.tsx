import type { AccionDeAviso, Aviso } from '@/lib/sistema/alertas';
import { Icon } from '../Icon';
import { ICONO_DE_AVISO, textoDeAccion } from './presentacion';

const NIVEL = { 3: 'Urgente', 2: 'Para hoy', 1: 'Para mirar' } as const;

type Props = {
  aviso: Aviso;
  enfocado?: boolean;
  onAccion: (accion: AccionDeAviso) => void;
  onDescartar: (aviso: Aviso) => void;
};

// Un aviso: qué pasa, por qué lo dice Sami y qué se puede hacer (Aplicar o Descartar).
export function TarjetaDeAviso({ aviso, enfocado = false, onAccion, onDescartar }: Props) {
  return (
    <article
      className={`alerta p${aviso.prioridad} ${enfocado ? 'foco' : ''}`.trim()}
      data-alerta={aviso.id}
    >
      <span className="alerta-icono">
        <Icon name={ICONO_DE_AVISO[aviso.tipo]} />
      </span>
      <div className="alerta-cuerpo">
        <div className="alerta-meta">
          <span className="alerta-nivel">{NIVEL[aviso.prioridad]}</span>
          <span>{aviso.sistema}</span>
        </div>
        <h3>{aviso.titulo}</h3>
        <p>{aviso.detalle}</p>
        {aviso.porque.length > 0 && (
          <ul className="alerta-porque">
            {aviso.porque.map((motivo) => (
              <li key={motivo}>{motivo}</li>
            ))}
          </ul>
        )}
        <div className="alerta-acciones">
          {aviso.acciones.map((accion, indice) => (
            <button
              key={`${accion.tipo}-${indice}`}
              type="button"
              className={`btn ${indice === 0 ? 'primary' : 'secondary'} small`}
              onClick={() => onAccion(accion)}
            >
              {textoDeAccion(accion)}
            </button>
          ))}
          <button
            type="button"
            className="alerta-descartar"
            aria-label="Descartar"
            title="Descartar"
            onClick={() => onDescartar(aviso)}
          >
            <Icon name="trash" />
          </button>
        </div>
      </div>
    </article>
  );
}
