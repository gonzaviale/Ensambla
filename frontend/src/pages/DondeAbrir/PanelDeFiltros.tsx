import { useId, useState } from 'react';
import { ArrowCounterClockwiseIcon, SlidersHorizontalIcon } from '@phosphor-icons/react';
import {
  FACTORES,
  FILTROS_INICIALES,
  PRESETS,
  SIN_LIMITE_COMPETENCIA,
  SIN_LIMITE_KM,
  conPreset,
  formatoKm,
  presetActivo,
  type FactorClave,
  type Filtros,
  type Importancia,
  type Limites,
} from '@/lib/dondeAbrir';

const NIVELES: { valor: Importancia; etiqueta: string }[] = [
  { valor: 0, etiqueta: 'No influye' },
  { valor: 1, etiqueta: 'Importa' },
  { valor: 2, etiqueta: 'Importa mucho' },
];

type Props = {
  filtros: Filtros;
  activos: number;
  onCambiar: (filtros: Filtros) => void;
};

// Los dos ajustes más útiles quedan a la vista. El resto sigue disponible sin llenar la pantalla de botones.
export function PanelDeFiltros({ filtros, activos, onCambiar }: Props) {
  const [avanzados, setAvanzados] = useState(false);
  const preset = presetActivo(filtros);

  const cambiarLimite = <K extends keyof Limites>(clave: K, valor: Limites[K]) =>
    onCambiar({ ...filtros, limites: { ...filtros.limites, [clave]: valor } });
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
            <h2>Ajustá la búsqueda</h2>
            <p>El mapa y el ranking cambian al instante.</p>
          </div>
        </div>
        {activos > 0 && <b aria-label={`${activos} filtros activos`}>{activos}</b>}
      </header>

      <div className="donde-filtros-principales">
        <label className="donde-campo-select">
          <span>Prioridad</span>
          <select
            aria-label="Prioridad"
            value={preset ?? 'personalizado'}
            onChange={(evento) => {
              const elegido = PRESETS.find((item) => item.id === evento.target.value);
              if (elegido) onCambiar(conPreset(filtros, elegido));
            }}
          >
            {preset === null && <option value="personalizado">Personalizada</option>}
            {PRESETS.map((item) => (
              <option key={item.id} value={item.id}>
                {item.nombre}
              </option>
            ))}
          </select>
        </label>
        <Deslizador
          etiqueta="Distancia a tu local"
          valor={filtros.limites.distanciaMaxKm}
          minimo={1}
          maximo={SIN_LIMITE_KM}
          paso={0.5}
          texto={(valor) => (valor >= SIN_LIMITE_KM ? 'Sin límite' : `Hasta ${formatoKm(valor)}`)}
          onCambiar={(valor) => cambiarLimite('distanciaMaxKm', valor)}
        />
      </div>

      <details
        className="donde-filtros-avanzados"
        open={avanzados}
        onToggle={(evento) => setAvanzados(evento.currentTarget.open)}
      >
        <summary aria-expanded={avanzados}>Más filtros</summary>
        <div className="donde-filtros-cuerpo">
          <div className="donde-bloque">
            <h3>Cuánto importa cada factor</h3>
            <div className="donde-factores">
              {FACTORES.map((factor) => (
                <label key={factor.clave} className="donde-factor" title={factor.ayuda}>
                  <span>{factor.etiqueta}</span>
                  <select
                    aria-label={`Importancia de ${factor.etiqueta.toLowerCase()}`}
                    value={filtros.importancia[factor.clave]}
                    onChange={(evento) =>
                      cambiarImportancia(factor.clave, Number(evento.target.value) as Importancia)
                    }
                  >
                    {NIVELES.map((nivel) => (
                      <option key={nivel.valor} value={nivel.valor}>
                        {nivel.etiqueta}
                      </option>
                    ))}
                  </select>
                </label>
              ))}
            </div>
          </div>

          <div className="donde-bloque">
            <h3>Límites que descartan zonas</h3>
            <div className="donde-limites">
              <Deslizador
                etiqueta="Comercios similares cerca"
                valor={filtros.limites.competenciaMax}
                minimo={0}
                maximo={SIN_LIMITE_COMPETENCIA}
                paso={1}
                texto={(valor) =>
                  valor >= SIN_LIMITE_COMPETENCIA ? 'Sin límite' : `Hasta ${valor}`
                }
                onCambiar={(valor) => cambiarLimite('competenciaMax', valor)}
              />
              <Deslizador
                etiqueta="Escuelas cerca"
                valor={filtros.limites.escuelasMin}
                minimo={0}
                maximo={9}
                paso={1}
                texto={(valor) => (valor === 0 ? 'No importa' : `Al menos ${valor}`)}
                onCambiar={(valor) => cambiarLimite('escuelasMin', valor)}
              />
              <Deslizador
                etiqueta="Parques y plazas cerca"
                valor={filtros.limites.parquesMin}
                minimo={0}
                maximo={4}
                paso={1}
                texto={(valor) => (valor === 0 ? 'No importa' : `Al menos ${valor}`)}
                onCambiar={(valor) => cambiarLimite('parquesMin', valor)}
              />
              <Deslizador
                etiqueta="Paradas y estaciones cerca"
                valor={filtros.limites.transporteMin}
                minimo={0}
                maximo={9}
                paso={1}
                texto={(valor) => (valor === 0 ? 'No importa' : `Al menos ${valor}`)}
                onCambiar={(valor) => cambiarLimite('transporteMin', valor)}
              />
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
        </div>
      </details>
    </section>
  );
}

function Deslizador({
  etiqueta,
  valor,
  minimo,
  maximo,
  paso,
  texto,
  onCambiar,
}: {
  etiqueta: string;
  valor: number;
  minimo: number;
  maximo: number;
  paso: number;
  texto: (valor: number) => string;
  onCambiar: (valor: number) => void;
}) {
  const id = useId();
  return (
    <div className="donde-deslizador">
      <label htmlFor={id}>
        {etiqueta}
        <output htmlFor={id}>{texto(valor)}</output>
      </label>
      <input
        id={id}
        type="range"
        min={minimo}
        max={maximo}
        step={paso}
        value={valor}
        aria-valuetext={texto(valor)}
        onChange={(evento) => onCambiar(Number(evento.target.value))}
      />
    </div>
  );
}
