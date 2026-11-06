import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import { moneda } from '@/lib/proveedores/calculos';
import type { ProveedoresDemo } from '@/hooks/proveedores/useProveedoresDemo';
import { PanelProveedor } from './PanelProveedor';

type Props = Pick<
  ProveedoresDemo,
  'datos' | 'proveedor' | 'productoNuevo' | 'setProductoNuevo' | 'guardarProducto'
>;
export function StockProveedor(p: Props) {
  return (
    <div className="prov-dos-columnas">
      <PanelProveedor
        titulo="Stock de prueba"
        detalle="Todos los proveedores. Confirmar una compra suma las unidades recibidas y actualiza el costo de referencia."
        icono="box"
      >
        <div className="prov-tabla-scroll">
          <table className="prov-tabla">
            <caption className="sr-only">Inventario de demostración</caption>
            <thead>
              <tr>
                <th>Producto</th>
                <th>Stock</th>
                <th>Mínimo</th>
                <th>Costo</th>
                <th>Venta</th>
              </tr>
            </thead>
            <tbody>
              {p.datos.productos.map((v) => (
                <tr key={v.id}>
                  <td>
                    <strong>{v.nombre}</strong>
                    <small>
                      {p.datos.proveedores.find((s) => s.id === v.proveedorId)?.nombre} ·{' '}
                      {v.presentacion}
                    </small>
                  </td>
                  <td>
                    <span
                      className={`prov-insignia ${v.stock === 0 ? 'alerta' : v.stock < v.minimo ? 'atencion' : 'neutra'}`}
                    >
                      {v.stock}
                    </span>
                  </td>
                  <td>{v.minimo}</td>
                  <td>{moneda(v.costo)}</td>
                  <td>{moneda(v.precio)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </PanelProveedor>
      <PanelProveedor
        titulo="Sumar un producto"
        detalle={`Se va a pedir a ${p.proveedor.nombre}. Podés cargar otra presentación como un producto distinto.`}
        icono="plus"
      >
        <form
          className="prov-form"
          onSubmit={(e) => {
            e.preventDefault();
            p.guardarProducto();
          }}
        >
          <label className="prov-campo">
            Nombre del producto
            <input
              required
              maxLength={120}
              value={p.productoNuevo.nombre}
              onChange={(e) => p.setProductoNuevo({ ...p.productoNuevo, nombre: e.target.value })}
              placeholder="Ej. Arroz Gallo 1 kg"
            />
          </label>
          <label className="prov-campo">
            Unidad o presentación
            <input
              required
              maxLength={80}
              value={p.productoNuevo.presentacion}
              onChange={(e) =>
                p.setProductoNuevo({ ...p.productoNuevo, presentacion: e.target.value })
              }
              placeholder="Ej. Paquete de 1 kg"
            />
          </label>
          <div className="prov-form-fila">
            <label className="prov-campo">
              Stock inicial
              <input
                type="number"
                min="0"
                max="1000000"
                step="1"
                required
                value={p.productoNuevo.stock}
                onChange={(e) =>
                  p.setProductoNuevo({ ...p.productoNuevo, stock: Number(e.target.value) })
                }
              />
            </label>
            <label className="prov-campo">
              Mínimo deseado
              <input
                type="number"
                min="0"
                max="1000000"
                step="1"
                required
                value={p.productoNuevo.minimo}
                onChange={(e) =>
                  p.setProductoNuevo({ ...p.productoNuevo, minimo: Number(e.target.value) })
                }
              />
            </label>
          </div>
          <div className="prov-form-fila">
            <label className="prov-campo">
              Costo por unidad ($)
              <input
                type="number"
                min="0"
                max="1000000000"
                step="0.01"
                required
                value={p.productoNuevo.costo}
                onChange={(e) =>
                  p.setProductoNuevo({ ...p.productoNuevo, costo: Number(e.target.value) })
                }
              />
            </label>
            <label className="prov-campo">
              Precio de venta ($)
              <input
                type="number"
                min="0"
                max="1000000000"
                step="0.01"
                required
                value={p.productoNuevo.precio}
                onChange={(e) =>
                  p.setProductoNuevo({ ...p.productoNuevo, precio: Number(e.target.value) })
                }
              />
            </label>
          </div>
          <Btn cls="primary" type="submit">
            <Icon name="plus" /> Guardar producto de prueba
          </Btn>
        </form>
      </PanelProveedor>
    </div>
  );
}
