import { pesos } from '@/lib/carteles';
import type { PromocionSugerida } from '@/types/carteles';

type Acciones = {
  deshabilitado: boolean;
  onElegir: (promocion: PromocionSugerida) => void;
};

export function PromocionesSugeridas({
  promociones,
  deshabilitado,
  onElegir,
}: Acciones & { promociones: PromocionSugerida[] }) {
  return (
    <ul className="carteles-promociones" aria-label="Qué promocionar">
      {promociones.map((promocion) => (
        <TarjetaDePromocion
          key={promocion.productos.join('|')}
          promocion={promocion}
          deshabilitado={deshabilitado}
          onElegir={onElegir}
        />
      ))}
    </ul>
  );
}

function TarjetaDePromocion({
  promocion,
  deshabilitado,
  onElegir,
}: Acciones & { promocion: PromocionSugerida }) {
  const nombre = promocion.productos.join(' + ');
  return (
    <li className="carteles-promocion">
      <strong>{nombre}</strong>
      <span>
        De {pesos(promocion.precioAnterior)} a {pesos(promocion.precioNuevo)}: ahorrás{' '}
        {pesos(promocion.ahorro)} ({promocion.descuentoPorcentaje} %)
      </span>
      <button
        type="button"
        className="btn primary"
        disabled={deshabilitado}
        onClick={() => onElegir(promocion)}
      >
        Armar cartel de {nombre}
      </button>
    </li>
  );
}
