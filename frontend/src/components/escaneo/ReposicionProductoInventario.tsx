import type { RefObject } from 'react';
import { Icon } from '@/components/sistema/Icon';

export function ReposicionProductoInventario({
  stockActual,
  cantidad,
  error,
  formulario,
  onCantidad,
  onAgregar,
  onDescartar,
}: {
  stockActual: number;
  cantidad: string;
  error?: string;
  formulario: RefObject<HTMLFormElement | null>;
  onCantidad: (cantidad: string) => void;
  onAgregar: () => void;
  onDescartar: () => void;
}) {
  const unidades = Number(cantidad);
  const valida =
    cantidad.trim() !== '' &&
    Number.isFinite(unidades) &&
    unidades > 0 &&
    stockActual + unidades > stockActual &&
    stockActual + unidades <= Number.MAX_SAFE_INTEGER;
  const numero = (valor: number) => valor.toLocaleString('es-AR', { maximumFractionDigits: 20 });
  return (
    <section className="escaneo-panel escaneo-reposicion" aria-label="Sumar al stock">
      <span className="escaneo-reposicion-aviso">
        <Icon name="box" /> Este producto ya está en tu inventario
      </span>
      <h2>¿Querés sumar al stock?</h2>
      <p>Ingresá cuántas unidades querés agregar. Los demás datos del producto se conservan.</p>
      <form
        ref={formulario}
        noValidate
        onSubmit={(evento) => {
          evento.preventDefault();
          onAgregar();
        }}
      >
        <div className="escaneo-suma-stock">
          <div className="escaneo-stock-actual">
            <span>Stock actual</span>
            <strong>{Number.isFinite(stockActual) ? numero(stockActual) : '—'}</strong>
            <small>unidades</small>
          </div>
          <span className="escaneo-signo-stock" aria-hidden="true">
            +
          </span>
          <div className="escaneo-stock-entrada">
            <label htmlFor="escaneo-cantidad-agregar">Unidades a agregar</label>
            <input
              id="escaneo-cantidad-agregar"
              type="number"
              inputMode="decimal"
              min="0"
              step="any"
              placeholder="0"
              autoComplete="off"
              value={cantidad}
              onChange={(evento) => onCantidad(evento.target.value)}
              aria-invalid={!!error}
              aria-describedby={error ? 'escaneo-cantidad-agregar-error' : undefined}
            />
          </div>
          <span className="escaneo-signo-stock" aria-hidden="true">
            =
          </span>
          <div className="escaneo-stock-total">
            <span>Quedarán en stock</span>
            <output aria-live="polite">
              {valida && Number.isFinite(stockActual) ? numero(stockActual + unidades) : '—'}
            </output>
            <small>unidades</small>
          </div>
        </div>
        {error && (
          <p className="escaneo-error-cantidad" id="escaneo-cantidad-agregar-error" role="alert">
            {error}
          </p>
        )}
        <div className="escaneo-acciones-carga">
          <button type="submit" className="btn primary">
            <Icon name="plus" /> Agregar al stock
          </button>
          <button type="button" className="btn escaneo-descartar" onClick={onDescartar}>
            <Icon name="close" /> Descartar carga
          </button>
        </div>
      </form>
    </section>
  );
}
