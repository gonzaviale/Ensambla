import { Btn } from '@/components/sistema/Btn';
import { Icon } from '@/components/sistema/Icon';
import { moneda, fechaCorta } from '@/lib/proveedores/calculos';
import type { ProveedoresDemo } from '@/hooks/proveedores/useProveedoresDemo';
import type { GastoProveedor } from '@/types/proveedores';
import { PanelProveedor } from './PanelProveedor';

type Props = Pick<
  ProveedoresDemo,
  | 'mes'
  | 'setMes'
  | 'resumen'
  | 'gastos'
  | 'gastoNuevo'
  | 'setGastoNuevo'
  | 'guardarGasto'
  | 'categoriasGasto'
>;
export function GastosProveedor(p: Props) {
  return (
    <>
      <div className="prov-periodo">
        <div>
          <h2>El negocio completo, en números</h2>
          <p>Todos los proveedores · ventas y gastos de prueba del período.</p>
        </div>
        <label className="prov-campo">
          Mes del resumen
          <input type="month" value={p.mes} onChange={(e) => p.setMes(e.target.value)} />
        </label>
      </div>
      <div className="prov-indicadores">
        <article>
          <span>Ventas registradas</span>
          <strong>{moneda(p.resumen.ingresos)}</strong>
          <small>Importe vendido, incluido fiado</small>
        </article>
        <article>
          <span>Margen bruto</span>
          <strong>{moneda(p.resumen.margen)}</strong>
          <small>Ventas − costo de unidades vendidas</small>
        </article>
        <article>
          <span>Gastos generales</span>
          <strong>{moneda(p.resumen.operativos)}</strong>
          <small>Alquiler, servicios y otros</small>
        </article>
        <article className="prov-indicador-resultado">
          <span>Resultado operativo estimado</span>
          <strong>{moneda(p.resumen.resultado)}</strong>
          <small>Margen bruto − gastos generales</small>
        </article>
      </div>
      <div className="prov-nota">
        <strong>Comprar mercadería y consumirla son momentos distintos.</strong> Compras del mes:{' '}
        {moneda(p.resumen.compras)}. Se registran como desembolso de mercadería y no se vuelven a
        restar del margen bruto. El costo de lo vendido ({moneda(p.resumen.costoVendido)}) se
        conserva al registrar cada venta de ejemplo. Este resultado no representa saldo de caja ni
        ganancia neta: sólo contempla los registros de la demo.
      </div>
      <div className="prov-dos-columnas">
        <PanelProveedor
          titulo="Registrar un gasto general"
          detalle="Las mismas categorías del bloque Gastos de tus cuadernos."
          icono="wallet"
        >
          <form
            className="prov-form"
            onSubmit={(e) => {
              e.preventDefault();
              p.guardarGasto();
            }}
          >
            <label className="prov-campo">
              Concepto del gasto
              <input
                required
                maxLength={120}
                placeholder="Ej. Alquiler, luz, gas…"
                value={p.gastoNuevo.name}
                onChange={(e) => p.setGastoNuevo({ ...p.gastoNuevo, name: e.target.value })}
              />
            </label>
            <div className="prov-form-fila">
              <label className="prov-campo">
                Categoría del gasto
                <select
                  value={p.gastoNuevo.category}
                  onChange={(e) =>
                    p.setGastoNuevo({
                      ...p.gastoNuevo,
                      category: e.target.value as GastoProveedor['category'],
                    })
                  }
                >
                  {p.categoriasGasto.map((c) => (
                    <option key={c}>{c}</option>
                  ))}
                </select>
              </label>
              <label className="prov-campo">
                Fecha del gasto
                <input
                  type="date"
                  required
                  value={p.gastoNuevo.date}
                  onChange={(e) => p.setGastoNuevo({ ...p.gastoNuevo, date: e.target.value })}
                />
              </label>
            </div>
            <label className="prov-campo">
              Importe del gasto ($)
              <input
                type="number"
                required
                min="0.01"
                max="1000000000"
                step="0.01"
                value={p.gastoNuevo.total || ''}
                onChange={(e) =>
                  p.setGastoNuevo({ ...p.gastoNuevo, total: Number(e.target.value) })
                }
              />
            </label>
            <Btn type="submit" cls="primary">
              <Icon name="plus" /> Agregar gasto de prueba
            </Btn>
          </form>
        </PanelProveedor>
        <PanelProveedor
          titulo="Gastos y compras del mes"
          detalle="Mercadería queda separada de los gastos generales en el cálculo."
          icono="note"
        >
          {p.gastos.length === 0 && (
            <p className="prov-vacio">No hay gastos registrados en este mes.</p>
          )}
          <div className="prov-lista">
            {p.gastos.map((g) => (
              <article className="prov-gasto" key={g.id}>
                <div>
                  <strong>{g.name}</strong>
                  <p>
                    {g.category} · {fechaCorta(g.date)}
                  </p>
                  {g.compraId && <small>Vinculado a una compra confirmada</small>}
                </div>
                <strong>{moneda(g.total)}</strong>
              </article>
            ))}
          </div>
        </PanelProveedor>
      </div>
    </>
  );
}
