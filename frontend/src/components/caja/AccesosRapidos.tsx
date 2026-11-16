import { useEffect, useId, useRef, type KeyboardEvent, type FocusEvent } from 'react';
import { useAccesosRapidos } from '@/hooks/caja/useAccesosRapidos';

export function AccesosRapidos() {
  const accesos = useAccesosRapidos();
  const id = useId();
  const boton = useRef<HTMLButtonElement>(null);
  const panel = useRef<HTMLElement>(null);
  useEffect(() => {
    if (accesos.abierto) panel.current?.querySelector<HTMLButtonElement>('button')?.focus();
  }, [accesos.abierto]);
  const cerrarAlSalir = (evento: FocusEvent) => {
    if (!evento.currentTarget.contains(evento.relatedTarget)) accesos.cerrar();
  };
  const cerrarConEscape = (evento: KeyboardEvent) => {
    if (evento.key !== 'Escape') return;
    accesos.cerrar();
    boton.current?.focus();
  };
  if (accesos.oculto) return null;
  return (
    <aside className="acciones-rapidas" aria-label="Accesos rápidos" onKeyDown={cerrarConEscape} onBlur={cerrarAlSalir}>
      {accesos.abierto && (
        <nav ref={panel} id={id} aria-label="Acciones de venta e inventario" className="acciones-rapidas-panel">
          <button type="button" onClick={accesos.vender}>Nueva venta</button>
          <InventariosRapidos accesos={accesos} />
          <button type="button" onClick={() => { accesos.cerrar(); boton.current?.focus(); }}>Cerrar accesos</button>
        </nav>
      )}
      <button ref={boton} type="button" className="acciones-rapidas-disparador"
        aria-expanded={accesos.abierto} aria-controls={accesos.abierto ? id : undefined}
        onClick={() => accesos.cambiarAbierto(!accesos.abierto)}>Accesos rápidos</button>
    </aside>
  );
}

function InventariosRapidos({ accesos }: { accesos: ReturnType<typeof useAccesosRapidos> }) {
  if (!accesos.inventarios.length) return <p>Este negocio todavía no tiene inventario.</p>;
  return <div className="acciones-rapidas-inventarios">{accesos.inventarios.map((inventario) =>
    <button key={inventario.id} type="button" onClick={() => accesos.escanear(inventario.id)}>
      Escanear {inventario.name}
    </button>
  )}</div>;
}
