import { ArrowCounterClockwiseIcon, SlidersHorizontalIcon } from '@phosphor-icons/react';
import {
  FACTORES,
  FILTROS_INICIALES,
  PRESETS,
  conPreset,
  presetActivo,
  type FactorClave,
  type Filtros,
  type Importancia,
} from '@/lib/dondeAbrir';

const NIVELES: { valor: Importancia; etiqueta: string }[] = [
  { valor: 0, etiqueta: 'Nada' },
  { valor: 1, etiqueta: 'Algo' },
  { valor: 2, etiqueta: 'Mucho' },
];

type Props = {
  filtros: Filtros;
  activos: number;
  onCambiar: (filtros: Filtros) => void;
};

type Factor = (typeof FACTORES)[number];

function CabezaDelPanel({
  activos,
  onRestablecer,
}: {
  activos: number;
  onRestablecer: () => void;
}) {
  return (
    <header className="donde-filtros-cabeza">
      <div>
        <span className="donde-filtros-icono" aria-hidden="true">
          <SlidersHorizontalIcon size={18} />
        </span>
        <div>
          <h2>Qué priorizás</h2>
          <p>Elegí un modo o ajustá cada punto.</p>
        </div>
      </div>
      <button
        type="button"
        className="donde-filtros-restablecer"
        aria-label="Restablecer"
        title="Restablecer"
        disabled={activos === 0}
        onClick={onRestablecer}
      >
        <ArrowCounterClockwiseIcon aria-hidden size={20} />
      </button>
    </header>
  );
}

function ModosRapidos({ filtros, onCambiar }: Pick<Props, 'filtros' | 'onCambiar'>) {
  const preset = presetActivo(filtros);
  return (
    <div className="donde-modos" role="group" aria-label="Modos rápidos">
      {PRESETS.map((item) => (
        <button
          key={item.id}
          type="button"
          aria-pressed={preset === item.id}
          onClick={() => onCambiar(conPreset(filtros, item))}
        >
          {item.nombre}
        </button>
      ))}
      <span
        className="donde-modo-personalizado"
        aria-current={preset === null ? 'true' : undefined}
      >
        Personalizado
      </span>
    </div>
  );
}

type PropsDelFactor = {
  factor: Factor;
  importancia: Importancia;
  onCambiar: (valor: Importancia) => void;
};

function NivelesDelFactor({ factor, importancia, onCambiar }: PropsDelFactor) {
  return (
    <div className="donde-niveles">
      {NIVELES.map((nivel) => (
        <label
          key={nivel.valor}
          className={importancia === nivel.valor ? 'marcada' : ''}
          data-nivel={nivel.valor}
        >
          <input
            type="radio"
            name={`importancia-${factor.clave}`}
            value={nivel.valor}
            checked={importancia === nivel.valor}
            onChange={() => onCambiar(nivel.valor)}
          />
          <span>{nivel.etiqueta}</span>
        </label>
      ))}
    </div>
  );
}

function ImportanciaDelFactor(props: PropsDelFactor) {
  const { factor } = props;
  return (
    <div className="donde-factor" role="group" aria-label={factor.etiqueta} title={factor.ayuda}>
      <span className="donde-factor-nombre">{factor.etiqueta}</span>
      <NivelesDelFactor {...props} />
    </div>
  );
}

// Los modos rápidos son puntos de partida. Cada factor queda siempre a mano para personalizar la recomendación.
export function PanelDeFiltros({ filtros, activos, onCambiar }: Props) {
  const cambiarImportancia = (clave: FactorClave, valor: Importancia) =>
    onCambiar({ ...filtros, importancia: { ...filtros.importancia, [clave]: valor } });
  const restablecer = () =>
    onCambiar({ ...FILTROS_INICIALES, rubro: filtros.rubro, publico: filtros.publico });

  return (
    <section className="donde-filtros" aria-label="Ajustes de la recomendación">
      <CabezaDelPanel activos={activos} onRestablecer={restablecer} />
      <ModosRapidos filtros={filtros} onCambiar={onCambiar} />
      <div className="donde-factores">
        {FACTORES.map((factor) => (
          <ImportanciaDelFactor
            key={factor.clave}
            factor={factor}
            importancia={filtros.importancia[factor.clave]}
            onCambiar={(valor) => cambiarImportancia(factor.clave, valor)}
          />
        ))}
      </div>
    </section>
  );
}
