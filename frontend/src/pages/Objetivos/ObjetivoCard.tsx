import { useId, useState } from 'react';
import { CheckCircleIcon, SparkleIcon, TrashIcon } from '@phosphor-icons/react';
import {
  avance,
  brecha,
  caminos,
  formatoPesos,
  ultimoValor,
  variacion,
  type Objetivo,
} from '@/lib/objetivos';
import { Explicacion, Progreso } from './piezas';

type Props = {
  objetivo: Objetivo;
  onPlan: () => void;
  onProbar: () => void;
  onQuitar: () => void;
};

function textoDeTendencia(objetivo: Objetivo) {
  const semanas = objetivo.historial.length - 1;
  if (semanas < 1) return 'Todavía no hay semanas para comparar. Sami lo va midiendo.';
  const cambio = variacion(objetivo);
  const plazo = semanas === 1 ? 'la última semana' : `las últimas ${semanas} semanas`;
  if (cambio === 0) return `Sin cambios en ${plazo}.`;
  return `${cambio > 0 ? 'Subió' : 'Bajó'} ${formatoPesos(Math.abs(cambio))} en ${plazo}.`;
}

export function ObjetivoCard({ objetivo, onPlan, onProbar, onQuitar }: Props) {
  const [recomendacionAbierta, setRecomendacionAbierta] = useState(false);
  const idPanel = useId();
  const cumplido = brecha(objetivo) === 0;
  const porcentaje = Math.round(avance(objetivo) * 100);
  const maximo = Math.max(1, ...objetivo.historial.map((punto) => punto.valor));

  return (
    <article className="objetivos-tarjeta">
      <header>
        <div>
          <p className="objetivos-etiqueta">Meta mensual · {formatoPesos(objetivo.meta)}</p>
          <h2>{objetivo.texto}</h2>
        </div>
        {cumplido ? (
          <span className="objetivos-cumplido">
            <CheckCircleIcon aria-hidden size={20} /> Objetivo alcanzado
          </span>
        ) : (
          <button
            type="button"
            className={`objetivos-recomendacion ${recomendacionAbierta ? 'abierta' : ''}`.trim()}
            aria-expanded={recomendacionAbierta}
            aria-controls={idPanel}
            onClick={() => setRecomendacionAbierta((abierta) => !abierta)}
          >
            <SparkleIcon aria-hidden size={20} weight="fill" />
            <span>Recomendación de Sami</span>
            {!recomendacionAbierta && <i aria-hidden className="objetivos-punto" />}
          </button>
        )}
      </header>

      <div className="objetivos-tarjeta-avance">
        <div>
          <div className="objetivos-avance-cifras">
            <strong>{formatoPesos(ultimoValor(objetivo))}</strong>
            <span>
              de {formatoPesos(objetivo.meta)} · {porcentaje} %
            </span>
          </div>
          <Progreso
            valor={avance(objetivo)}
            etiqueta={`Avance hacia ${formatoPesos(objetivo.meta)}`}
          />
          <p className="objetivos-tendencia-texto">{textoDeTendencia(objetivo)}</p>
        </div>
        <div
          className="objetivos-tendencia"
          role="img"
          aria-label={`Resultado mensual estimado: ${objetivo.historial
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

      {recomendacionAbierta && (
        <div id={idPanel} className="objetivos-recomendacion-panel">
          <h3>Recomendación para tu objetivo</h3>
          <p>
            Te faltan {formatoPesos(brecha(objetivo))} por mes. Son tres caminos calculados sobre tu
            estructura actual de ventas, costos y gastos.
          </p>
          <div className="objetivos-caminos">
            {caminos(objetivo).map((camino) => (
              <article
                key={camino.clave}
                className={`objetivos-camino ${camino.recomendado ? 'recomendado' : ''}`.trim()}
              >
                {camino.recomendado && (
                  <span className="objetivos-recomendado">Recomendado por equilibrio</span>
                )}
                <h4>{camino.titulo}</h4>
                <p>{camino.bajada}</p>
                <dl>
                  {camino.datos.map((dato) => (
                    <div key={dato.nombre}>
                      <dt>{dato.nombre}</dt>
                      <dd>{dato.valor}</dd>
                    </div>
                  ))}
                </dl>
                <small>
                  <CheckCircleIcon aria-hidden /> {camino.confianza}
                </small>
                {camino.recomendado && (
                  <button type="button" className="btn primary" onClick={onProbar}>
                    Probar este camino
                  </button>
                )}
              </article>
            ))}
          </div>
          <Explicacion>
            Recomiendo combinar porque aumentar solamente las ventas exige muchas operaciones
            adicionales, y subir solo los precios podría reducir la demanda. Son estimaciones de
            demostración, no promesas.
          </Explicacion>
        </div>
      )}

      <footer className="objetivos-tarjeta-acciones">
        <button type="button" className="btn primary" onClick={onPlan}>
          Ver plan de acción
        </button>
        <button type="button" className="btn secondary" onClick={onProbar}>
          Ver cómo funcionaría
        </button>
        <button type="button" className="objetivos-textual" onClick={onQuitar}>
          <TrashIcon aria-hidden size={16} /> Quitar objetivo
        </button>
      </footer>
    </article>
  );
}
