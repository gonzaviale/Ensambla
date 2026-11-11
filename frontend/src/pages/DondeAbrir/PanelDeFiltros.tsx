import { useId, useState } from 'react';
import {
  ArrowCounterClockwiseIcon,
  CaretDownIcon,
  SlidersHorizontalIcon,
} from '@phosphor-icons/react';
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
  { valor: 0, etiqueta: 'Nada' },
  { valor: 1, etiqueta: 'Importa' },
  { valor: 2, etiqueta: 'Mucho' },
];

type Props = {
  filtros: Filtros;
  /** Cuántos filtros se apartan de lo de siempre. */
  activos: number;
  onCambiar: (filtros: Filtros) => void;
};

// Los filtros arrancan cerrados: el resultado es lo primero que se ve. Cada cambio recalcula las zonas al instante.
export function PanelDeFiltros({ filtros, activos, onCambiar }: Props) {
  const [abierto, setAbierto] = useState(false);
  const idCuerpo = useId();
  const preset = presetActivo(filtros);

  const cambiarLimite = <K extends keyof Limites>(clave: K, valor: Limites[K]) =>
    onCambiar({ ...filtros, limites: { ...filtros.limites, [clave]: valor } });
  const cambiarImportancia = (clave: FactorClave, valor: Importancia) =>
    onCambiar({ ...filtros, importancia: { ...filtros.importancia, [clave]: valor } });

  return (
    <section className="donde-filtros" aria-label="Filtros">
      <div className="donde-filtros-cabeza">
        <button
          type="button"
          className="donde-filtros-boton"
          aria-expanded={abierto}
          aria-controls={idCuerpo}
          onClick={() => setAbierto((valor) => !valor)}
        >
          <SlidersHorizontalIcon aria-hidden size={18} />
          <span>Ajustar con filtros</span>
          {activos > 0 && <b aria-label={`${activos} activos`}>{activos}</b>}
          <CaretDownIcon aria-hidden size={14} className="flecha" />
        </button>
        {abierto && (
          <button
            type="button"
            className="donde-filtros-restablecer"
            disabled={activos === 0}
            onClick={() =>
              onCambiar({ ...FILTROS_INICIALES, rubro: filtros.rubro, publico: filtros.publico })
            }
          >
            <ArrowCounterClockwiseIcon aria-hidden size={14} /> Restablecer
          </button>
        )}
      </div>

      {abierto && (
        <div id={idCuerpo} className="donde-filtros-cuerpo">
          <div className="donde-bloque">
            <h3>Qué te importa</h3>
            <div className="donde-presets" role="group" aria-label="Combinaciones rápidas">
              {PRESETS.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  aria-pressed={preset === item.id}
                  className={preset === item.id ? 'activo' : ''}
                  onClick={() => onCambiar(conPreset(filtros, item))}
                >
                  {item.nombre}
                </button>
              ))}
            </div>
            <div className="donde-factores">
              {FACTORES.map((factor) => (
                <div key={factor.clave} className="donde-factor" title={factor.ayuda}>
                  <span id={`factor-${factor.clave}`}>{factor.etiqueta}</span>
                  <div
                    className="donde-segmento"
                    role="radiogroup"
                    aria-label={`Importancia de ${factor.etiqueta.toLowerCase()}`}
                  >
                    {NIVELES.map((nivel) => (
                      <label
                        key={nivel.valor}
                        className={
                          filtros.importancia[factor.clave] === nivel.valor ? 'marcada' : ''
                        }
                        data-nivel={nivel.valor}
                      >
                        <input
                          type="radio"
                          name={`importancia-${factor.clave}`}
                          checked={filtros.importancia[factor.clave] === nivel.valor}
                          onChange={() => cambiarImportancia(factor.clave, nivel.valor)}
                        />
                        {nivel.etiqueta}
                      </label>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="donde-bloque">
            <h3>Límites que descartan zonas</h3>
            <div className="donde-limites">
              <Deslizador
                etiqueta="Distancia a tu local"
                valor={filtros.limites.distanciaMaxKm}
                minimo={1}
                maximo={SIN_LIMITE_KM}
                paso={0.5}
                texto={(valor) =>
                  valor >= SIN_LIMITE_KM ? 'Sin límite' : `Hasta ${formatoKm(valor)}`
                }
                onCambiar={(valor) => cambiarLimite('distanciaMaxKm', valor)}
              />
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
        </div>
      )}
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
