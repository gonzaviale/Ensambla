import { dineroCaja } from '@/lib/caja';
import { Icon } from '@/components/sistema/Icon';
import type { LineaCaja } from '@/types/caja';

export function CompraActual({
  lineas,
  total,
  bloqueada,
  pendiente,
  onAjustar,
  onQuitar,
  onCobrar,
  onDeshacer,
  puedeDeshacer,
  onAgregar,
}: {
  lineas: LineaCaja[];
  total: number;
  bloqueada: boolean;
  pendiente: boolean;
  onAjustar: (id: string, cantidad: number) => void;
  onQuitar: (id: string) => void;
  onCobrar: () => void;
  onDeshacer: () => void;
  puedeDeshacer: boolean;
  onAgregar: () => void;
}) {
  return (
    <aside className="caja-compra" aria-label="Compra actual">
      <div className="caja-entre caja-compra-cabecera">
        <div>
          <span className="caja-antetitulo">02 · REVISAR COMPRA</span>
          <h2>Compra actual</h2>
        </div>
        <span className="caja-contador">{lineas.length}</span>
        <button
          type="button"
          className="btn secondary small caja-seguir-agregando"
          aria-label="Seguir agregando"
          disabled={bloqueada}
          onClick={onAgregar}
        >
          <Icon name="plus" />
          Agregar
        </button>
      </div>
      {!lineas.length && (
        <div className="caja-vacio">
          <Icon name="orders" />
          <h3>Todavía no hay productos en esta venta</h3>
          <p>Agregá productos para comenzar.</p>
        </div>
      )}
      <ul className="caja-lista">
        {lineas.map((linea) => (
          <li key={linea.id}>
            <div className="caja-entre">
              <strong>{linea.nombre}</strong>
              <strong>{dineroCaja(linea.importeCentavos)}</strong>
            </div>
            <p className="caja-ayuda caja-precio-unitario">
              {linea.productoId === null ? (
                'Importe libre'
              ) : (
                <>
                  {dineroCaja(Math.round(linea.precio * 100))} /{' '}
                  {linea.modo === 'peso' ? 'kg' : 'unidad'}
                </>
              )}
            </p>
            <div className="caja-entre">
              {linea.productoId !== null && (
                <div className="caja-cantidad">
                  <button
                    type="button"
                    aria-label={`Restar ${linea.modo === 'peso' ? '100 gramos de' : 'una unidad de'} ${linea.nombre}`}
                    disabled={bloqueada}
                    onClick={() =>
                      onAjustar(
                        linea.id,
                        Math.round((linea.cantidad - (linea.modo === 'peso' ? 0.1 : 1)) * 1000) /
                          1000,
                      )
                    }
                  >
                    −
                  </button>
                  <label>
                    <span className="sr-only">
                      Cantidad de {linea.nombre}
                      {linea.modo === 'peso' ? ' en gramos' : ''}
                    </span>
                    <input
                      key={`${linea.id}:${linea.cantidad}`}
                      aria-label={`Cantidad de ${linea.nombre}${linea.modo === 'peso' ? ' en gramos' : ''}`}
                      inputMode="numeric"
                      disabled={bloqueada}
                      defaultValue={
                        linea.modo === 'peso' ? Math.round(linea.cantidad * 1000) : linea.cantidad
                      }
                      onBlur={(e) => {
                        const valor = e.target.value.trim();
                        if (/^\d+$/.test(valor))
                          onAjustar(linea.id, Number(valor) / (linea.modo === 'peso' ? 1000 : 1));
                        else
                          e.target.value = String(
                            linea.modo === 'peso'
                              ? Math.round(linea.cantidad * 1000)
                              : linea.cantidad,
                          );
                      }}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') e.currentTarget.blur();
                      }}
                    />
                    {linea.modo === 'peso' && <span>g</span>}
                  </label>
                  <button
                    type="button"
                    aria-label={`Sumar ${linea.modo === 'peso' ? '100 gramos de' : 'una unidad de'} ${linea.nombre}`}
                    disabled={bloqueada}
                    onClick={() =>
                      onAjustar(
                        linea.id,
                        Math.round((linea.cantidad + (linea.modo === 'peso' ? 0.1 : 1)) * 1000) /
                          1000,
                      )
                    }
                  >
                    +
                  </button>
                </div>
              )}
              <button
                type="button"
                className="caja-quitar"
                aria-label={`Quitar ${linea.nombre}`}
                disabled={bloqueada}
                onClick={() => onQuitar(linea.id)}
              >
                <Icon name="trash" />
                <span className="caja-quitar-texto">Quitar</span>
              </button>
            </div>
            {linea.modo === 'peso' && (
              <small className="caja-ayuda">
                ±100 g · Tocá la cantidad para indicar el peso exacto.
              </small>
            )}
          </li>
        ))}
      </ul>
      {puedeDeshacer && (
        <button
          type="button"
          className="btn ghost caja-deshacer"
          disabled={bloqueada}
          onClick={onDeshacer}
        >
          <Icon name="undo" />
          Deshacer último cambio
        </button>
      )}
      <div className="caja-cierre">
        <div className="caja-entre">
          <span>Total a cobrar</span>
          <strong>{dineroCaja(total)}</strong>
        </div>
        <button
          type="button"
          className="btn primary"
          disabled={!lineas.length || bloqueada || pendiente}
          onClick={onCobrar}
        >
          Cobrar {dineroCaja(total)}
        </button>
        {pendiente && (
          <p role="status" className="caja-ayuda">
            Guardando los cambios del stock…
          </p>
        )}
      </div>
    </aside>
  );
}
