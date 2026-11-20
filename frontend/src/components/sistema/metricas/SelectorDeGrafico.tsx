import type { ReactNode } from 'react';
import { ETIQUETA_DEL_GRAFICO } from '@/lib/metricas/graficos';
import type { GraficoDeMetrica } from '@/types/metricas';

// Íconos de línea propios, del mismo trazo que los del resto de la app.
const ICONOS: Record<GraficoDeMetrica, ReactNode> = {
  NUMERO: <path d="M5 9h14M5 15h14M10 4 8 20M16 4l-2 16" />,
  BARRAS: <path d="M5 20V11M10 20V5M15 20v-7M20 20V8M3 20h19" />,
  DONA: (
    <>
      <circle cx="12" cy="12" r="8" />
      <circle cx="12" cy="12" r="3.5" />
      <path d="M12 4v4.5M19.5 15l-4-1.5" />
    </>
  ),
  LINEA: <path d="M3 17l5-6 4 3 4-7 5 4M3 21h18" />,
};

type Props = {
  grafico: GraficoDeMetrica;
  posibles: GraficoDeMetrica[];
  onElegir: (grafico: GraficoDeMetrica) => void;
};

/**
 * Cómo ver la métrica: sólo los gráficos que tienen sentido para su reparto; si hay uno solo, no se muestra. El número
 * no es una opción: siempre se ve junto al gráfico.
 */
export function SelectorDeGrafico({ grafico, posibles, onElegir }: Props) {
  const opciones = posibles.filter((opcion) => opcion !== 'NUMERO');
  if (opciones.length < 2) return null;
  return (
    <div className="selector-grafico" role="radiogroup" aria-label="Cómo verla">
      {opciones.map((opcion) => (
        <button
          key={opcion}
          type="button"
          role="radio"
          aria-checked={opcion === grafico}
          title={ETIQUETA_DEL_GRAFICO[opcion]}
          onClick={() => onElegir(opcion)}
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            {ICONOS[opcion]}
          </svg>
          <span className="sr-only">{ETIQUETA_DEL_GRAFICO[opcion]}</span>
        </button>
      ))}
    </div>
  );
}
