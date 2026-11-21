import { useState } from 'react';
import { DialogoCaja } from '@/components/caja/DialogoCaja';

export function DialogoDePrueba({ bloqueado = false }: { bloqueado?: boolean }) {
  const [abierto, abrir] = useState(false);
  return (
    <>
      <button onClick={() => abrir(true)}>Abrir cobro</button>
      {abierto && (
        <DialogoCaja titulo="Cobro" onCerrar={() => abrir(false)} bloqueado={bloqueado}>
          <label>
            Importe
            <input />
          </label>
          <label>
            Nota deshabilitada
            <textarea disabled />
          </label>
          <label>
            Medio
            <select>
              <option>Efectivo</option>
              <option>Tarjeta</option>
            </select>
          </label>
        </DialogoCaja>
      )}
    </>
  );
}
