import { CalendarCheckIcon, CheckCircleIcon, TrashIcon } from '@phosphor-icons/react';
import { useSemanasObjetivo } from '@/hooks/objetivos/queries';
import { resumenSinVer } from '@/lib/objetivosAutogestion';
import {
  avance,
  brecha,
  formatoPesos,
  ultimoValor,
  variacion,
  type Objetivo,
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
  if (semanas < 1) return 'Todavía no hay semanas para comparar.';
  const cambio = variacion(objetivo);
  const plazo = semanas === 1 ? 'la última semana' : `las últimas ${semanas} semanas`;
  if (cambio === 0) return `Sin cambios en ${plazo}.`;
  return `${cambio > 0 ? 'Subió' : 'Bajó'} ${formatoPesos(Math.abs(cambio))} en ${plazo}.`;
}

export function ObjetivoCard({ objetivo, onQuitar, onVerSemana }: Props) {
  const ultimoCierre = useSemanasObjetivo(objetivo.id).data?.ultimoCierre ?? null;
  const resumenListo = resumenSinVer(objetivo.id, ultimoCierre);
  const cumplido = brecha(objetivo) === 0;

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
        <div className="objetivos-avance-cifras">
          <strong>{formatoPesos(ultimoValor(objetivo))}</strong>
          <span>ganás por mes</span>
        </div>
        <div className="objetivos-avance-meta">
          <p>
            <span>Tu meta: {formatoPesos(objetivo.meta)}</span>
            <strong>
              {cumplido ? 'Meta cumplida' : `Te faltan ${formatoPesos(brecha(objetivo))}`}
            </strong>
          </p>
          <Progreso
            valor={avance(objetivo)}
            etiqueta={`Lo que ganás hoy frente a ${formatoPesos(objetivo.meta)}`}
          />
          <p className="objetivos-tendencia-texto">{textoDeTendencia(objetivo)}</p>
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
