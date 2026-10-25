import { pesos } from '@/lib/sistema/alertas';
import { EN_LINEA } from '@/lib/sistema/zona';

type Props = {
  minimo: number;
  promedio: number;
  maximo: number;
  /** Tu precio, si tenés el producto: se marca sobre el rango. */
  mio?: number;
};

// El rango de precios de la zona en una barra: mínimo, promedio y máximo, y el tuyo si lo tenés.
// La barra se pinta con los mismos tramos que la etiqueta de tu precio: más barato, en línea y más caro.
export function RangoDePrecios({ minimo, promedio, maximo, mio }: Props) {
  const enLineaDesde = promedio * EN_LINEA.desde;
  const enLineaHasta = promedio * EN_LINEA.hasta;
  // La barra se extiende un poco para que tu precio entre aunque esté fuera del rango de la zona.
  const desde = Math.min(minimo, mio ?? minimo);
  const hasta = Math.max(maximo, mio ?? maximo);
  const margen = (hasta - desde) * 0.08 || 1;
  const inicio = desde - margen;
  const largo = hasta + margen - inicio;
  const porcentaje = (valor: number) =>
    Math.max(0, Math.min(100, ((valor - inicio) / largo) * 100));
  const lugar = (valor: number) => `${porcentaje(valor)}%`;
  const corteBajo = porcentaje(enLineaDesde);
  const corteAlto = porcentaje(enLineaHasta);

  return (
    <div
      className="zona-rango"
      role="img"
      aria-label={`De ${pesos(minimo)} a ${pesos(maximo)}, promedio ${pesos(promedio)}. En línea entre ${pesos(enLineaDesde)} y ${pesos(enLineaHasta)}${mio ? `, tu precio ${pesos(mio)}` : ''}`}
      style={{
        background: `linear-gradient(to right, var(--yellow) 0 ${corteBajo}%, var(--blue) ${corteBajo}% ${corteAlto}%, var(--coral) ${corteAlto}% 100%)`,
      }}
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
