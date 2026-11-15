import { AntiguedadDeudaCaja } from './AntiguedadDeudaCaja';
import { useState } from 'react';
import { DialogoCaja } from './DialogoCaja';
import { CobroDeudaCaja } from './CobroDeudaCaja';
import { HistorialCobrosCaja } from './HistorialCobrosCaja';
import { dineroCaja, fechaCaja } from '@/lib/caja';
import { hayPagoDeudaPendiente } from '@/lib/cobrosDeudasCaja';
import { useSistemaStore } from '@/stores/sistemaStore';
import type { CobroDeudaCaja as CobroRegistrado, VentaCaja } from '@/types/caja';

export function CuentaClienteCaja({
  sistemaId,
  inventarioId,
  clienteId,
  nombre,
  ventas,
  cobros,
  onCerrar,
}: {
  sistemaId: string;
  inventarioId: string;
  clienteId: string;
  nombre: string;
  ventas: VentaCaja[];
  cobros: CobroRegistrado[];
  onCerrar: () => void;
}) {
  const [cobrando, setCobrando] = useState<string[] | null>(null);
  const usuarioId = useSistemaStore((s) => s.usuarioId);
  const incierto = hayPagoDeudaPendiente(usuarioId, sistemaId, inventarioId, clienteId);
  const pendientes = ventas.filter((v) => (v.saldo ?? 0) > 0);
  const saldo = pendientes.reduce((sum, v) => sum + Math.round((v.saldo ?? 0) * 100), 0);
  return (
    <>
      <DialogoCaja titulo={`Cuenta de ${nombre}`} onCerrar={onCerrar}>
        <div className="caja-total-resumen">
          <span>Saldo pendiente</span>
          <strong>{dineroCaja(saldo)}</strong>
          <span>{saldo ? `${pendientes.length} ventas pendientes` : 'Sin deuda'}</span>
        </div>
        {(!!saldo || incierto) && (
          <button
            type="button"
            className="btn primary full"
            onClick={() => setCobrando((incierto ? ventas : pendientes).map((v) => v.id))}
          >
            {incierto ? 'Verificar pago pendiente' : 'Registrar pago'}
          </button>
        )}
        <h3>Ventas del cliente</h3>
        {!ventas.length && (
          <p className="caja-vacio">Todavía no tiene ventas registradas en esta caja.</p>
        )}
        <ul className="caja-clientes-lista">
          {[...ventas]
            .sort((a, b) => Date.parse(b.creadoEn) - Date.parse(a.creadoEn))
            .map((v) => (
              <li key={v.id}>
                <div>
                  <strong>
                    Venta {v.id.slice(0, 8)} · {fechaCaja(v.creadoEn)}
                  </strong>
                  <AntiguedadDeudaCaja ventas={[v]} />
                  <span>Total: {dineroCaja(Math.round(v.total * 100))}</span>
                  <span>Cobrado: {dineroCaja(Math.round((v.cobrado ?? v.total) * 100))}</span>
                  <span>
                    {(v.saldo ?? 0) > 0
                      ? `Pendiente: ${dineroCaja(Math.round((v.saldo ?? 0) * 100))}`
                      : 'Pagada'}
                  </span>
                </div>
                {(v.saldo ?? 0) > 0 && (
                  <button
                    type="button"
                    className="btn secondary"
                    aria-label={`Registrar pago de la venta ${v.id.slice(0, 8)}`}
                    onClick={() => setCobrando([v.id])}
                  >
                    Registrar pago
                  </button>
                )}
              </li>
            ))}
        </ul>
        <HistorialCobrosCaja ventas={ventas} cobros={cobros} />
      </DialogoCaja>
      {cobrando && (
        <CobroDeudaCaja
          sistemaId={sistemaId}
          inventarioId={inventarioId}
          clienteId={clienteId}
          clienteNombre={nombre}
          ventas={ventas.filter((v) => cobrando.includes(v.id))}
          onCerrar={() => setCobrando(null)}
        />
      )}
    </>
  );
}
