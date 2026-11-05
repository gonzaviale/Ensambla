import { useState, type PointerEvent } from 'react';
import { pesos } from '@/lib/gestion/formato';
import type { Proyeccion } from '@/lib/gestion/proyeccion';

// La ganancia acumulada de los próximos 90 días, sin cambiar nada y con la decisión. Dos series con colores
// validados (contraste, daltonismo), la de hoy punteada para no depender del color, etiquetas al final de cada
// línea y una línea guía con el detalle al pasar el mouse o el dedo.

const ANCHO = 640;
const ALTO = 240;
const MARGEN = { arriba: 16, derecha: 112, abajo: 28, izquierda: 72 };

/** Marcas redondas del eje (1, 2 o 5 por una potencia de 10) que cubren el rango e incluyen el cero. */
function marcasDelEje(minimo: number, maximo: number) {
  const crudo = (maximo - minimo) / 3 || 1;
  const potencia = 10 ** Math.floor(Math.log10(crudo));
  const paso = ([1, 2, 5, 10].find((factor) => factor * potencia >= crudo) ?? 10) * potencia;
  const desde = Math.floor(minimo / paso) * paso;
  const hasta = Math.ceil(maximo / paso) * paso;
  const marcas: number[] = [];
  for (let valor = desde; valor <= hasta + paso / 2; valor += paso) marcas.push(Math.round(valor));
  return marcas;
}

export function GraficoDeProyeccion({ serie }: { serie: Proyeccion['serie'] }) {
  const [marcado, setMarcado] = useState<number | null>(null);
  const valores = serie.flatMap((punto) => [punto.antes, punto.despues]);
  const marcas = marcasDelEje(Math.min(0, ...valores), Math.max(1, ...valores));
  const minimo = marcas[0] ?? 0;
  const maximo = marcas[marcas.length - 1] ?? 1;
  const ultimoDia = serie[serie.length - 1]?.dia ?? 1;
  const ancho = ANCHO - MARGEN.izquierda - MARGEN.derecha;
  const alto = ALTO - MARGEN.arriba - MARGEN.abajo;
  const x = (dia: number) => MARGEN.izquierda + (dia / ultimoDia) * ancho;
  const y = (valor: number) => MARGEN.arriba + ((maximo - valor) / (maximo - minimo)) * alto;
  const linea = (clave: 'antes' | 'despues') =>
    serie
      .map((punto, indice) => `${indice ? 'L' : 'M'}${x(punto.dia)},${y(punto[clave])}`)
      .join(' ');
  const ultimo = serie[serie.length - 1];
  const punto = marcado === null ? null : serie[marcado];

  const alMover = (evento: PointerEvent<SVGRectElement>) => {
    const caja = evento.currentTarget.getBoundingClientRect();
    const relativo = (evento.clientX - caja.left) / caja.width;
    setMarcado(Math.max(0, Math.min(serie.length - 1, Math.round(relativo * (serie.length - 1)))));
  };

  // Las etiquetas del final se separan si quedan encimadas.
  const etiquetaAntes = ultimo ? y(ultimo.antes) : 0;
  let etiquetaDespues = ultimo ? y(ultimo.despues) : 0;
  if (Math.abs(etiquetaDespues - etiquetaAntes) < 16)
    etiquetaDespues = etiquetaAntes + (etiquetaDespues >= etiquetaAntes ? 16 : -16);

  return (
    <figure className="gn-grafico">
      <figcaption>
        <span>Ganancia de verdad acumulada · próximos 90 días</span>
        <span className="gn-leyenda">
          <span data-serie="antes">Sin cambiar nada</span>
          <span data-serie="despues">Con la decisión</span>
        </span>
      </figcaption>
      <div className="gn-grafico-lienzo">
        <svg
          viewBox={`0 0 ${ANCHO} ${ALTO}`}
          role="img"
          aria-label="Proyección de la ganancia acumulada a 90 días"
        >
          {marcas.map((valor) => (
            <g key={valor} className={valor === 0 ? 'gn-eje-cero' : 'gn-eje'}>
              <line
                x1={MARGEN.izquierda}
                x2={MARGEN.izquierda + ancho}
                y1={y(valor)}
                y2={y(valor)}
              />
              <text x={MARGEN.izquierda - 8} y={y(valor)} dy="0.32em" textAnchor="end">
                {pesos(valor)}
              </text>
            </g>
          ))}
          {[0, 30, 60, 90].map((dia) => (
            <text
              key={dia}
              className="gn-eje-x"
              x={x(Math.min(dia, ultimoDia))}
              y={ALTO - 8}
              textAnchor="middle"
            >
              {dia === 0 ? 'Hoy' : `Día ${dia}`}
            </text>
          ))}
          <path className="gn-serie" data-serie="antes" d={linea('antes')} />
          <path className="gn-serie" data-serie="despues" d={linea('despues')} />
          {ultimo && (
            <>
              <text className="gn-etiqueta" x={x(ultimo.dia) + 8} y={etiquetaAntes} dy="0.32em">
                {pesos(ultimo.antes)}
              </text>
              <text
                className="gn-etiqueta fuerte"
                x={x(ultimo.dia) + 8}
                y={etiquetaDespues}
                dy="0.32em"
              >
                {pesos(ultimo.despues)}
              </text>
            </>
          )}
          {punto && (
            <g className="gn-guia">
              <line
                x1={x(punto.dia)}
                x2={x(punto.dia)}
                y1={MARGEN.arriba}
                y2={MARGEN.arriba + alto}
              />
              <circle data-serie="antes" cx={x(punto.dia)} cy={y(punto.antes)} r={5} />
              <circle data-serie="despues" cx={x(punto.dia)} cy={y(punto.despues)} r={5} />
            </g>
          )}
          <rect
            className="gn-grafico-zona"
            x={MARGEN.izquierda}
            y={0}
            width={ancho}
            height={ALTO}
            onPointerMove={alMover}
            onPointerDown={alMover}
            onPointerLeave={() => setMarcado(null)}
          />
        </svg>
        {punto && (
          <div
            className="gn-tooltip"
            style={{ left: `${(x(punto.dia) / ANCHO) * 100}%` }}
            data-lado={x(punto.dia) > ANCHO / 2 ? 'izquierda' : 'derecha'}
          >
            <strong>{punto.dia === 0 ? 'Hoy' : `En ${punto.dia} días`}</strong>
            <span data-serie="antes">Sin cambiar nada: {pesos(punto.antes)}</span>
            <span data-serie="despues">Con la decisión: {pesos(punto.despues)}</span>
          </div>
        )}
      </div>
      <table className="sr-only">
        <caption>Ganancia acumulada por semana</caption>
        <thead>
          <tr>
            <th scope="col">Día</th>
            <th scope="col">Sin cambiar nada</th>
            <th scope="col">Con la decisión</th>
          </tr>
        </thead>
        <tbody>
          {serie.map((fila) => (
            <tr key={fila.dia}>
              <th scope="row">{fila.dia}</th>
              <td>{pesos(fila.antes)}</td>
              <td>{pesos(fila.despues)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </figure>
  );
}
