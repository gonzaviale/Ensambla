import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import { moneda } from '@/lib/proveedores/calculos';
import type { ProveedoresDemo } from '@/hooks/proveedores/useProveedoresDemo';
import { PanelProveedor } from './PanelProveedor';

type Props = Pick<
  ProveedoresDemo,
  | 'lineasPedido'
  | 'pedidoValido'
  | 'disponibles'
  | 'productoAAgregar'
  | 'setProductoAAgregar'
  | 'totalPedido'
  | 'mensaje'
  | 'cambiarCantidad'
  | 'quitarLinea'
  | 'agregarLinea'
  | 'regenerar'
  | 'copiarMensaje'
  | 'imprimir'
>;
export function PedidoProveedor(p: Props) {
  return (
    <div className="prov-dos-columnas">
      <PanelProveedor
        titulo="Tu próximo pedido"
        detalle="Agotados y bajo mínimo. Sugerimos reponer hasta dos veces el mínimo; vos decidís."
        icono="orders"
        accion={
          <Btn cls="ghost small" onClick={p.regenerar}>
            <Icon name="undo" /> Recalcular sugerencia
          </Btn>
        }
      >
        <p className="prov-aclaracion">
          Podés quitar lo que ya no vendés o sumar cualquier producto de este proveedor. Preparar un
          pedido todavía no mueve stock.
        </p>
        {p.lineasPedido.length ? (
          <div className="prov-lista">
            {p.lineasPedido.map((l) => (
              <div className="prov-pedido-linea" key={l.productoId}>
                <div>
                  <strong>{l.producto.nombre}</strong>
                  <p>{l.producto.presentacion}</p>
                  <span
                    className={`prov-insignia ${l.producto.stock === 0 ? 'alerta' : l.producto.stock < l.producto.minimo ? 'atencion' : 'neutra'}`}
                  >
                    {l.producto.stock === 0
                      ? 'Agotado'
                      : l.producto.stock < l.producto.minimo
                        ? 'Stock bajo'
                        : 'Agregado por vos'}{' '}
                    · {l.producto.stock} disponibles · mínimo {l.producto.minimo}
                  </span>
                </div>
                <label className="prov-campo prov-cantidad">
                  A pedir
                  <input
                    aria-label={`Cantidad a pedir de ${l.producto.nombre}`}
                    type="number"
                    min="0"
                    max="1000000"
                    step="1"
                    value={l.cantidad}
                    onChange={(e) => p.cambiarCantidad(l.productoId, Number(e.target.value))}
                  />
                </label>
                <Btn
                  cls="ghost small"
                  aria-label={`Quitar ${l.producto.nombre} del pedido`}
                  onClick={() => p.quitarLinea(l.productoId)}
                >
                  <Icon name="trash" />
                </Btn>
              </div>
            ))}
          </div>
        ) : (
          <div className="prov-vacio">
            <Icon name="check-circle" />
            <h3>El pedido está vacío</h3>
            <p>Sumá un producto o recalculá los faltantes de este proveedor.</p>
          </div>
        )}
        <div className="prov-form-fila">
          <label className="prov-campo">
            Agregar otro producto
            <select
              value={p.productoAAgregar}
              onChange={(e) => p.setProductoAAgregar(e.target.value)}
            >
              <option value="">Elegí un producto del proveedor</option>
              {p.disponibles.map((v) => (
                <option key={v.id} value={v.id}>
                  {v.nombre} · stock {v.stock}
                </option>
              ))}
            </select>
          </label>
          <Btn cls="secondary" disabled={!p.productoAAgregar} onClick={p.agregarLinea}>
            <Icon name="plus" /> Agregar
          </Btn>
        </div>
        <div className="prov-total">
          <span>Estimado con el último costo de stock</span>
          <strong>{moneda(p.totalPedido)}</strong>
        </div>
        <p className="prov-aclaracion">
          El proveedor confirma precios y cargos finales. Todos los importes de esta demo están en
          pesos argentinos.
        </p>
      </PanelProveedor>
      <PanelProveedor
        titulo="Listo para compartir"
        detalle="El mensaje se actualiza con tu pedido."
        icono="note"
      >
        <label className="prov-campo">
          Mensaje para WhatsApp
          <textarea
            className="prov-mensaje"
            rows={12}
            value={p.mensaje || 'Sumá productos para preparar el mensaje.'}
            readOnly
          />
        </label>
        <Btn cls="primary full" disabled={!p.pedidoValido} onClick={() => void p.copiarMensaje()}>
          <Icon name="copy" /> Copiar mensaje
        </Btn>
        <Btn cls="secondary full" disabled={!p.pedidoValido} onClick={p.imprimir}>
          <Icon name="download" /> Imprimir / Guardar PDF
        </Btn>
        <p className="prov-aclaracion">
          Pegá el mensaje en WhatsApp o elegí «Guardar como PDF» en la ventana de impresión. Vos
          elegís cuándo enviarlo.
        </p>
      </PanelProveedor>
    </div>
  );
}
