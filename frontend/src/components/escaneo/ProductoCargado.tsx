import type { Fila } from '@/lib/sistema/tipos';
import { Icon } from '@/components/sistema/Icon';

export function ProductoCargado({
  producto,
  pendiente,
  inventario,
  agregadas,
}: {
  producto: Fila;
  pendiente: boolean;
  inventario: string;
  agregadas?: number | null;
}) {
  return (
    <section
      className={`escaneo-ultimo ${pendiente ? 'escaneo-ultimo-pendiente' : ''}`}
      aria-label="Último producto guardado"
    >
      <span className="escaneo-ultimo-icono">
        <Icon name={pendiente ? 'clock' : 'check-circle'} />
      </span>
      <div className="escaneo-ultimo-detalle" role="status">
        <span className="escaneo-ultimo-etiqueta">
          {pendiente ? 'Guardando los cambios' : 'Carga confirmada'}
        </span>
        <h2>
          {pendiente
            ? 'Guardando producto…'
            : agregadas != null
              ? 'Stock actualizado'
              : 'Producto cargado al inventario'}
        </h2>
        <p>{String(producto.name)}</p>
        <small>
          {pendiente
            ? 'Esperá la confirmación del guardado.'
            : agregadas != null
              ? `Agregaste ${agregadas.toLocaleString('es-AR')} unidades. Ya podés escanear el siguiente producto.`
              : 'Ya podés escanear o ingresar el siguiente código.'}
        </small>
      </div>
      <div className="escaneo-ultimo-cantidad">
        <strong>{String(producto.quantity ?? '—')}</strong>
        <span>unidades en {inventario}</span>
      </div>
    </section>
  );
}
