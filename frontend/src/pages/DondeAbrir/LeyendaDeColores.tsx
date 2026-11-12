import { COLOR_DE_PUNTO } from '@/hooks/dondeAbrir/useMapaDeZonas';
import {
  CAPAS,
  ETIQUETA_DE_COINCIDENCIA,
  type Coincidencia,
  type TipoDePunto,
} from '@/lib/dondeAbrir';

const ORDEN: Coincidencia[] = ['buena', 'intermedia', 'mala'];

// Qué quiere decir cada cosa del mapa: los anillos son zonas (el color dice qué tan bien coinciden con lo que se busca)
// y los puntitos son lugares reales alrededor. Los dos usan rojo y verde con significados distintos, por eso se
// explican por separado y los puntos sólo muestran lo que está prendido en «Qué mostrar en el mapa».
export function LeyendaDeColores({
  capas,
  hayLugarElegido = false,
}: {
  capas: Record<TipoDePunto, boolean>;
  hayLugarElegido?: boolean;
}) {
  const visibles = CAPAS.filter(({ tipo }) => capas[tipo]);
  return (
    <div className="donde-leyendas">
      <ul className="donde-leyenda" aria-label="Qué significa cada color">
        <li className="titulo">Zonas (anillos)</li>
        {ORDEN.map((coincidencia) => (
          <li key={coincidencia} className={`zona ${coincidencia}`}>
            <i aria-hidden />
            {ETIQUETA_DE_COINCIDENCIA[coincidencia]}
          </li>
        ))}
        {hayLugarElegido && (
          <li className="zona elegido">
            <i aria-hidden />
            Lugar que mediste
          </li>
        )}
      </ul>
      {visibles.length > 0 && (
        <ul className="donde-leyenda" aria-label="Qué significa cada punto del mapa">
          <li className="titulo">Lugares a 500 m (puntos)</li>
          {visibles.map(({ tipo, etiqueta }) => (
            <li key={tipo}>
              <i aria-hidden style={{ background: COLOR_DE_PUNTO[tipo] }} />
              {etiqueta}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
