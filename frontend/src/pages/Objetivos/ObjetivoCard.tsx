import { CalendarCheckIcon, CheckCircleIcon, TrashIcon } from '@phosphor-icons/react';
import { useMiniObjetivos, useSemanasObjetivo } from '@/hooks/objetivos/queries';
import { ETIQUETA_DE_TIPO } from '@/lib/objetivosSami';
import { resumenSinVer } from '@/lib/objetivosAutogestion';
import {
  avance,
  brecha,
  formatoPesos,
  resultadoDelMes,
  tieneMeta,
  variacion,
  type Objetivo,
  type ObjetivoConMeta,
} from '@/lib/objetivos';
import { MinisDeLaTarjeta } from './MinisDeLaTarjeta';
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
    <article className="objetivos-tarjeta dinamico" data-tipo={objetivo.tipo}>
      <header>
        <div>
          <p className="objetivos-etiqueta">Tu objetivo</p>
          <h2>{objetivo.texto}</h2>
        </div>
        <span className="objetivos-tipo">{ETIQUETA_DE_TIPO[objetivo.tipo]}</span>
      </header>
      <MinisDeLaTarjeta minis={minis} />
      <footer className="objetivos-tarjeta-acciones">
        <button type="button" className="btn secondary" onClick={onVerSemana}>
          <CalendarCheckIcon aria-hidden size={18} /> Ver mi semana
        </button>
        {resumenListo && <span className="objetivos-chip-resumen">Resumen semanal disponible</span>}
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
          <strong>{resultadoDelMes(objetivo).monto}</strong>
          <span>{resultadoDelMes(objetivo).etiqueta}</span>
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
        {resumenListo && <span className="objetivos-chip-resumen">Resumen semanal disponible</span>}
        <button type="button" className="objetivos-textual" onClick={onQuitar}>
          <TrashIcon aria-hidden size={16} /> Quitar objetivo
        </button>
      </footer>
    </article>
  );
}
