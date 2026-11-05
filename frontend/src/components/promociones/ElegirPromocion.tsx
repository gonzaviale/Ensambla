import { formatoPesos } from '@/lib/sistema/formato';
import type { Promocion } from '@/types/promociones';

type Props = {
  promociones: Promocion[];
  elegida: string | null;
  onElegir: (clave: string) => void;
};

const MAXIMO_VISIBLE = 3;

const resumenDeProductos = (promocion: Promocion) => {
  const visibles = promocion.productos.slice(0, MAXIMO_VISIBLE);
  const sobrantes = promocion.productos.length - visibles.length;
  const nombres = visibles.map((producto) => `${producto.nombre} (${formatoPesos(producto.precio)})`);
  return sobrantes > 0 ? `${nombres.join(', ')} y ${sobrantes} más` : nombres.join(', ');
};

// Las ideas para promocionar, una tarjeta cada una. Elegir una abre la elección del canal.
export function ElegirPromocion({ promociones, elegida, onElegir }: Props) {
  return (
    <ul className="promociones-ideas" aria-label="Qué promocionar">
      {promociones.map((promocion) => (
        <li key={promocion.clave}>
          <button
            type="button"
            className={promocion.clave === elegida ? 'promocion-idea activa' : 'promocion-idea'}
            aria-pressed={promocion.clave === elegida}
            onClick={() => onElegir(promocion.clave)}
          >
            <strong>{promocion.titulo}</strong>
            <span>{promocion.gancho}</span>
            <small>{resumenDeProductos(promocion)}</small>
          </button>
        </li>
      ))}
    </ul>
  );
}
