import { fechaCorta, moneda } from '@/lib/proveedores/calculos';
import type { ProveedoresDemo } from '@/hooks/proveedores/useProveedoresDemo';
import { PanelProveedor } from './PanelProveedor';

type Props = Pick<ProveedoresDemo, 'datos' | 'compras'>;
export function HistorialProveedor({ datos, compras }: Props) {
  return (
    <PanelProveedor
      titulo="Historial del proveedor"
      detalle="Comprobantes confirmados. Cada uno queda vinculado a su ingreso de stock y gasto de mercadería."
      icono="clock"
    >
      {!compras.length && (
        <p className="prov-vacio">Este proveedor todavía no tiene compras de prueba.</p>
      )}
      {compras.map((c) => (
        <details className="prov-historial" key={c.id}>
          <summary>
            <span>
              <strong>{c.numero}</strong>
              <small>
                {fechaCorta(c.fecha)} · {c.lineas.length} productos
              </small>
            </span>
            <span className="prov-historial-importe">
              {moneda(c.total)}
              <small>Confirmado · ver detalle</small>
            </span>
          </summary>
          <p className="prov-aclaracion">
            Referencia: {c.archivo || 'Carga manual'} · {c.fecha}
          </p>
          <ul>
            {c.lineas.map((l) => (
              <li key={l.productoId}>
                {l.cantidad} ×{' '}
                {datos.productos.find((p) => p.id === l.productoId)?.nombre ??
                  'Producto no disponible'}{' '}
                · {moneda(l.costo)} por unidad
              </li>
            ))}
          </ul>
          <p>
            Otros cargos: {moneda(c.extras)} · Descuento: {moneda(c.descuento)}
          </p>
        </details>
      ))}
    </PanelProveedor>
  );
}
