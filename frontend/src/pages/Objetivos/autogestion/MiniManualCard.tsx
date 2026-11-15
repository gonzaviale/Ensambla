import { ESTADO_DE_MINI } from '@/lib/objetivosAutogestion';
import type { MiniManual } from '@/types/objetivos';

// Un mini-objetivo manual: no tiene números, es una acción que la persona hace y después le cuenta a Sami.
export function MiniManualCard({ mini }: { mini: MiniManual }) {
  const idTitulo = `mini-${mini.id}`;
  return (
    <article className="mini-objetivo mini-manual" aria-labelledby={idTitulo}>
      <header>
        <span className="mini-objetivo-numero" aria-hidden>
          {mini.posicion}
        </span>
        <div>
          <p className="objetivos-etiqueta">{ESTADO_DE_MINI[mini.estado]}</p>
          <h3 id={idTitulo}>{mini.titulo}</h3>
        </div>
      </header>
      <p className="mini-objetivo-porque">{mini.justificacion}</p>
    </article>
  );
}
