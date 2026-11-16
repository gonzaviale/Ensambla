import type { FormEvent } from 'react';
import { Icon } from '@/components/sistema/Icon';
import { usePanelLector } from '@/hooks/escaneo/usePanelLector';
import type { LectorInventario } from '@/types/lectorInventario';

export function CodigoManualInventario({ lector }: { lector: LectorInventario }) {
  const { cerrarPanel } = usePanelLector(lector);
  const buscarCodigo = (evento: FormEvent) => {
    evento.preventDefault();
    lector.onBuscar();
  };
  return (
    <form className="escaneo-codigo" onSubmit={buscarCodigo}>
      <p>Escribí el código o usá un lector USB/Bluetooth y presioná Enter.</p>
      <label htmlFor="escaneo-codigo">Código de barras</label>
      <input
        ref={lector.entradaCodigo}
        id="escaneo-codigo"
        inputMode="numeric"
        autoComplete="off"
        placeholder="EAN / UPC"
        value={lector.codigo}
        onChange={(e) => lector.onCodigo(e.target.value)}
        disabled={lector.buscando}
      />
      {lector.mensaje && <p role="status">{lector.mensaje}</p>}
      <div className="escaneo-dialogo-acciones">
        <button type="button" className="btn secondary" onClick={cerrarPanel}>
          Volver al lector
        </button>
        <button
          type="submit"
          className="btn primary"
          disabled={lector.buscando || !lector.codigo.trim()}
        >
          <Icon name="search" />
          {lector.buscando ? 'Buscando…' : 'Buscar'}
        </button>
      </div>
    </form>
  );
}
