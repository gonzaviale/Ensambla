import type { CSSProperties } from 'react';
import { formatoDelValor } from '@/lib/metricas/metricasAMedida';
import type { Parte, PropsDelGrafico } from '@/lib/metricas/graficos';

// Margen en porcentaje para que el primer y el último punto no queden pegados al borde.
const MARGEN = 4;

type Punto = Parte & { x: number; y: number };

// La línea une los días de la semana en su orden. Se estira con el ancho del panel sin deformar los puntos ni el texto:
// la línea es SVG y los puntos y los días van encima, en HTML. Cada punto dice su valor al pasar el mouse o con el lector.
export function LineaDeLaMetrica({ definicion, resultado, partes }: PropsDelGrafico) {
  const puntos = puntosDe(partes);
  return (
    <figure className="grafico-linea" aria-label={definicion.nombre}>
      <div className="grafico-linea-area">
        <svg viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
          <polyline
            points={puntos.map((punto) => `${punto.x},${punto.y}`).join(' ')}
            vectorEffect="non-scaling-stroke"
          />
        </svg>
        {puntos.map((punto) => (
          <span
            key={punto.etiqueta}
            className="grafico-linea-punto"
            style={posicionDe(punto)}
            tabIndex={0}
            aria-label={`${punto.etiqueta}: ${formatoDelValor(punto.valor, resultado.formato)}`}
            title={formatoDelValor(punto.valor, resultado.formato)}
          />
        ))}
      </div>
      <div className="grafico-linea-dias" aria-hidden="true">
        {puntos.map((punto) => (
          <span key={punto.etiqueta} style={{ left: `${punto.x}%` }}>
            {punto.etiqueta.slice(0, 3)}
          </span>
        ))}
      </div>
    </figure>
  );
}

function puntosDe(partes: Parte[]): Punto[] {
  const maximo = Math.max(0, ...partes.map((parte) => parte.valor));
  const paso = partes.length > 1 ? (100 - 2 * MARGEN) / (partes.length - 1) : 0;
  return partes.map((parte, lugar) => ({
    ...parte,
    x: MARGEN + lugar * paso,
    y: maximo > 0 ? 100 - MARGEN - (parte.valor / maximo) * (100 - 2 * MARGEN) : 100 - MARGEN,
  }));
}

const posicionDe = (punto: Punto): CSSProperties => ({ left: `${punto.x}%`, top: `${punto.y}%` });
