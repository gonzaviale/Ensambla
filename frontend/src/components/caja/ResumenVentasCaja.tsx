import { AntiguedadDeudaCaja } from './AntiguedadDeudaCaja';
import { detalleLineaVendida } from '@/lib/caja/importeLibre';
import { useState } from 'react';
import { dineroCaja, fechaCaja } from '@/lib/caja';
import { Icon } from '@/components/sistema/Icon';
import type { MedioCaja, ResumenVentasCaja as Resumen } from '@/types/caja';
import type { VentaCaja } from '@/types/caja';
import { claseDeSeleccion, tonoDe } from '@/lib/sistema/tonos';

const medios = [
  { id: 'EFECTIVO', nombre: 'Efectivo', clave: 'efectivo', icono: 'wallet' },
  { id: 'TRANSFERENCIA', nombre: 'Transferencia', clave: 'transferencia', icono: 'arrow' },
  { id: 'TARJETA', nombre: 'Tarjeta', clave: 'tarjeta', icono: 'orders' },
] as const;

const FILTROS_DE_ESTADO = [
  ['todas', 'Todas', 'ventas'],
  ['pendientes', 'Pendientes', 'fiado'],
  ['pagadas', 'Pagadas', 'cobrado'],
] as const;

export function ResumenVentasCaja({
  resumen,
  titulo,
  onVerCuenta,
}: {
  resumen: Resumen;
  titulo: string;
  onVerCuenta?: (venta: VentaCaja) => void;
}) {
  const [filtro, setFiltro] = useState<MedioCaja | null>(null);
  const [estado, setEstado] = useState<'todas' | 'pendientes' | 'pagadas'>('todas');
  const ventas = resumen.ventas.filter(
    (v) =>
      (!filtro || v.pagos.some((p) => p.medio === filtro)) &&
      (estado === 'todas' || (estado === 'pendientes' ? (v.saldo ?? 0) > 0 : (v.saldo ?? 0) === 0)),
  );
  return (
    <section className="caja-resumen-ventas" aria-label={titulo}>
      <div className="caja-total-resumen solido" data-tono={tonoDe('ventas')}>
        <span className="caja-antetitulo">{titulo}</span>
        <strong className="solido-cifra">{dineroCaja(Math.round(resumen.total * 100))}</strong>
        <span>
          {resumen.ventas.length}{' '}
          {resumen.ventas.length === 1 ? 'venta guardada' : 'ventas guardadas'}
        </span>
        <span>
          Cobrado en este período:{' '}
          {dineroCaja(
            Math.round((resumen.efectivo + resumen.transferencia + resumen.tarjeta) * 100),
          )}
        </span>
      </div>
      <div className="caja-medios-resumen">
        {medios.map((m) => (
          <button
            type="button"
            key={m.id}
            className="solido"
            data-tono={tonoDe(m.clave)}
            aria-pressed={filtro === m.id}
            onClick={() => setFiltro(filtro === m.id ? null : m.id)}
          >
            <span className="caja-medio-nombre">
              <span className="solido-icono">
                <Icon name={m.icono} />
              </span>
              {m.nombre}
            </span>
            <strong className="solido-cifra">
              {dineroCaja(Math.round(resumen[m.clave] * 100))}
            </strong>
            <small className="solido-enlace">
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
      <div className="caja-opciones" aria-label="Estado de las ventas">
        {FILTROS_DE_ESTADO.map(([id, texto, concepto]) => (
          <button
            key={id}
            type="button"
            className={claseDeSeleccion(estado === id)}
            data-tono={tonoDe(concepto)}
            aria-pressed={estado === id}
            onClick={() => setEstado(id)}
          >
            {texto}
          </button>
        ))}
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
                {onVerCuenta && <AntiguedadDeudaCaja ventas={[v]} />}
                <small>
                  {v.clienteNombre && `${v.clienteNombre} · `}
                  {(v.saldo ?? 0) > 0
                    ? v.estado === 'PARCIAL'
                      ? 'Pendiente de pago · Pago parcial'
                      : 'Pendiente de pago'
                    : 'Pagada'}
                  {' · '}
                  {!v.pagos.length
                    ? 'Sin cobros'
                    : v.medio === 'MIXTO'
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
                    {l.nombre} · {detalleLineaVendida(l)}
                  </span>
                  <strong>{dineroCaja(Math.round(l.importe * 100))}</strong>
                </li>
              ))}
            </ul>
            <div className="caja-pagos-detalle">
              <p>
                Cobrado acumulado:{' '}
                <strong>{dineroCaja(Math.round((v.cobrado ?? v.total) * 100))}</strong>
              </p>
              <p>
                Pendiente: <strong>{dineroCaja(Math.round((v.saldo ?? 0) * 100))}</strong>
              </p>
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
            {onVerCuenta && (
              <button type="button" className="btn secondary" onClick={() => onVerCuenta(v)}>
                Ver pagos{(v.saldo ?? 0) > 0 ? ' y guardar cobro' : ''}
              </button>
            )}
          </details>
        ))}
      </div>
    </section>
  );
}
