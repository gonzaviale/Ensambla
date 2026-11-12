import { ArrowsClockwiseIcon } from '@phosphor-icons/react';
import { DECISION, diaCorto, domingoDe, efectoDe, semanaDel } from '@/lib/objetivosAutogestion';
import { formatoPesos } from '@/lib/objetivos';
import type { DecisionObjetivo, SemanasObjetivo } from '@/types/objetivos';

type Props = {
  decisiones: DecisionObjetivo[];
  semanas: SemanasObjetivo['semanas'];
  estaSemana: string;
};

// Lo que pasó, semana por semana y de la más nueva a la más vieja, unido como un recorrido: qué decidió la persona,
// cómo cerró el domingo y qué cambió Sami.
export function RecorridoDeLaSemana({ decisiones, semanas, estaSemana }: Props) {
  const cierreDe = new Map(semanas.map((semana) => [semana.semana, semana]));
  const lunes = [
    ...new Set([...decisiones.map((decision) => decision.semanaInicio), ...cierreDe.keys()]),
  ]
    .sort()
    .reverse();

  if (lunes.length === 0) {
    return (
      <p className="recorrido-vacio">
        Todavía no pasó nada. Cuando decidas algo o cierre el primer domingo, lo vas a ver acá.
      </p>
    );
  }

  return (
    <ol className="recorrido" aria-label="Lo que pasó cada semana">
      {lunes.map((semana) => {
        const cierre = cierreDe.get(semana);
        const deLaSemana = decisiones.filter((decision) => decision.semanaInicio === semana);
        return (
          <li key={semana} className="recorrido-semana">
            <span className="recorrido-nodo" aria-hidden />
            <p className="recorrido-titulo">
              <strong>{semana === estaSemana ? 'Esta semana' : semanaDel(semana)}</strong>
              {cierre && <span>Cerró en {formatoPesos(cierre.resultado)}</span>}
            </p>
            <ul>
              {deLaSemana.map((decision) => {
                const efecto = efectoDe(decision);
                return (
                  <li
                    key={decision.id}
                    className={`recorrido-hecho ${DECISION[decision.tipo].clase}`}
                  >
                    <span className="recorrido-icono" aria-hidden>
                      {DECISION[decision.tipo].simbolo}
                    </span>
                    <div>
                      <strong>
                        {DECISION[decision.tipo].texto}: {decision.descripcion}
                      </strong>
                      <p>
                        {diaCorto(decision.tomadaEn)}
                        {efecto ? ` · ${efecto}` : ''}
                      </p>
                    </div>
                  </li>
                );
              })}
              {cierre?.reajustes.map((reajuste, indice) => (
                <li key={`reajuste-${indice}`} className="recorrido-hecho sami">
                  <span className="recorrido-icono" aria-hidden>
                    <ArrowsClockwiseIcon size={14} weight="bold" />
                  </span>
                  <div>
                    <strong>
                      Sami cambió «{reajuste.de.titulo}»
                      {reajuste.a ? ` por «${reajuste.a.titulo}»` : ''}
                    </strong>
                    <p>Domingo {diaCorto(domingoDe(semana))}</p>
                  </div>
                </li>
              ))}
              {deLaSemana.length === 0 && !cierre?.reajustes.length && (
                <li className="recorrido-hecho nada">
                  <span className="recorrido-icono" aria-hidden />
                  <p>Sin decisiones ni cambios.</p>
                </li>
              )}
            </ul>
          </li>
        );
      })}
    </ol>
  );
}
