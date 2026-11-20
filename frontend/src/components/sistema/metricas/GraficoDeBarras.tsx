import type { CSSProperties } from 'react';

export type Barra = {
  clave: string;
  /** Lo que va debajo de la barra; vacío para no amontonar etiquetas. */
  etiqueta: string;
  /** El día o el nombre completo, para el detalle. */
  nombre: string;
  valor: number;
  /** El valor ya escrito, por ejemplo «$ 6.800». */
  detalle: string;
  destacada?: boolean;
};

// Barras verticales de una sola serie (sin leyenda: el título dice qué son). Crecen desde abajo al aparecer y cada
// una muestra su valor al pasar el mouse o al llegar con el teclado. `oscuro` es para dibujarlas sobre un fondo de color.
export function GraficoDeBarras({
  barras,
  titulo,
  variante = 'claro',
}: {
  barras: Barra[];
  titulo: string;
  variante?: 'claro' | 'oscuro';
}) {
  const maximo = Math.max(0, ...barras.map((barra) => barra.valor));
  return (
    <figure className={`grafico-barras grafico-barras-${variante}`}>
      <figcaption className="sr-only">{titulo}</figcaption>
      <ul className="grafico-barras-area" aria-label={titulo}>
        {barras.map((barra, indice) => (
          <li
            key={barra.clave}
            className="grafico-barra"
            data-destacada={barra.destacada ? 'true' : undefined}
            style={
              {
                '--alto': `${maximo > 0 ? (barra.valor / maximo) * 100 : 0}%`,
                '--i': indice,
              } as CSSProperties
            }
            tabIndex={0}
            aria-label={`${barra.nombre}: ${barra.detalle}`}
          >
            <span className="grafico-barra-relleno" />
            <span className="grafico-barra-detalle" aria-hidden="true">
              {barra.nombre}
              <strong>{barra.detalle}</strong>
            </span>
            <span className="grafico-barra-etiqueta" aria-hidden="true">
              {barra.etiqueta}
            </span>
          </li>
        ))}
      </ul>
    </figure>
  );
}
