import type { MiniObjetivo } from '@/types/objetivos';
import { Progreso } from './piezas';

const COMO_SE_SIGUE: Record<MiniObjetivo['modo'], string> = {
  MEDIDO: 'Lo mide Sami',
  MANUAL: 'Me lo contás vos',
};

function AvanceDelMini({ mini }: { mini: MiniObjetivo }) {
  if (mini.modo !== 'MEDIDO') return null;
  return <Progreso valor={mini.avance} etiqueta={`Avance de «${mini.titulo}»`} />;
}

function MiniDeLaTarjeta({ mini }: { mini: MiniObjetivo }) {
  return (
    <li className="tarjeta-mini" data-modo={mini.modo}>
      <span className="tarjeta-mini-numero" aria-hidden>
        {mini.posicion}
      </span>
      <div className="tarjeta-mini-texto">
        <p>{mini.titulo}</p>
        <AvanceDelMini mini={mini} />
      </div>
      <span className="tarjeta-mini-modo">{COMO_SE_SIGUE[mini.modo]}</span>
    </li>
  );
}

// Los mini-objetivos de la semana de un objetivo sin meta en pesos: qué hay que hacer y quién lo sigue.
export function MinisDeLaTarjeta({ minis }: { minis: MiniObjetivo[] }) {
  if (minis.length === 0) return null;
  return (
    <ul className="tarjeta-minis" aria-label="Mini-objetivos de esta semana">
      {minis.map((mini) => (
        <MiniDeLaTarjeta key={mini.id} mini={mini} />
      ))}
    </ul>
  );
}
