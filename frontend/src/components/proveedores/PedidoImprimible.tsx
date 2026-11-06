import { moneda } from '@/lib/proveedores/calculos';
import type { ProveedoresDemo } from '@/hooks/proveedores/useProveedoresDemo';

type Props = Pick<ProveedoresDemo, 'proveedor' | 'lineasParaImprimir' | 'totalPedido'>;
export function PedidoImprimible({ proveedor, lineasParaImprimir, totalPedido }: Props) {
  return (
    <section className="prov-print" aria-hidden="true">
      <p>ENSAMBLA · PEDIDO DE DEMOSTRACIÓN</p>
      <h1>Pedido a {proveedor.nombre}</h1>
      {proveedor.telefono && <p>Contacto: {proveedor.telefono}</p>}
      <p>A confirmar por el proveedor: disponibilidad, precios y entrega.</p>
      <table className="prov-tabla">
        <thead>
          <tr>
            <th>Producto</th>
            <th>Presentación</th>
            <th>Unidades</th>
          </tr>
        </thead>
        <tbody>
          {lineasParaImprimir.map((l) => (
            <tr key={l.productoId}>
              <td>{l.producto.nombre}</td>
              <td>{l.producto.presentacion}</td>
              <td>{l.cantidad}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p>
        Estimado según últimos costos: <strong>{moneda(totalPedido)}</strong> (ARS). No es una orden
        de pago.
      </p>
    </section>
  );
}
