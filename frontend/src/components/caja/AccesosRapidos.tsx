import {
  useEffect,
  useId,
  useRef,
  type FocusEvent,
  type KeyboardEvent,
  type ReactNode,
  type RefObject,
} from 'react';
import { useAccesosRapidos } from '@/hooks/caja/useAccesosRapidos';
import { Icon } from '@/components/sistema/Icon';
import type { NombreDeIcono } from '@/lib/sistema/iconos';
import { tonoDe, type ConceptoDeColor } from '@/lib/sistema/tonos';

type Accesos = ReturnType<typeof useAccesosRapidos>;

export function AccesosRapidos() {
  const accesos = useAccesosRapidos();
  const id = useId();
  const disparador = useRef<HTMLButtonElement>(null);
  const cerrarYVolverAlDisparador = () => {
    accesos.cerrar();
    disparador.current?.focus();
  };
  if (accesos.oculto) return null;
  return (
    <aside
      className="acciones-rapidas"
      aria-label="Accesos rápidos"
      onKeyDown={cerrarConEscape(cerrarYVolverAlDisparador)}
      onBlur={cerrarAlSalir(accesos.cerrar)}
    >
      {accesos.abierto && (
        <PanelDeAccesos id={id} accesos={accesos} alCerrar={cerrarYVolverAlDisparador} />
      )}
      <DisparadorDeAccesos disparador={disparador} panel={id} accesos={accesos} />
    </aside>
  );
}

function cerrarConEscape(cerrar: () => void) {
  return (evento: KeyboardEvent) => {
    if (evento.key === 'Escape') cerrar();
  };
}

function cerrarAlSalir(cerrar: () => void) {
  return (evento: FocusEvent) => {
    if (!evento.currentTarget.contains(evento.relatedTarget)) cerrar();
  };
}

function PanelDeAccesos({
  id,
  accesos,
  alCerrar,
}: {
  id: string;
  accesos: Accesos;
  alCerrar: () => void;
}) {
  const opciones = useEnfocarLaPrimeraOpcion();
  return (
    <div id={id} className="acciones-rapidas-panel">
      <nav
        ref={opciones}
        aria-label="Acciones de venta y stock"
        className="acciones-rapidas-opciones"
      >
        <OpcionDeAcceso concepto="accesoNuevaVenta" icono="check-circle" alElegir={accesos.vender}>
          Nueva venta
        </OpcionDeAcceso>
        <InventariosRapidos accesos={accesos} />
      </nav>
      <CruzDeCerrar alCerrar={alCerrar} />
    </div>
  );
}

function useEnfocarLaPrimeraOpcion() {
  const opciones = useRef<HTMLElement>(null);
  useEffect(() => {
    opciones.current?.querySelector<HTMLButtonElement>('button')?.focus();
  }, []);
  return opciones;
}

function CruzDeCerrar({ alCerrar }: { alCerrar: () => void }) {
  return (
    <button
      type="button"
      className="acciones-rapidas-cerrar solido"
      data-tono={tonoDe('cruzDeCerrar')}
      aria-label="Cerrar accesos rápidos"
      onClick={alCerrar}
    >
      <Icon name="close" />
    </button>
  );
}

function InventariosRapidos({ accesos }: { accesos: Accesos }) {
  if (!accesos.inventarios.length) return <p>Este negocio todavía no tiene stock cargado.</p>;
  return (
    <div className="acciones-rapidas-inventarios">
      {accesos.inventarios.map((inventario) => (
        <OpcionDeAcceso
          key={inventario.id}
          concepto="accesoEscanearStock"
          icono="camera"
          alElegir={() => accesos.escanear(inventario.id)}
        >
          Escanear {inventario.name}
        </OpcionDeAcceso>
      ))}
    </div>
  );
}

type PropsDeOpcion = {
  concepto: ConceptoDeColor;
  icono: NombreDeIcono;
  alElegir: () => void;
  children: ReactNode;
};

function OpcionDeAcceso({ concepto, icono, alElegir, children }: PropsDeOpcion) {
  return (
    <button type="button" className="solido" data-tono={tonoDe(concepto)} onClick={alElegir}>
      <Icon name={icono} />
      <span>{children}</span>
    </button>
  );
}

type PropsDelDisparador = {
  disparador: RefObject<HTMLButtonElement | null>;
  panel: string;
  accesos: Accesos;
};

function DisparadorDeAccesos({ disparador, panel, accesos }: PropsDelDisparador) {
  return (
    <button
      ref={disparador}
      type="button"
      className="acciones-rapidas-disparador solido"
      data-tono={tonoDe('botonDeAccesosRapidos')}
      aria-expanded={accesos.abierto}
      aria-controls={accesos.abierto ? panel : undefined}
      onClick={() => accesos.cambiarAbierto(!accesos.abierto)}
    >
      Accesos rápidos
    </button>
  );
}
