import type { ProductoFaltante } from '@/types/reposicion';
import type { DecisionDelProducto } from '@/lib/sistema/pedidoProveedor';

type Props = {
  producto: ProductoFaltante;
  decision: DecisionDelProducto;
  onCambio: (cambio: Partial<DecisionDelProducto>) => void;
};

// Un producto del pedido: se tilda para pedirlo y se ajusta cuánto. Abajo, por qué hace falta.
export function FilaDelPedido({ producto, decision, onCambio }: Props) {
  const idDeCantidad = `pedido-cantidad-${producto.id}`;
  return (
    <li className="pedido-fila">
      <input
        type="checkbox"
        id={`pedido-elegido-${producto.id}`}
        checked={decision.pedir}
        aria-label={`Pedir ${producto.nombre}`}
        onChange={(evento) => onCambio({ pedir: evento.target.checked })}
      />
      <div className="pedido-fila-texto">
        <strong>{producto.nombre}</strong>
        <span>{producto.situacion}</span>
      </div>
      <div className="pedido-fila-cantidad">
        <label htmlFor={idDeCantidad}>Cantidad de {producto.nombre}</label>
        <input
          className="input"
          id={idDeCantidad}
          type="number"
          inputMode="decimal"
          min="0"
          step="any"
          value={decision.cantidad}
          disabled={!decision.pedir}
          onChange={(evento) => onCambio({ cantidad: evento.target.value })}
        />
      </div>
    </li>
  );
}
