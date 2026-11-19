import { useState } from 'react';
import { DialogoCaja } from './DialogoCaja';
import { HistorialCobrosCaja } from './HistorialCobrosCaja';
import { CobroDeudaCaja } from './CobroDeudaCaja';
import { useCuentasCaja } from '@/hooks/caja/useCuentasCaja';
import { dineroCaja } from '@/lib/caja';
import type { VentaCaja } from '@/types/caja';

export function CuentaVentaCaja({
  sistemaId,
  inventarioId,
  venta,
  onCerrar,
}: {
  sistemaId: string;
  inventarioId: string;
  venta: VentaCaja;
  onCerrar: () => void;
}) {
  const cuenta = useCuentasCaja(sistemaId, inventarioId);
  const [cobrando, setCobrando] = useState(false);
  const actual = cuenta.data?.ventas.find((v) => v.id === venta.id) ?? venta;
  return (
    <>
      <DialogoCaja titulo={`Pagos de la venta ${venta.id.slice(0, 8)}`} onCerrar={onCerrar}>
        <p>{actual.clienteNombre ?? 'Venta sin cliente'}</p>
        <p>
          Total: <strong>{dineroCaja(Math.round(actual.total * 100))}</strong>
        </p>
        <p>
          Cobrado: <strong>{dineroCaja(Math.round((actual.cobrado ?? actual.total) * 100))}</strong>
        </p>
        <p>
          Pendiente: <strong>{dineroCaja(Math.round((actual.saldo ?? 0) * 100))}</strong>
        </p>
        {cuenta.isPending && <p role="status">Cargando los pagos de esta venta…</p>}
        {cuenta.error && (
          <p role="alert" className="caja-error">
            No pudimos actualizar la cuenta.
            <button type="button" onClick={() => void cuenta.refetch()}>
              Reintentar
            </button>
          </p>
        )}
        {cuenta.data && (
          <>
            {(actual.saldo ?? 0) > 0 && actual.clienteId && (
              <button type="button" className="btn primary" onClick={() => setCobrando(true)}>
                Guardar pago
              </button>
            )}
            <HistorialCobrosCaja
              ventas={[actual]}
              cobros={cuenta.data.cobros}
              ventaId={actual.id}
            />
          </>
        )}
      </DialogoCaja>
      {cobrando && actual.clienteId && (
        <CobroDeudaCaja
          sistemaId={sistemaId}
          inventarioId={inventarioId}
          clienteId={actual.clienteId}
          clienteNombre={actual.clienteNombre ?? 'Cliente'}
          ventas={[actual]}
          onCerrar={() => setCobrando(false)}
        />
      )}
    </>
  );
}
