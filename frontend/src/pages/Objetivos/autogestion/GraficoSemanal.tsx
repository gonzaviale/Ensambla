import { useId } from 'react';
import { DECISION, diaCorto, semanaDel } from '@/lib/objetivosAutogestion';
import { formatoPesos } from '@/lib/objetivos';
import type { DecisionObjetivo, ProgresoObjetivo } from '@/types/objetivos';

type Props = {
  progreso: ProgresoObjetivo;
  decisiones: DecisionObjetivo[];
};

const ANCHO = 640;
const ALTO = 300;
const MARGEN = { arriba: 34, abajo: 34, izquierda: 64, derecha: 28 };
const LINEAS_DE_GUIA = 4;

/** «$120 mil», para que el eje se lea de un vistazo. */
function corto(valor: number) {
  if (Math.abs(valor) >= 1_000_000) return `$${(valor / 1_000_000).toFixed(1).replace('.', ',')} M`;
  if (Math.abs(valor) >= 1_000) return `$${Math.round(valor / 1_000)} mil`;
  return formatoPesos(valor);
}

// El resultado del objetivo semana a semana, la meta y, sobre cada semana, lo que decidió la persona.
// SVG propio (no hay librería de gráficos); los lectores de pantalla leen la misma información en una tabla.
export function GraficoSemanal({ progreso, decisiones }: Props) {
  // El id va dentro de url(#…): sin los signos que React pone en useId.
  const idDegradado = `grafico-${useId().replace(/[^a-zA-Z0-9-]/g, '')}`;
  const puntos = progreso.semanas;
  if (puntos.length === 0) {
    return (
      <p className="grafico-semanal-vacio">
        Todavía no hay semanas para dibujar. Cuando cierre el primer domingo, acá vas a ver cómo
        vas.
      </p>
    );
  }
  const techo = Math.max(progreso.meta, ...puntos.map((punto) => punto.valor)) * 1.12 || 1;
  const piso = Math.min(0, ...puntos.map((punto) => punto.valor));
  const anchoUtil = ANCHO - MARGEN.izquierda - MARGEN.derecha;
  const altoUtil = ALTO - MARGEN.arriba - MARGEN.abajo;
  const x = (indice: number) =>
    MARGEN.izquierda +
    (puntos.length === 1 ? anchoUtil / 2 : (indice / (puntos.length - 1)) * anchoUtil);
  const y = (valor: number) => MARGEN.arriba + (1 - (valor - piso) / (techo - piso)) * altoUtil;
  const base = y(Math.max(piso, 0));
  const linea = puntos.map((punto, indice) => `${x(indice)},${y(punto.valor)}`).join(' ');
  const area = `${x(0)},${base} ${linea} ${x(puntos.length - 1)},${base}`;
  const guias = Array.from(
    { length: LINEAS_DE_GUIA + 1 },
    (_, indice) => piso + ((techo - piso) * indice) / LINEAS_DE_GUIA,
  );
  const indiceDe = new Map(puntos.map((punto, indice) => [punto.semana, indice]));
  const porSemana = new Map<string, DecisionObjetivo[]>();
  decisiones
    .filter((decision) => indiceDe.has(decision.semanaInicio))
    .forEach((decision) =>
      porSemana.set(decision.semanaInicio, [
        ...(porSemana.get(decision.semanaInicio) ?? []),
        decision,
      ]),
    );
  const ultimo = puntos.length - 1;
  const yMeta = y(progreso.meta);

  return (
    <figure className="grafico-semanal">
      <svg viewBox={`0 0 ${ANCHO} ${ALTO}`} role="img" aria-labelledby={`${idDegradado}-titulo`}>
        <title id={`${idDegradado}-titulo`}>
          Tu resultado por semana frente a la meta de {formatoPesos(progreso.meta)}
        </title>
        <defs>
          <linearGradient id={idDegradado} x1="0" x2="0" y1="0" y2="1">
            <stop offset="0%" stopColor="var(--petrol)" stopOpacity="0.22" />
            <stop offset="100%" stopColor="var(--petrol)" stopOpacity="0" />
          </linearGradient>
        </defs>
        {guias.map((valor) => (
          <g key={valor}>
            <line
              className="grafico-semanal-guia"
              x1={MARGEN.izquierda}
              x2={ANCHO - MARGEN.derecha}
              y1={y(valor)}
              y2={y(valor)}
            />
            <text className="grafico-semanal-eje-y" x={MARGEN.izquierda - 10} y={y(valor) + 4}>
              {corto(valor)}
            </text>
          </g>
        ))}
        <polygon className="grafico-semanal-area" points={area} fill={`url(#${idDegradado})`} />
        <line
          className="grafico-semanal-meta"
          x1={MARGEN.izquierda}
          x2={ANCHO - MARGEN.derecha}
          y1={yMeta}
          y2={yMeta}
        />
        <g className="grafico-semanal-etiqueta-meta">
          <rect x={MARGEN.izquierda + 6} y={yMeta - 24} width={132} height={20} rx={10} />
          <text x={MARGEN.izquierda + 72} y={yMeta - 10}>
            Meta {corto(progreso.meta)}
          </text>
        </g>
        <polyline className="grafico-semanal-linea" points={linea} />
        {puntos.map((punto, indice) => (
          <g key={punto.semana}>
            <circle
              className={`grafico-semanal-punto ${punto.estimado ? 'estimado' : ''} ${indice === ultimo ? 'actual' : ''}`
                .replace(/\s+/g, ' ')
                .trim()}
              cx={x(indice)}
              cy={y(punto.valor)}
              r={indice === ultimo ? 7 : 5}
            />
            <text className="grafico-semanal-eje" x={x(indice)} y={ALTO - 10}>
              {diaCorto(punto.semana)}
            </text>
          </g>
        ))}
        <text
          className="grafico-semanal-valor-actual"
          x={x(ultimo)}
          y={y(puntos[ultimo]?.valor ?? 0) + 24}
        >
          {corto(puntos[ultimo]?.valor ?? 0)}
        </text>
        {[...porSemana.entries()].map(([semana, deLaSemana]) => {
          const indice = indiceDe.get(semana) ?? 0;
          const valor = puntos[indice]?.valor ?? 0;
          return deLaSemana.map((decision, orden) => (
            <g
              key={decision.id}
              className={`grafico-semanal-marca ${DECISION[decision.tipo].clase}`}
              transform={`translate(${x(indice)} ${y(valor) - 20 - orden * 22})`}
            >
              <circle r={9} />
              <text y={4}>{DECISION[decision.tipo].simbolo}</text>
            </g>
          ));
        })}
      </svg>
      <figcaption>
        <ul className="grafico-semanal-leyenda">
          {Object.values(DECISION).map((tipo) => (
            <li key={tipo.clase} className={tipo.clase}>
              <span aria-hidden>{tipo.simbolo}</span> {tipo.texto}
            </li>
          ))}
        </ul>
      </figcaption>
      <table className="sr-only">
        <caption>Resultado y decisiones por semana</caption>
        <thead>
          <tr>
            <th scope="col">Semana</th>
            <th scope="col">Resultado</th>
            <th scope="col">Decisiones</th>
          </tr>
        </thead>
        <tbody>
          {puntos.map((punto) => (
            <tr key={punto.semana}>
              <th scope="row">{semanaDel(punto.semana)}</th>
              <td>
                {formatoPesos(punto.valor)}
                {punto.estimado ? ' (calculado con tus secciones)' : ''}
              </td>
              <td>
                {(porSemana.get(punto.semana) ?? [])
                  .map((decision) => `${DECISION[decision.tipo].texto}: ${decision.descripcion}`)
                  .join('; ') || 'Ninguna'}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </figure>
  );
}
