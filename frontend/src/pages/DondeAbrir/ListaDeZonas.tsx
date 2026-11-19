import { useDesplazarHastaLaZona } from '@/hooks/dondeAbrir/useDesplazarHastaLaZona';
import {
  ETIQUETA_DE_COINCIDENCIA,
  SIGLA_DE_JURISDICCION,
  formatoKm,
  formatoPuntaje,
  type Descartada,
  type ZonaPuntuada,
} from '@/lib/dondeAbrir';
import type { CambioDePuesto } from '@/lib/dondeAbrirRecorrido';

type Props = {
  ranking: ZonaPuntuada[];
  descartadas: Descartada[];
  seleccionadaId: string;
  cambios: Record<string, CambioDePuesto>;
  onElegir: (id: string) => void;
};

const CAMBIO_DE_PUESTO = {
  sube: { verbo: 'Subió', flecha: '▲' },
  baja: { verbo: 'Bajó', flecha: '▼' },
};

const textoDePuestos = (cantidad: number) => `${cantidad} ${cantidad === 1 ? 'puesto' : 'puestos'}`;

function Cambio({ cambio }: { cambio: CambioDePuesto | undefined }) {
  if (cambio === undefined || cambio === 0) return null;
  if (cambio === 'nueva') {
    return (
      <em className="donde-cambio nueva" aria-label="Zona que antes estaba descartada">
        Vuelve
      </em>
    );
  }
  const sentido = cambio > 0 ? 'sube' : 'baja';
  const { verbo, flecha } = CAMBIO_DE_PUESTO[sentido];
  const cantidad = Math.abs(cambio);
  return (
    <em className={`donde-cambio ${sentido}`} aria-label={`${verbo} ${textoDePuestos(cantidad)}`}>
      {flecha} {cantidad}
    </em>
  );
}

type PropsDeZona = {
  item: ZonaPuntuada;
  puesto: number;
  activa: boolean;
  cambio: CambioDePuesto | undefined;
  onElegir: (id: string) => void;
};

const claseDeLaZona = (item: ZonaPuntuada, activa: boolean) =>
  ['donde-zona', item.coincidencia, activa && 'activa'].filter(Boolean).join(' ');

function ZonaDelRanking({ item, puesto, activa, cambio, onElegir }: PropsDeZona) {
  return (
    <li>
      <button
        type="button"
        id={`zona-${item.zona.id}`}
        className={claseDeLaZona(item, activa)}
        aria-pressed={activa}
        onClick={() => onElegir(item.zona.id)}
      >
        <span className="donde-zona-puesto" aria-hidden="true">
          {puesto}
        </span>
        <span className="donde-zona-texto">
          <strong>
            {item.zona.nombre}
            <Cambio cambio={cambio} />
          </strong>
          <span>
            {SIGLA_DE_JURISDICCION[item.zona.jurisdiccion]} · {formatoKm(item.distanciaKm)} de tu
            local
          </span>
        </span>
        <span className="donde-nota">
          {formatoPuntaje(item.puntaje)}
          <span className="sr-only"> · {ETIQUETA_DE_COINCIDENCIA[item.coincidencia]}</span>
        </span>
      </button>
    </li>
  );
}

function ZonasRecomendadas({
  ranking,
  seleccionadaId,
  cambios,
  onElegir,
}: Omit<Props, 'descartadas'>) {
  if (ranking.length === 0) {
    return (
      <p className="donde-sin-zonas" role="status">
        Con esos límites no quedó ninguna zona. Probá aflojar alguno.
      </p>
    );
  }
  return (
    <ol className="donde-zonas" aria-label="Zonas recomendadas, de la mejor a la peor">
      {ranking.map((item, indice) => (
        <ZonaDelRanking
          key={item.zona.id}
          item={item}
          puesto={indice + 1}
          activa={item.zona.id === seleccionadaId}
          cambio={cambios[item.zona.id]}
          onElegir={onElegir}
        />
      ))}
    </ol>
  );
}

function ZonasDescartadas({ descartadas }: { descartadas: Descartada[] }) {
  if (descartadas.length === 0) return null;
  return (
    <details className="donde-descartadas">
      <summary>
        Descartadas por tus límites <b>{descartadas.length}</b>
      </summary>
      <ul>
        {descartadas.map((item) => (
          <li key={item.zona.id}>
            <strong>{item.zona.nombre}</strong>
            <span>{item.motivos.join(' · ')}</span>
          </li>
        ))}
      </ul>
    </details>
  );
}

export function ListaDeZonas({ descartadas, ...recomendadas }: Props) {
  const lista = useDesplazarHastaLaZona(recomendadas.seleccionadaId);
  return (
    <div ref={lista} className="donde-lista" role="region" aria-label="Lista de zonas" tabIndex={0}>
      <ZonasRecomendadas {...recomendadas} />
      <ZonasDescartadas descartadas={descartadas} />
    </div>
  );
}
