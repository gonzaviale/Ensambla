import { useState, type ReactNode } from 'react';
import {
  ETIQUETA_DE_COINCIDENCIA,
  NOMBRE_DE_JURISDICCION,
  formatoKm,
  type Filtros,
  type ZonaPuntuada,
} from '@/lib/dondeAbrir';
import { GuiaDeTramites } from './GuiaDeTramites';
import { PorQueEstaZona } from './PorQueEstaZona';

type Pestania = 'porque' | 'tramites';

type Props = {
  item: ZonaPuntuada;
  filtros: Filtros;
  superficie: number;
  onSuperficie: (metros: number) => void;
  calculoAbierto: boolean;
  onAlternarCalculo: () => void;
};

const PESTANIAS: { clave: Pestania; titulo: string }[] = [
  { clave: 'porque', titulo: 'Por qué esta zona' },
  { clave: 'tramites', titulo: 'Qué tenés que tramitar' },
];

function CabezaDelDetalle({ item }: { item: ZonaPuntuada }) {
  return (
    <header className="donde-detalle-cabeza">
      <div>
        <h3 id="donde-detalle-titulo">{item.zona.nombre}</h3>
        <p>
          {NOMBRE_DE_JURISDICCION[item.zona.jurisdiccion]} · {formatoKm(item.distanciaKm)} de tu
          local
        </p>
      </div>
      <span className={`donde-sello ${item.coincidencia}`}>
        <i aria-hidden /> {ETIQUETA_DE_COINCIDENCIA[item.coincidencia]}
      </span>
    </header>
  );
}

type PropsDePestanias = { activa: Pestania; nombreDeLaZona: string; elegir: (p: Pestania) => void };

function Pestanias({ activa, nombreDeLaZona, elegir }: PropsDePestanias) {
  return (
    <div className="donde-pestanias" role="tablist" aria-label={`Sobre ${nombreDeLaZona}`}>
      {PESTANIAS.map(({ clave, titulo }) => (
        <button
          key={clave}
          type="button"
          role="tab"
          id={`pestania-${clave}`}
          aria-selected={activa === clave}
          aria-controls={`panel-${clave}`}
          onClick={() => elegir(clave)}
        >
          {titulo}
        </button>
      ))}
    </div>
  );
}

const CONTENIDO_DE_PESTANIA: Record<Pestania, (props: Props) => ReactNode> = {
  porque: ({ item, filtros, calculoAbierto, onAlternarCalculo }) => (
    <PorQueEstaZona
      item={item}
      filtros={filtros}
      calculoAbierto={calculoAbierto}
      onAlternarCalculo={onAlternarCalculo}
    />
  ),
  tramites: ({ item, filtros, superficie, onSuperficie }) => (
    <GuiaDeTramites
      zona={item.zona}
      rubro={filtros.rubro}
      superficie={superficie}
      onSuperficie={onSuperficie}
    />
  ),
};

// La zona elegida, en dos partes: por qué se la recomienda y qué hay que tramitar para abrir ahí.
export function DetalleDeZona(props: Props) {
  const [pestania, setPestania] = useState<Pestania>('porque');
  return (
    <article className="donde-detalle" aria-labelledby="donde-detalle-titulo">
      <CabezaDelDetalle item={props.item} />
      <Pestanias activa={pestania} nombreDeLaZona={props.item.zona.nombre} elegir={setPestania} />
      <div role="tabpanel" id={`panel-${pestania}`} aria-labelledby={`pestania-${pestania}`}>
        {CONTENIDO_DE_PESTANIA[pestania](props)}
      </div>
    </article>
  );
}
