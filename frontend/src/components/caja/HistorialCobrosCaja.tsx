import { dineroCaja, fechaCaja } from '@/lib/caja';
import type { CobroDeudaCaja, VentaCaja } from '@/types/caja';

const nombreMedio = (medio: string) =>
  medio === 'EFECTIVO' ? 'Efectivo' : medio === 'TARJETA' ? 'Tarjeta' : 'Transferencia';

export function HistorialCobrosCaja({
  ventas,
  cobros,
  ventaId,
}: {
  ventas: VentaCaja[];
  cobros: CobroDeudaCaja[];
  ventaId?: string;
}) {
  const iniciales = ventas
    .filter((v) => !ventaId || v.id === ventaId)
    .flatMap((venta) => {
      const pagos = venta.pagosIniciales ?? venta.pagos;
      return pagos.length
        ? [
            {
              id: `venta-${venta.id}`,
              fecha: venta.creadoEn,
              titulo: `Cobro inicial · ${venta.clienteNombre ?? 'Venta sin cliente'}`,
              importe: pagos.reduce((sum, p) => sum + p.importe, 0),
              pagos,
              referencia: `Venta ${venta.id.slice(0, 8)}`,
            },
          ]
        : [];
    });
  const posteriores = cobros.flatMap((cobro) => {
    const aplicaciones = cobro.aplicaciones.filter((a) => !ventaId || a.ventaId === ventaId);
    if (!aplicaciones.length) return [];
    return [
      {
        id: cobro.id,
        fecha: cobro.creadoEn,
        titulo: `Pago de deuda · ${cobro.clienteNombre}`,
        importe: aplicaciones.reduce((sum, a) => sum + a.importe, 0),
        pagos: aplicaciones.flatMap((a) => a.pagos),
        referencia: aplicaciones.map((a) => `Venta ${a.ventaId.slice(0, 8)}`).join(' · '),
      },
    ];
  });
  const movimientos = [...iniciales, ...posteriores].sort(
    (a, b) => Date.parse(b.fecha) - Date.parse(a.fecha),
  );
  return (
    <section aria-label="Historial de cobros">
      <h3>Cobros registrados</h3>
      {!movimientos.length && <p className="caja-vacio">Todavía no se registraron cobros.</p>}
      <ul className="caja-clientes-lista caja-cobros-lista">
        {movimientos.map((m) => (
          <li key={m.id}>
            <div>
              <strong>{m.titulo}</strong>
              <span>{fechaCaja(m.fecha)}</span>
              <small>{m.referencia}</small>
              <span>
                {m.pagos
                  .map((p) => `${nombreMedio(p.medio)} ${dineroCaja(Math.round(p.importe * 100))}`)
                  .join(' · ')}
              </span>
            </div>
            <strong>{dineroCaja(Math.round(m.importe * 100))}</strong>
          </li>
        ))}
      </ul>
    </section>
  );
}
