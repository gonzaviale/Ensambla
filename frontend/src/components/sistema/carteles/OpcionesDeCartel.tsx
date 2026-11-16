import { dibujoComoImagen } from '@/lib/carteles';
import type { OpcionDeCartel, VarianteDeCartel } from '@/types/carteles';

const NOMBRES: Record<VarianteDeCartel, string> = {
  BLOQUE: 'Opción 1 · Bloque',
  FRANJA: 'Opción 2 · Franja',
  MARCO: 'Opción 3 · Marco',
};

type Props = {
  opciones: OpcionDeCartel[];
  elegida: VarianteDeCartel;
  onElegir: (variante: VarianteDeCartel) => void;
};

export function OpcionesDeCartel({ opciones, elegida, onElegir }: Props) {
  return (
    <div className="carteles-opciones" role="radiogroup" aria-label="Opciones de cartel">
      {opciones.map((opcion) => (
        <label key={opcion.variante} className="carteles-opcion">
          <input
            type="radio"
            name="opcion-de-cartel"
            checked={opcion.variante === elegida}
            onChange={() => onElegir(opcion.variante)}
          />
          <img src={dibujoComoImagen(opcion.dibujo)} alt={`Vista previa: ${NOMBRES[opcion.variante]}`} />
          <span>{NOMBRES[opcion.variante]}</span>
        </label>
      ))}
    </div>
  );
}
