import { useState } from 'react';
import { DialogoCaja } from './DialogoCaja';
import { dineroCaja, leerPrecio } from '@/lib/caja';
import type { SolicitudCobro } from '@/types/caja';

export function CobroCaja({ total, ocupada, incierta, error, onConfirmar, onCerrar }: {
  total: number; ocupada: boolean; incierta: boolean; error: string;
  onConfirmar: (medio: SolicitudCobro['medio'], recibido: number | null) => void; onCerrar: () => void;
}) {
  const [medio, setMedio] = useState<SolicitudCobro['medio']>('EFECTIVO');
  const [recibido, setRecibido] = useState('');
  let importe = total, valido = true;
  try { if (recibido) importe = leerPrecio(recibido); } catch { valido = false; }
  if (medio === 'EFECTIVO' && importe < total) valido = false;
  if (medio !== 'EFECTIVO') valido = true;
  return <DialogoCaja titulo={incierta ? 'Verificar la venta' : 'Registrar el cobro'} onCerrar={onCerrar} bloqueado={ocupada || incierta}>
    <p className="caja-importe-previo">{dineroCaja(total)}</p>
    <p className="caja-ayuda">Registrá cómo te pagaron. El pago se realiza por tu medio habitual.</p>
    <div className="caja-opciones">{(['EFECTIVO','TRANSFERENCIA','TARJETA'] as const).map(valor => <button key={valor} type="button"
      disabled={ocupada || incierta} aria-pressed={medio === valor} onClick={() => setMedio(valor)}>{valor === 'EFECTIVO' ? 'Efectivo' : valor === 'TARJETA' ? 'Tarjeta' : 'Transferencia'}</button>)}</div>
    {medio === 'EFECTIVO' && <label>Recibido ($)<input inputMode="decimal" value={recibido} disabled={ocupada || incierta} placeholder="Importe exacto" onChange={e => setRecibido(e.target.value)} /></label>}
    <p role="status">{medio === 'EFECTIVO' ? valido ? `Vuelto: ${dineroCaja(importe - total)}` : 'El recibido debe alcanzar el total.' : 'Confirmá que ya recibiste el pago por este medio.'}</p>
    {error && <p className="caja-error" role="alert">{error}</p>}
    {incierta && <p className="caja-nota">Estamos verificando si se registró. Consultar de nuevo conserva la misma operación; no vuelvas a cobrarle a la persona.</p>}
    <div className="caja-acciones"><button type="button" className="btn secondary" disabled={ocupada || incierta} onClick={onCerrar}>Volver</button>
      <button type="button" className="btn primary" disabled={ocupada || (!incierta && !valido)}
        onClick={() => onConfirmar(medio, medio === 'EFECTIVO' && recibido ? importe / 100 : null)}>{ocupada ? 'Confirmando…' : incierta ? 'Verificar misma operación' : 'Confirmar cobro'}</button></div>
  </DialogoCaja>;
}
