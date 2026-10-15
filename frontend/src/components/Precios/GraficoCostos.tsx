import { useId } from 'react';
import type { CostoHistorial } from '@/types/red';
import { costo as formatoCosto, fechaCorta } from '@/lib/red/formato';

type GraficoCostosProps = { historial: CostoHistorial[]; producto: string };

const ANCHO = 320;
const ALTO = 120;
const MARGEN = { x: 14, arriba: 14, abajo: 14 };

// Mini gráfico de línea del costo en el tiempo. Es un complemento: los mismos valores están en la
// tabla de abajo, y el gráfico lleva título y descripción para lectores de pantalla.
export function GraficoCostos({ historial, producto }: GraficoCostosProps) {
  const id = useId();
  const puntos = [...historial].sort((a, b) => a.fecha.localeCompare(b.fecha));
  const primero = puntos[0];
  const ultimo = puntos[puntos.length - 1];
  if (!primero || !ultimo) return null;

  const t0 = new Date(primero.fecha).getTime();
  const t1 = new Date(ultimo.fecha).getTime();
  const valores = puntos.map((p) => p.costo);
  const minimo = Math.min(...valores);
  const maximo = Math.max(...valores);

  const x = (fecha: string) =>
    t1 === t0
      ? ANCHO / 2
      : MARGEN.x + ((new Date(fecha).getTime() - t0) / (t1 - t0)) * (ANCHO - 2 * MARGEN.x);
  const y = (valor: number) =>
    maximo === minimo
      ? ALTO / 2
      : ALTO -
        MARGEN.abajo -
        ((valor - minimo) / (maximo - minimo)) * (ALTO - MARGEN.arriba - MARGEN.abajo);

  const trazo = puntos.map((p) => `${x(p.fecha).toFixed(1)},${y(p.costo).toFixed(1)}`).join(' ');
  const resumen =
    puntos.length === 1
      ? `Un solo costo registrado: ${formatoCosto(ultimo.costo)} desde el ${fechaCorta(ultimo.fecha)}.`
      : `El costo pasó de ${formatoCosto(primero.costo)} el ${fechaCorta(primero.fecha)} a ${formatoCosto(ultimo.costo)} el ${fechaCorta(ultimo.fecha)}.`;

  return (
    <svg
      role="img"
      aria-labelledby={`${id}-titulo ${id}-desc`}
      viewBox={`0 0 ${ANCHO} ${ALTO}`}
      className="h-auto w-full max-w-full rounded-md bg-sand-100/60 text-accent-800"
    >
      <title id={`${id}-titulo`}>{`Cómo cambió el costo de ${producto}`}</title>
      <desc id={`${id}-desc`}>{resumen}</desc>
      <line
        x1={MARGEN.x}
        x2={ANCHO - MARGEN.x}
        y1={ALTO - MARGEN.abajo}
        y2={ALTO - MARGEN.abajo}
        stroke="currentColor"
        strokeOpacity={0.25}
      />
      {puntos.length > 1 && (
        <polyline
          points={trazo}
          fill="none"
          stroke="currentColor"
          strokeWidth={3}
          strokeLinejoin="round"
          strokeLinecap="round"
        />
      )}
      {puntos.map((punto, indice) => (
        <circle
          key={`${punto.fecha}-${indice}`}
          cx={x(punto.fecha)}
          cy={y(punto.costo)}
          r={indice === puntos.length - 1 ? 6 : 4}
          fill={indice === puntos.length - 1 ? 'currentColor' : 'white'}
          stroke="currentColor"
          strokeWidth={2.5}
        />
      ))}
    </svg>
  );
}
