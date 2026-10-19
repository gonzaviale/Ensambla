import { pesos } from '@/lib/sistema/alertas';

type Props = {
  minimo: number;
  promedio: number;
  maximo: number;
  /** Tu precio, si tenés el producto: se marca sobre el rango. */
  mio?: number;
};

// El rango de precios de la zona en una barra: mínimo, promedio y máximo, y el tuyo si lo tenés.
export function RangoDePrecios({ minimo, promedio, maximo, mio }: Props) {
  // La barra se extiende un poco para que tu precio entre aunque esté fuera del rango de la zona.
  const desde = Math.min(minimo, mio ?? minimo);
  const hasta = Math.max(maximo, mio ?? maximo);
  const margen = (hasta - desde) * 0.08 || 1;
  const inicio = desde - margen;
  const largo = hasta + margen - inicio;
  const lugar = (valor: number) =>
    `${Math.max(0, Math.min(100, ((valor - inicio) / largo) * 100))}%`;

  return (
    <div
      className="zona-rango"
      role="img"
      aria-label={`De ${pesos(minimo)} a ${pesos(maximo)}, promedio ${pesos(promedio)}${mio ? `, tu precio ${pesos(mio)}` : ''}`}
    >
      <span className="minimo" style={{ left: lugar(minimo) }}>
        {pesos(minimo)}
      </span>
      <i style={{ left: lugar(promedio) }} />
      <span style={{ left: lugar(promedio) }}>promedio</span>
      <span className="maximo" style={{ left: lugar(maximo) }}>
        {pesos(maximo)}
      </span>
      {mio ? <i className="mio" style={{ left: lugar(mio) }} /> : null}
    </div>
  );
}
