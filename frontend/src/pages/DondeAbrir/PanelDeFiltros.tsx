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
  { valor: 1, etiqueta: 'Importa' },
  { valor: 2, etiqueta: 'Mucho' },
];

type Props = {
  filtros: Filtros;
  activos: number;
  onCambiar: (filtros: Filtros) => void;
};

// Los modos rápidos son puntos de partida. Cada factor queda siempre a mano para personalizar la recomendación.
export function PanelDeFiltros({ filtros, activos, onCambiar }: Props) {
  const preset = presetActivo(filtros);

  const cambiarImportancia = (clave: FactorClave, valor: Importancia) =>
    onCambiar({ ...filtros, importancia: { ...filtros.importancia, [clave]: valor } });
  const restablecer = () =>
    onCambiar({ ...FILTROS_INICIALES, rubro: filtros.rubro, publico: filtros.publico });

  return (
    <section className="donde-filtros" aria-label="Ajustes de la recomendación">
      <header className="donde-filtros-cabeza">
        <div>
          <span className="donde-filtros-icono" aria-hidden="true">
            <SlidersHorizontalIcon size={18} />
          </span>
          <div>
            <h2>Qué te importa</h2>
            <p>Elegí un modo o personalizá cada factor.</p>
          </div>
        </div>
        <button
          type="button"
          className="donde-filtros-restablecer"
          disabled={activos === 0}
          onClick={restablecer}
        >
          <ArrowCounterClockwiseIcon aria-hidden size={14} /> Restablecer
        </button>
      </header>

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

      <div className="donde-factores">
        {FACTORES.map((factor) => (
          <div
            key={factor.clave}
            className="donde-factor"
            role="group"
            aria-label={factor.etiqueta}
            title={factor.ayuda}
          >
            <span className="donde-factor-nombre">{factor.etiqueta}</span>
            <div className="donde-niveles">
              {NIVELES.map((nivel) => (
                <label
                  key={nivel.valor}
                  className={filtros.importancia[factor.clave] === nivel.valor ? 'marcada' : ''}
                  data-nivel={nivel.valor}
                >
                  <input
                    type="radio"
                    name={`importancia-${factor.clave}`}
                    value={nivel.valor}
                    checked={filtros.importancia[factor.clave] === nivel.valor}
                    onChange={() => cambiarImportancia(factor.clave, nivel.valor)}
                  />
                  <span>{nivel.etiqueta}</span>
                </label>
              ))}
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
