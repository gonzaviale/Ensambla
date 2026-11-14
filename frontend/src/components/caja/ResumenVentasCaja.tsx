import { useState } from 'react';
import { dineroCaja, fechaCaja } from '@/lib/caja';
import { Icon } from '@/components/sistema/Icon';
import type { MedioCaja, ResumenVentasCaja as Resumen } from '@/types/caja';

const medios = [
  { id: 'EFECTIVO', nombre: 'Efectivo', clave: 'efectivo' },
  { id: 'TRANSFERENCIA', nombre: 'Transferencia', clave: 'transferencia' },
  { id: 'TARJETA', nombre: 'Tarjeta', clave: 'tarjeta' },
] as const;

export function ResumenVentasCaja({ resumen, titulo }: { resumen: Resumen; titulo: string }) {
  const [filtro, setFiltro] = useState<MedioCaja | null>(null);
  const ventas = resumen.ventas.filter((v) => !filtro || v.pagos.some((p) => p.medio === filtro));
  return (
    <section className="caja-resumen-ventas" aria-label={titulo}>
      <div className="caja-total-resumen">
        <span className="caja-antetitulo">{titulo}</span>
        <strong>{dineroCaja(Math.round(resumen.total * 100))}</strong>
        <span>
          {resumen.ventas.length}{' '}
          {resumen.ventas.length === 1 ? 'venta registrada' : 'ventas registradas'}
        </span>
      </div>
      <div className="caja-medios-resumen">
        {medios.map((m) => (
          <button
            type="button"
            key={m.id}
            aria-pressed={filtro === m.id}
            onClick={() => setFiltro(filtro === m.id ? null : m.id)}
          >
            <span className="caja-medio-nombre">
              <Icon
                name={
                  m.id === 'EFECTIVO' ? 'wallet' : m.id === 'TRANSFERENCIA' ? 'arrow' : 'orders'
                }
              />
              {m.nombre}
            </span>
            <strong>{dineroCaja(Math.round(resumen[m.clave] * 100))}</strong>
            <small>
              {filtro === m.id ? 'Filtro activo' : 'Ver ventas'} <span aria-hidden="true">→</span>
            </small>
          </button>
        ))}
      </div>
      <div className="caja-entre">
        <h3>
          {filtro
            ? `Ventas con ${medios.find((m) => m.id === filtro)?.nombre.toLowerCase()}`
            : 'Detalle de ventas'}
        </h3>
        {filtro && (
          <button type="button" className="btn secondary small" onClick={() => setFiltro(null)}>
            Ver todas
          </button>
        )}
      </div>
      {filtro && (
        <p className="caja-ayuda">
          Las ventas mixtas aparecen en cada medio utilizado. El total de arriba cuenta cada venta
          una sola vez.
        </p>
      )}
      {!ventas.length && <p className="caja-vacio">Todavía no hay ventas para mostrar.</p>}
      <div className="caja-ventas-detalle">
        {ventas.map((v) => (
          <details key={v.id}>
            <summary>
              <span>
                {fechaCaja(v.creadoEn)}
                <small>
                  {v.medio === 'MIXTO'
                    ? 'Pago mixto'
                    : v.medio === 'EFECTIVO'
                      ? 'Efectivo'
                      : v.medio === 'TARJETA'
                        ? 'Tarjeta'
                        : 'Transferencia'}
                </small>
              </span>
              <strong>{dineroCaja(Math.round(v.total * 100))}</strong>
              <span aria-hidden="true">⌄</span>
            </summary>
            <ul>
              {v.lineas.map((l, i) => (
                <li key={i}>
                  <span>
                    {l.nombre} ·{' '}
                    {l.modo === 'PESO' ? `${Math.round(l.cantidad * 1000)} g` : `${l.cantidad} u.`}
                  </span>
                  <strong>{dineroCaja(Math.round(l.importe * 100))}</strong>
                </li>
              ))}
            </ul>
            <div className="caja-pagos-detalle">
              {v.pagos.map((p) => (
                <p key={p.medio}>
                  <span>{medios.find((m) => m.id === p.medio)?.nombre}</span>
                  <strong>{dineroCaja(Math.round(p.importe * 100))}</strong>
                </p>
              ))}
            </div>
            <p className="caja-ayuda">
              Vuelto: {dineroCaja(Math.round(v.vuelto * 100))} · Operación {v.id.slice(0, 8)}
            </p>
          </details>
        ))}
      </div>
    </section>
  );
}
