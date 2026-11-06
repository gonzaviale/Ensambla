import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import { moneda } from '@/lib/proveedores/calculos';
import type { ProveedoresDemo } from '@/hooks/proveedores/useProveedoresDemo';
import type { RevisionCompra } from '@/types/proveedores';
import { PanelProveedor } from './PanelProveedor';

type Props = Pick<
  ProveedoresDemo,
  | 'datos'
  | 'actualizarRevision'
  | 'actualizarLinea'
  | 'agregarLineaCompra'
  | 'revisado'
  | 'setRevisado'
  | 'guardarCompra'
  | 'cancelarRevision'
  | 'errorRevision'
  | 'totalRevision'
> & { revision: RevisionCompra };
export function RevisionProveedor(p: Props) {
  const r = p.revision;
  return (
    <PanelProveedor
      titulo="Revisá antes de confirmar"
      detalle="Lectura simulada: los renglones son de ejemplo y no se extrajeron del archivo."
      icono="eye"
    >
      <p className="prov-nota">
        Comprobá producto, presentación y costo por envase. Si el ticket dice «caja x12», cargá 12
        unidades y el costo de cada unidad. Para otra presentación, creá otro producto desde Stock
        de prueba.
      </p>
      <div className="prov-form-fila">
        <label className="prov-campo">
          Proveedor del comprobante
          <select
            value={r.proveedorId}
            onChange={(e) => p.actualizarRevision({ proveedorId: e.target.value })}
          >
            {p.datos.proveedores.map((v) => (
              <option key={v.id} value={v.id}>
                {v.nombre}
              </option>
            ))}
          </select>
        </label>
        <label className="prov-campo">
          Número del comprobante
          <input
            required
            maxLength={80}
            value={r.numero}
            onChange={(e) => p.actualizarRevision({ numero: e.target.value })}
          />
        </label>
        <label className="prov-campo">
          Fecha del comprobante
          <input
            type="date"
            required
            value={r.fecha}
            onChange={(e) => p.actualizarRevision({ fecha: e.target.value })}
          />
        </label>
      </div>
      <div className="prov-lista">
        {r.lineas.map((linea, i) => (
          <div className="prov-revision-linea" key={i}>
            <label className="prov-campo">
              Producto del renglón {i + 1}
              <select
                aria-label={`Producto del renglón ${i + 1}`}
                value={linea.productoId}
                onChange={(e) => p.actualizarLinea(i, { productoId: e.target.value })}
              >
                <option value="">Vinculá un producto</option>
                {p.datos.productos.map((v) => (
                  <option key={v.id} value={v.id}>
                    {v.nombre} · {v.presentacion}
                  </option>
                ))}
              </select>
            </label>
            <label className="prov-campo">
              Unidades
              <input
                aria-label={`Unidades del renglón ${i + 1}`}
                type="number"
                min="1"
                max="1000000"
                step="1"
                value={linea.cantidad}
                onChange={(e) => p.actualizarLinea(i, { cantidad: Number(e.target.value) })}
              />
            </label>
            <label className="prov-campo">
              Costo unitario ($)
              <input
                aria-label={`Costo del renglón ${i + 1}`}
                type="number"
                min="0.01"
                max="1000000000"
                step="0.01"
                value={linea.costo}
                onChange={(e) => p.actualizarLinea(i, { costo: Number(e.target.value) })}
              />
            </label>
            <Btn
              cls="ghost small"
              aria-label={`Quitar renglón ${i + 1}`}
              onClick={() =>
                p.actualizarRevision({ lineas: r.lineas.filter((_, indice) => i !== indice) })
              }
            >
              <Icon name="trash" />
            </Btn>
          </div>
        ))}
      </div>
      <Btn cls="secondary small" onClick={p.agregarLineaCompra}>
        <Icon name="plus" /> Agregar renglón
      </Btn>
      <div className="prov-form-fila">
        <label className="prov-campo">
          Otros cargos / envío ($)
          <input
            type="number"
            min="0"
            step="0.01"
            value={r.extras}
            onChange={(e) => p.actualizarRevision({ extras: Number(e.target.value) })}
          />
        </label>
        <label className="prov-campo">
          Descuento total ($)
          <input
            type="number"
            min="0"
            step="0.01"
            value={r.descuento}
            onChange={(e) => p.actualizarRevision({ descuento: Number(e.target.value) })}
          />
        </label>
        <label className="prov-campo">
          Total del comprobante ($)
          <input
            type="number"
            min="0.01"
            step="0.01"
            value={r.totalDeclarado}
            onChange={(e) => p.actualizarRevision({ totalDeclarado: Number(e.target.value) })}
          />
        </label>
      </div>
      <div className="prov-total">
        <span>Total calculado</span>
        <strong>{moneda(p.totalRevision)}</strong>
      </div>
      <p className="prov-aclaracion">
        Los cargos y descuentos generales integran el desembolso. La comparación usa el costo
        unitario cargado, sin prorratearlos.
      </p>
      {p.errorRevision && (
        <p className="prov-error" role="alert">
          {p.errorRevision}
        </p>
      )}
      <label className="prov-check">
        <input
          type="checkbox"
          checked={p.revisado}
          onChange={(e) => p.setRevisado(e.target.checked)}
        />{' '}
        Revisé productos, unidades, precios y total. Confirmo la recepción de esta compra de prueba.
      </label>
      <div className="prov-acciones">
        <Btn cls="primary" disabled={!p.revisado || !!p.errorRevision} onClick={p.guardarCompra}>
          <Icon name="check" /> Confirmar e ingresar al stock
        </Btn>
        <Btn cls="ghost" onClick={p.cancelarRevision}>
          Cancelar revisión
        </Btn>
      </div>
    </PanelProveedor>
  );
}
