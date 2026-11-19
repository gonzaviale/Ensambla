import { formatoDelValor } from '@/lib/metricas/metricasAMedida';
import type { Parte, PropsDelGrafico } from '@/lib/metricas/graficos';

// Los colores de la paleta para las porciones, en orden; «Otros» va en celeste claro para no competir con las grandes.
const COLORES = [
  '#00506a',
  '#ee8000',
  '#f85f5a',
  '#b52d66',
  '#4eb2d4',
  '#ebce32',
  '#003e53',
  '#a95700',
];
const COLOR_DE_OTROS = '#a8dcec';

const RADIO = 42;
const LARGO = 2 * Math.PI * RADIO;

type Porcion = Parte & { color: string; largo: number; desde: number; porcentaje: number };

// La torta como dona: cada parte es una porción con su color y, al lado, la lista con el valor y el porcentaje.
export function DonaDeLaMetrica({ definicion, resultado, partes }: PropsDelGrafico) {
  const porciones = porcionesDe(partes);
  return (
    <figure className="grafico-dona">
      <svg viewBox="0 0 100 100" role="img" aria-label={definicion.nombre}>
        <circle className="grafico-dona-fondo" cx="50" cy="50" r={RADIO} />
        {porciones.map((porcion) => (
          <circle
            key={porcion.etiqueta}
            cx="50"
            cy="50"
            r={RADIO}
            stroke={porcion.color}
            strokeDasharray={`${porcion.largo} ${LARGO - porcion.largo}`}
            strokeDashoffset={-porcion.desde}
          />
        ))}
      </svg>
      <ul className="grafico-dona-lista">
        {porciones.map((porcion) => (
          <li key={porcion.etiqueta}>
            <span
              className="grafico-dona-color"
              style={{ background: porcion.color }}
              aria-hidden="true"
            />
            <span className="grafico-dona-etiqueta">{porcion.etiqueta}</span>
            <strong>{formatoDelValor(porcion.valor, resultado.formato)}</strong>
            <span className="grafico-dona-porcentaje">{porcion.porcentaje}%</span>
          </li>
        ))}
      </ul>
    </figure>
  );
}

function porcionesDe(partes: Parte[]): Porcion[] {
  const total = partes.reduce((suma, parte) => suma + parte.valor, 0);
  let desde = 0;
  return partes.map((parte, lugar) => {
    const largo = total > 0 ? (parte.valor / total) * LARGO : 0;
    const porcion = {
      ...parte,
      color: colorDe(parte, lugar),
      largo,
      desde,
      porcentaje: porcentajeDe(parte, total),
    };
    desde += largo;
    return porcion;
  });
}

const colorDe = (parte: Parte, lugar: number) =>
  parte.otros ? COLOR_DE_OTROS : (COLORES[lugar % COLORES.length] ?? COLORES[0] ?? '#00506a');

const porcentajeDe = (parte: Parte, total: number) =>
  total > 0 ? Math.round((parte.valor / total) * 100) : 0;
