import { useEffect, useRef } from 'react';
import {
  ETIQUETA_DE_COINCIDENCIA,
  SIGLA_DE_JURISDICCION,
  formatoKm,
  formatoPuntaje,
  type Descartada,
  type ZonaPuntuada,
} from '@/lib/dondeAbrir';

/** Cuántos puestos subió (+) o bajó (−) una zona con el último cambio de filtros; `nueva` si antes estaba afuera. */
export type CambioDePuesto = number | 'nueva';

type Props = {
  ranking: ZonaPuntuada[];
  descartadas: Descartada[];
  seleccionadaId: string;
  cambios: Record<string, CambioDePuesto>;
  onElegir: (id: string) => void;
};

export function ListaDeZonas({ ranking, descartadas, seleccionadaId, cambios, onElegir }: Props) {
  const lista = useRef<HTMLDivElement>(null);
  // Si la zona se elige en el mapa, se desplaza únicamente esta lista. Nunca se mueve la página completa.
  const primera = useRef(true);
  useEffect(() => {
    if (primera.current) {
      primera.current = false;
      return;
    }
    const contenedor = lista.current;
    const elemento = document.getElementById(`zona-${seleccionadaId}`);
    if (!contenedor || !elemento) return;
    const cajaContenedor = contenedor.getBoundingClientRect();
    const cajaElemento = elemento.getBoundingClientRect();
    const arriba = cajaElemento.top - cajaContenedor.top + contenedor.scrollTop;
    const abajo = cajaElemento.bottom - cajaContenedor.top + contenedor.scrollTop;
    if (arriba < contenedor.scrollTop) contenedor.scrollTop = arriba;
    else if (abajo > contenedor.scrollTop + contenedor.clientHeight) {
      contenedor.scrollTop = abajo - contenedor.clientHeight;
    }
  }, [seleccionadaId]);

  return (
    <div ref={lista} className="donde-lista" role="region" aria-label="Lista de zonas" tabIndex={0}>
      {ranking.length > 0 ? (
        <ol className="donde-zonas" aria-label="Zonas recomendadas, de la mejor a la peor">
          {ranking.map((item, indice) => (
            <li key={item.zona.id}>
              <button
                type="button"
                id={`zona-${item.zona.id}`}
                className={`donde-zona ${item.coincidencia} ${item.zona.id === seleccionadaId ? 'activa' : ''}`.trim()}
                aria-pressed={item.zona.id === seleccionadaId}
                onClick={() => onElegir(item.zona.id)}
              >
                <span className="donde-zona-puesto" aria-hidden="true">
                  {indice + 1}
                </span>
                <span className="donde-zona-texto">
                  <strong>
                    {item.zona.nombre}
                    <Cambio cambio={cambios[item.zona.id]} />
                  </strong>
                  <span>
                    {SIGLA_DE_JURISDICCION[item.zona.jurisdiccion]} · {formatoKm(item.distanciaKm)}{' '}
                    de tu local
                  </span>
                </span>
                <span className="donde-nota">
                  {formatoPuntaje(item.puntaje)}
                  <span className="sr-only"> · {ETIQUETA_DE_COINCIDENCIA[item.coincidencia]}</span>
                </span>
              </button>
            </li>
          ))}
        </ol>
      ) : (
        <p className="donde-sin-zonas" role="status">
          Con esos límites no quedó ninguna zona. Probá aflojar alguno.
        </p>
      )}

      {descartadas.length > 0 && (
        <details className="donde-descartadas">
          <summary>
            Descartadas por tus límites <b>{descartadas.length}</b>
          </summary>
          <ul>
            {descartadas.map((item) => (
              <li key={item.zona.id}>
                <strong>{item.zona.nombre}</strong>
                <span>{item.motivos.join(' · ')}</span>
              </li>
            ))}
          </ul>
        </details>
      )}
    </div>
  );
}

function Cambio({ cambio }: { cambio: CambioDePuesto | undefined }) {
  if (cambio === undefined || cambio === 0) return null;
  if (cambio === 'nueva') {
    return (
      <em className="donde-cambio nueva" aria-label="Zona que antes estaba descartada">
        Vuelve
      </em>
    );
  }
  const sube = cambio > 0;
  const cantidad = Math.abs(cambio);
  return (
    <em
      className={`donde-cambio ${sube ? 'sube' : 'baja'}`}
      aria-label={`${sube ? 'Subió' : 'Bajó'} ${cantidad} ${cantidad === 1 ? 'puesto' : 'puestos'}`}
    >
      {sube ? '▲' : '▼'} {cantidad}
    </em>
  );
}
