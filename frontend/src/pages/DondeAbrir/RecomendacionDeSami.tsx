import { Sami } from '@/components/sistema/Sami';
import type { Recorrido } from '@/hooks/dondeAbrir/useRecorridoDondeAbrir';
import { NOMBRE_DE_RUBRO, type ZonaPuntuada } from '@/lib/dondeAbrir';

const claseDelBoton = (item: ZonaPuntuada, activa: boolean) =>
  [item.coincidencia, activa && 'activa'].filter(Boolean).join(' ');

function BotonDeZona({ item, recorrido }: { item: ZonaPuntuada; recorrido: Recorrido }) {
  const activa = item.zona.id === recorrido.elegida?.zona.id;
  return (
    <li>
      <button
        type="button"
        aria-pressed={activa}
        className={claseDelBoton(item, activa)}
        onClick={() => recorrido.elegirZona(item.zona.id)}
      >
        <i aria-hidden />
        {item.zona.nombre}
      </button>
    </li>
  );
}

const titulo = (esLaMejor: boolean, recorrido: Recorrido) =>
  esLaMejor
    ? `Mi recomendación para tu ${NOMBRE_DE_RUBRO[recorrido.filtros.rubro]}`
    : 'Lo que sé de la zona que elegiste';

/** Sami recomienda la mejor zona o, si la persona eligió otra, cuenta lo que sabe de esa. */
export function RecomendacionDeSami({ recorrido }: { recorrido: Recorrido }) {
  const { elegida, ranking, mejores } = recorrido;
  if (!elegida) return null;
  const esLaMejor = elegida.zona.id === ranking[0]?.zona.id;
  return (
    <section className="donde-recomienda" aria-label="Recomendación de Sami">
      <div className="donde-recomienda-cabeza">
        <div className="donde-sami-cara" aria-hidden="true">
          <Sami pose="think" />
        </div>
        <div>
          <h2>{titulo(esLaMejor, recorrido)}</h2>
          <p>
            Elegí una zona para ver por qué la recomiendo y qué tenés que tramitar para abrir ahí.
          </p>
        </div>
      </div>
      <ol className="donde-mejores" aria-label="Mejores zonas">
        {mejores.map((item) => (
          <BotonDeZona key={item.zona.id} item={item} recorrido={recorrido} />
        ))}
      </ol>
    </section>
  );
}
