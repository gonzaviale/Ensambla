import { CalendarCheckIcon, CheckCircleIcon, TrashIcon } from '@phosphor-icons/react';
import { useMiniObjetivos, useSemanasObjetivo } from '@/hooks/objetivos/queries';
import { ETIQUETA_DE_TIPO } from '@/lib/objetivosSami';
import { resumenSinVer } from '@/lib/objetivosAutogestion';
import {
  avance,
  brecha,
  formatoPesos,
  tieneMeta,
  ultimoValor,
  variacion,
  type Objetivo,
  type ObjetivoConMeta,
} from '@/lib/objetivos';
import { Progreso } from './piezas';

type Props = {
  objetivo: Objetivo;
  onQuitar: () => void;
  /** Lleva al seguimiento semanal del objetivo: ahí están los mini-objetivos con qué hacer cada semana. */
  onVerSemana: () => void;
};

function textoDeTendencia(objetivo: Objetivo) {
  const semanas = objetivo.historial.length - 1;
  if (semanas < 1) return 'Todavía no hay semanas para comparar. Sami lo va midiendo.';
  const cambio = variacion(objetivo);
  const plazo = semanas === 1 ? 'la última semana' : `las últimas ${semanas} semanas`;
  if (cambio === 0) return `Sin cambios en ${plazo}.`;
  return `${cambio > 0 ? 'Subió' : 'Bajó'} ${formatoPesos(Math.abs(cambio))} en ${plazo}.`;
}

export function ObjetivoCard({ objetivo, onQuitar, onVerSemana }: Props) {
  return tieneMeta(objetivo) ? (
    <ObjetivoConMetaCard objetivo={objetivo} onQuitar={onQuitar} onVerSemana={onVerSemana} />
  ) : (
    <ObjetivoSinMetaCard objetivo={objetivo} onQuitar={onQuitar} onVerSemana={onVerSemana} />
  );
}

// Un objetivo sin meta en pesos (armado con Sami): no hay una cifra que mostrar, así que la tarjeta muestra sus
// mini-objetivos de la semana.
function ObjetivoSinMetaCard({ objetivo, onQuitar, onVerSemana }: Props) {
  const ultimoCierre = useSemanasObjetivo(objetivo.id).data?.ultimoCierre ?? null;
  const resumenListo = resumenSinVer(objetivo.id, ultimoCierre);
  const minis = useMiniObjetivos(objetivo.id).data?.minis ?? [];
  return (
    <article className="objetivos-tarjeta">
      <header>
        <div>
          <p className="objetivos-etiqueta">Tu objetivo · {ETIQUETA_DE_TIPO[objetivo.tipo]}</p>
          <h2>{objetivo.texto}</h2>
        </div>
      </header>
      {minis.length > 0 && (
        <ul className="objetivos-minis-resumen" aria-label="Mini-objetivos de esta semana">
          {minis.map((mini) => (
            <li key={mini.id}>{mini.titulo}</li>
          ))}
        </ul>
      )}
      <footer className="objetivos-tarjeta-acciones">
        <button type="button" className="btn secondary" onClick={onVerSemana}>
          <CalendarCheckIcon aria-hidden size={18} /> Ver mi semana
        </button>
        {resumenListo && <span className="objetivos-chip-resumen">Resumen de la semana listo</span>}
        <button type="button" className="objetivos-textual" onClick={onQuitar}>
          <TrashIcon aria-hidden size={16} /> Quitar objetivo
        </button>
      </footer>
    </article>
  );
}

function ObjetivoConMetaCard({
  objetivo,
  onQuitar,
  onVerSemana,
}: Omit<Props, 'objetivo'> & { objetivo: ObjetivoConMeta }) {
  const ultimoCierre = useSemanasObjetivo(objetivo.id).data?.ultimoCierre ?? null;
  const resumenListo = resumenSinVer(objetivo.id, ultimoCierre);
  const cumplido = brecha(objetivo) === 0;
  const maximo = Math.max(1, ...objetivo.historial.map((punto) => punto.valor));

  return (
    <article className="objetivos-tarjeta">
      <header>
        <div>
          <p className="objetivos-etiqueta">Tu objetivo</p>
          <h2>{objetivo.texto}</h2>
        </div>
        {cumplido && (
          <span className="objetivos-cumplido">
            <CheckCircleIcon aria-hidden size={20} /> Lo lograste
          </span>
        )}
      </header>

      <div className="objetivos-tarjeta-avance">
        <div>
          <div className="objetivos-avance-cifras">
            <strong>{formatoPesos(ultimoValor(objetivo))}</strong>
            <span>
              {cumplido
                ? 'ganás por mes'
                : `ganás por mes · te faltan ${formatoPesos(brecha(objetivo))}`}
            </span>
          </div>
          <Progreso
            valor={avance(objetivo)}
            etiqueta={`Lo que ganás hoy frente a ${formatoPesos(objetivo.meta)}`}
          />
          <p className="objetivos-tendencia-texto">{textoDeTendencia(objetivo)}</p>
        </div>
        <div
          className="objetivos-tendencia"
          role="img"
          aria-label={`Lo que ganaste por mes: ${objetivo.historial
            .map((punto) => `${punto.semana} ${formatoPesos(punto.valor)}`)
            .join(', ')}`}
        >
          {objetivo.historial.map((punto) => (
            <figure key={punto.semana}>
              <i style={{ height: `${Math.max(6, (punto.valor / maximo) * 100)}%` }} />
              <figcaption>{punto.semana}</figcaption>
            </figure>
          ))}
        </div>
      </div>

      <footer className="objetivos-tarjeta-acciones">
        <button type="button" className="btn secondary" onClick={onVerSemana}>
          <CalendarCheckIcon aria-hidden size={18} /> Ver mi semana
        </button>
        {resumenListo && <span className="objetivos-chip-resumen">Resumen de la semana listo</span>}
        <button type="button" className="objetivos-textual" onClick={onQuitar}>
          <TrashIcon aria-hidden size={16} /> Quitar objetivo
        </button>
      </footer>
    </article>
  );
}
