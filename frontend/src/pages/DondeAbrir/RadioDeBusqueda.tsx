import { useId } from 'react';
import { MapPinIcon } from '@phosphor-icons/react';
import { SIN_LIMITE_KM, formatoKm } from '@/lib/dondeAbrir';

type Props = {
  valor: number;
  onCambiar: (valor: number) => void;
};

// La distancia define el área visible del análisis, por eso vive junto al mapa y no entre los factores de puntaje.
export function RadioDeBusqueda({ valor, onCambiar }: Props) {
  const id = useId();
  const texto = valor >= SIN_LIMITE_KM ? 'Sin límite' : `Hasta ${formatoKm(valor)}`;

  return (
    <section className="donde-radio-busqueda" aria-labelledby={`${id}-titulo`}>
      <span className="donde-radio-icono" aria-hidden="true">
        <MapPinIcon size={18} weight="fill" />
      </span>
      <div>
        <label id={`${id}-titulo`} htmlFor={id}>
          Radio de búsqueda <output htmlFor={id}>{texto}</output>
        </label>
        <input
          id={id}
          type="range"
          min={1}
          max={SIN_LIMITE_KM}
          step={0.5}
          value={valor}
          aria-valuetext={texto}
          onChange={(evento) => onCambiar(Number(evento.target.value))}
        />
      </div>
    </section>
  );
}
