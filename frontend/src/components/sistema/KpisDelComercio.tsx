import type { CSSProperties } from 'react';
import { Icon } from './Icon';
import type { NombreDeIcono } from '@/lib/sistema/iconos';

type Kpi = {
  clave: string;
  etiqueta: string;
  valor: string;
  /** Cuánto cambió respecto del período anterior, con su texto y si es buena o mala noticia. */
  cambio: { texto: string; sube: boolean; bueno: boolean };
  ayuda: string;
  icono: NombreDeIcono;
  color: 'orange' | 'blue' | 'coral' | 'petrol' | 'yellow';
  serie: number[];
};

// Cifras de ejemplo de un comercio de barrio: sólo para ver cómo quedarían, no salen de los datos del cuaderno.
const KPIS: Kpi[] = [
  {
    clave: 'ventas',
    etiqueta: 'Ventas de hoy',
    valor: '$ 184.500',
    cambio: { texto: '12 % más que ayer', sube: true, bueno: true },
    ayuda: '41 ventas',
    icono: 'wallet',
    color: 'orange',
    serie: [120, 138, 131, 152, 149, 165, 184],
  },
  {
    clave: 'ticket',
    etiqueta: 'Ticket promedio',
    valor: '$ 4.500',
    cambio: { texto: '3 % más que la semana pasada', sube: true, bueno: true },
    ayuda: 'por cada venta',
    icono: 'orders',
    color: 'blue',
    serie: [4.1, 4.3, 4.2, 4.4, 4.3, 4.4, 4.5],
  },
  {
    clave: 'margen',
    etiqueta: 'Ganancia del mes',
    valor: '31 %',
    cambio: { texto: '1,2 puntos menos que el mes pasado', sube: false, bueno: false },
    ayuda: 'sobre lo que vendés',
    icono: 'chart',
    color: 'coral',
    serie: [33, 32.6, 32.4, 32, 31.8, 31.4, 31],
  },
  {
    clave: 'reponer',
    etiqueta: 'Para reponer',
    valor: '9 productos',
    cambio: { texto: '4 más que ayer', sube: true, bueno: false },
    ayuda: 'con poco stock',
    icono: 'box',
    color: 'petrol',
    serie: [3, 4, 4, 5, 6, 5, 9],
  },
  {
    clave: 'fiado',
    etiqueta: 'Fiado por cobrar',
    valor: '$ 128.400',
    cambio: { texto: '$ 9.000 menos que ayer', sube: false, bueno: true },
    ayuda: '7 clientes',
    icono: 'users',
    color: 'yellow',
    serie: [150, 146, 142, 140, 138, 137, 128],
  },
];

const ANCHO = 120;
const ALTO = 34;

function Tendencia({ serie }: { serie: number[] }) {
  const minimo = Math.min(...serie);
  const rango = Math.max(...serie) - minimo || 1;
  const puntos = serie.map((valor, indice) => {
    const x = (indice / (serie.length - 1)) * (ANCHO - 6) + 3;
    const y = ALTO - 4 - ((valor - minimo) / rango) * (ALTO - 8);
    return [x, y] as const;
  });
  const linea = puntos.map(([x, y]) => `${x.toFixed(1)},${y.toFixed(1)}`).join(' ');
  const [ultimoX, ultimoY] = puntos[puntos.length - 1] ?? [0, 0];
  return (
    <svg
      className="kpi-tendencia"
      viewBox={`0 0 ${ANCHO} ${ALTO}`}
      preserveAspectRatio="none"
      aria-hidden="true"
    >
      <polyline points={linea} fill="none" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
      <circle cx={ultimoX} cy={ultimoY} r="3.5" />
    </svg>
  );
}

// Las cifras que mira todos los días el dueño de un comercio, en tarjetas con la paleta de la página.
export function KpisDelComercio() {
  return (
    <section className="kpis" aria-labelledby="kpis-titulo">
      <div className="kpis-cabeza">
        <h2 id="kpis-titulo">Cómo viene tu negocio</h2>
        <span className="kpis-ejemplo">Datos de ejemplo</span>
      </div>
      <ul className="kpis-lista">
        {KPIS.map((kpi) => (
          <li
            key={kpi.clave}
            className="kpi"
            data-color={kpi.color}
            style={{ '--kpi': `var(--${kpi.color})` } as CSSProperties}
          >
            <div className="kpi-fila">
              <span className="kpi-icono">
                <Icon name={kpi.icono} />
              </span>
              <span className="kpi-etiqueta">{kpi.etiqueta}</span>
            </div>
            <strong className="kpi-valor">{kpi.valor}</strong>
            <span className={`kpi-cambio ${kpi.cambio.bueno ? 'bueno' : 'malo'}`}>
              <Icon name={kpi.cambio.sube ? 'up' : 'down'} />
              {kpi.cambio.texto}
            </span>
            <span className="kpi-ayuda">{kpi.ayuda}</span>
            <Tendencia serie={kpi.serie} />
          </li>
        ))}
      </ul>
    </section>
  );
}
