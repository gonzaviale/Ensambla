import { useState } from 'react';
import { DialogoCaja } from './DialogoCaja';
import { ConfirmacionCambios } from './ConfirmacionCambios';
import { useConfirmarCambios } from '@/hooks/useConfirmarCambios';

export function ImporteLibreCaja({
  onAgregar,
  onCerrar,
}: {
  onAgregar: (descripcion: string, importe: string) => void;
  onCerrar: () => void;
}) {
  const [descripcion, setDescripcion] = useState('');
  const [importe, setImporte] = useState('');
  const [error, setError] = useState('');
  const cambios = useConfirmarCambios();
  const solicitarCierre = async () => {
    if (
      !(descripcion || importe) ||
      (await cambios.confirmar('El importe todavía no se agregó a la compra.'))
    )
      onCerrar();
  };
  const agregar = (evento: React.FormEvent) => {
    evento.preventDefault();
    try {
      onAgregar(descripcion, importe);
      onCerrar();
    } catch (causa) {
      setError(causa instanceof Error ? causa.message : 'Revisá el importe.');
    }
  };
  return (
    <>
      <DialogoCaja titulo="Agregar importe" onCerrar={() => void solicitarCierre()}>
        <form onSubmit={agregar}>
          <p className="caja-ayuda">Sumá un servicio u otro importe a esta venta.</p>
          <label>
            Descripción
            <input
              autoFocus
              required
              maxLength={160}
              value={descripcion}
              onChange={(evento) => setDescripcion(evento.target.value)}
            />
          </label>
          <label>
            Importe ($)
            <input
              required
              inputMode="decimal"
              value={importe}
              onChange={(evento) => setImporte(evento.target.value)}
            />
          </label>
          {error && (
            <p className="caja-error" role="alert">
              {error}
            </p>
          )}
          <div className="caja-acciones">
            <button type="button" className="btn secondary" onClick={() => void solicitarCierre()}>
              Volver
            </button>
            <button type="submit" className="btn primary">
              Agregar a la compra
            </button>
          </div>
        </form>
      </DialogoCaja>
      {cambios.confirmacion && (
        <ConfirmacionCambios
          mensaje={cambios.confirmacion}
          onConservar={cambios.conservar}
          onDescartar={cambios.aceptar}
        />
      )}
    </>
  );
}
