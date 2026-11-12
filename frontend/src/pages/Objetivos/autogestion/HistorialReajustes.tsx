import { ArrowRightIcon, ArrowsClockwiseIcon, TrendUpIcon } from '@phosphor-icons/react';
import { Sami } from '@/components/sistema/Sami';
import { diaCorto, domingoDe } from '@/lib/objetivosAutogestion';
import type { SemanasObjetivo } from '@/types/objetivos';

const TIPO = {
  SUBIR_META: 'Subimos la meta',
  REEMPLAZAR: 'Lo cambiamos',
  PROBAR_OTRO_CAMINO: 'Probamos otro camino',
} as const;

// «El domingo cambiamos X por Y, porque…»: los cambios que hizo Sami cada domingo, del más nuevo al más viejo.
export function HistorialReajustes({ semanas }: { semanas: SemanasObjetivo['semanas'] }) {
  const conCambios = semanas.filter((semana) => semana.reajustes.length > 0);
  if (conCambios.length === 0) {
    return (
      <p className="reajustes-vacio">
        Todavía no cambiamos ningún mini-objetivo. Cada domingo Sami revisa cómo van y, si hace
        falta, te propone otro.
      </p>
    );
  }
  return (
    <ol className="reajustes">
      {conCambios.flatMap((semana) =>
        semana.reajustes.map((reajuste, indice) => (
          <li key={`${semana.semana}-${indice}`} className={reajuste.tipo.toLowerCase()}>
            <header>
              <span className="reajustes-tipo">
                {reajuste.tipo === 'SUBIR_META' ? (
                  <TrendUpIcon aria-hidden size={16} weight="bold" />
                ) : (
                  <ArrowsClockwiseIcon aria-hidden size={16} weight="bold" />
                )}
                {TIPO[reajuste.tipo]}
              </span>
              <span className="reajustes-fecha">Domingo {diaCorto(domingoDe(semana.semana))}</span>
            </header>
            <div className="reajustes-cambio">
              <div className="antes">
                <span>Antes</span>
                <s>{reajuste.de.titulo}</s>
              </div>
              {reajuste.a && (
                <>
                  <ArrowRightIcon aria-label="por" size={22} weight="bold" />
                  <div className="ahora">
                    <span>Ahora</span>
                    <strong>{reajuste.a.titulo}</strong>
                  </div>
                </>
              )}
            </div>
            <div className="reajustes-motivo">
              <div className="reajustes-sami" aria-hidden>
                <Sami pose="think" />
              </div>
              <p>{reajuste.motivo}</p>
            </div>
          </li>
        )),
      )}
    </ol>
  );
}
