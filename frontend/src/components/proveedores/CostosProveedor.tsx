import { moneda, fechaCorta } from '@/lib/proveedores/calculos';
import type { ProveedoresDemo } from '@/hooks/proveedores/useProveedoresDemo';
import { PanelProveedor } from './PanelProveedor';

type Props = Pick<ProveedoresDemo, 'comparaciones' | 'costosComparables' | 'costosSinReferencia'>;
export function CostosProveedor({ comparaciones, costosComparables, costosSinReferencia }: Props) {
  return (
    <PanelProveedor
      titulo="¿Cambió lo que te cobra?"
      detalle="Últimas dos compras confirmadas del mismo producto, presentación y proveedor."
      icono="chart"
    >
      <p className="prov-aclaracion">
        Compará siempre el mismo envase. Los porcentajes reflejan costos nominales; no ajustan
        inflación, impuestos ni bonificaciones generales.
      </p>
      {!comparaciones.length && (
        <p className="prov-vacio">Confirmá una compra de este proveedor para ver sus costos.</p>
      )}
      <div className="prov-costos-lista">
        {costosComparables.map((c) => (
          <article className="prov-costo" key={c.producto.id}>
            <div className="prov-costo-cabeza">
              <div>
                <h3>{c.producto.nombre}</h3>
                <p>
                  {c.producto.presentacion} · compra del {fechaCorta(c.compra.fecha)}
                </p>
              </div>
              <span
                className={`prov-insignia ${c.variacion !== null && c.variacion > 0 ? 'alerta' : 'neutra'}`}
              >
                {c.variacion === null
                  ? 'Primera compra'
                  : `${c.variacion > 0 ? '+' : ''}${c.variacion.toFixed(1).replace('.', ',')} %`}
              </span>
            </div>
            <dl className="prov-costos-cifras">
              <div>
                <dt>Compra anterior</dt>
                <dd>{c.anterior === null ? 'Sin referencia' : moneda(c.anterior)}</dd>
              </div>
              <div>
                <dt>Último costo unitario</dt>
                <dd>{moneda(c.linea.costo)}</dd>
              </div>
              <div>
                <dt>Tu precio de venta</dt>
                <dd>{moneda(c.producto.precio)}</dd>
              </div>
              <div>
                <dt>Margen bruto con ese costo</dt>
                <dd>
                  {c.margenActual === null
                    ? 'Sin precio'
                    : `${c.margenActual.toFixed(1).replace('.', ',')} %`}
                </dd>
              </div>
            </dl>
            {c.variacion !== null && c.variacion > 0 && (
              <p className="prov-nota">
                <strong>Revisá tu precio.</strong>{' '}
                {c.precioSugerido !== null ? (
                  <>
                    Para conservar el porcentaje de margen que daría tu precio de venta actual con
                    el costo anterior, la referencia es <strong>{moneda(c.precioSugerido)}</strong>.
                  </>
                ) : (
                  'No hay un margen anterior positivo para calcular una referencia.'
                )}{' '}
                Es una sugerencia: decidí considerando tus gastos y tu mercado. El precio de venta
                no se modifica.
              </p>
            )}
            {c.variacion === null && (
              <p className="prov-aclaracion">
                Con otra compra de este producto vas a poder comparar la variación.
              </p>
            )}
          </article>
        ))}
      </div>
      {!!costosSinReferencia.length && (
        <details className="prov-adjunto">
          <summary>
            {costosSinReferencia.length} productos con una sola compra · ver costos iniciales
          </summary>
          <p className="prov-aclaracion">
            Con una segunda compra del mismo producto y proveedor vas a poder comparar.
          </p>
          {costosSinReferencia.map((c) => (
            <div className="prov-gasto" key={c.producto.id}>
              <div>
                <strong>{c.producto.nombre}</strong>
                <p>
                  {c.producto.presentacion} · {fechaCorta(c.compra.fecha)}
                </p>
              </div>
              <strong>{moneda(c.linea.costo)}</strong>
            </div>
          ))}
        </details>
      )}
    </PanelProveedor>
  );
}
