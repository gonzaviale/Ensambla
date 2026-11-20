import { Icon } from '@/components/sistema/Icon';
import { permiteFiado } from '@/lib/metricas/metricasAMedida';
import {
  CANTIDAD_DE_PASOS,
  type OpcionDelPaso,
  type PasoDeLaReceta,
} from '@/lib/metricas/pasosDeLaReceta';
import type { DefinicionDeMetrica, FuenteDeMetrica } from '@/types/metricas';

type Props = {
  paso: PasoDeLaReceta;
  numero: number;
  hecho: boolean;
  definicion: DefinicionDeMetrica;
  fuentes: FuenteDeMetrica[];
  onElegir: (definicion: DefinicionDeMetrica) => void;
  onCambiar: (definicion: DefinicionDeMetrica) => void;
};

/** Una pregunta de la receta como tarjetas con ícono: la elegida queda marcada y al tocar otra se sigue. */
export function TarjetasDelPaso({
  paso,
  numero,
  hecho,
  definicion,
  fuentes,
  onElegir,
  onCambiar,
}: Props) {
  const elegida = hecho ? paso.elegida(definicion) : null;
  return (
    <fieldset className="armala-vos-paso" data-color={paso.color}>
      <legend>
        Paso {numero} de {CANTIDAD_DE_PASOS} · {paso.titulo}
      </legend>
      <div className="armala-vos-tarjetas" role="radiogroup" aria-label={paso.titulo}>
        {paso.opciones(definicion, fuentes).map((opcion) => (
          <TarjetaDeOpcion
            key={opcion.valor}
            opcion={opcion}
            elegida={opcion.valor === elegida}
            onElegir={() => onElegir(paso.elegir(definicion, opcion.valor))}
          />
        ))}
      </div>
      <OpcionDelFiado paso={numero} definicion={definicion} onCambiar={onCambiar} />
    </fieldset>
  );
}

type PropsDeLaTarjeta = { opcion: OpcionDelPaso; elegida: boolean; onElegir: () => void };

function TarjetaDeOpcion({ opcion, elegida, onElegir }: PropsDeLaTarjeta) {
  return (
    <button
      type="button"
      role="radio"
      aria-checked={elegida}
      className="armala-vos-tarjeta"
      onClick={onElegir}
    >
      <Icon name={opcion.icono} />
      {opcion.etiqueta}
    </button>
  );
}

const PASO_DE_LA_CUENTA = 2;

type PropsDelFiado = {
  paso: number;
  definicion: DefinicionDeMetrica;
  onCambiar: (definicion: DefinicionDeMetrica) => void;
};

// En la cuenta de las ventas se puede mirar sólo lo fiado: es un interruptor aparte de las tarjetas, y tocarlo no pasa de paso.
function OpcionDelFiado({ paso, definicion, onCambiar }: PropsDelFiado) {
  if (paso !== PASO_DE_LA_CUENTA || !permiteFiado(definicion.fuente)) return null;
  return (
    <button
      type="button"
      role="switch"
      aria-checked={definicion.soloFiado}
      className="armala-vos-fiado"
      onClick={() => onCambiar({ ...definicion, soloFiado: !definicion.soloFiado })}
    >
      <Icon name={definicion.soloFiado ? 'check' : 'users'} />
      Sólo lo fiado
    </button>
  );
}
