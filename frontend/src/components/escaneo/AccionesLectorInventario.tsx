import { Icon } from '@/components/sistema/Icon';
import { usePanelLector } from '@/hooks/escaneo/usePanelLector';
import { presentacionDelLector } from '@/lib/escaneo/presentacionLector';
import type { LectorInventario } from '@/types/lectorInventario';

export function AccionesLectorInventario({ lector }: { lector: LectorInventario }) {
  const panel = usePanelLector(lector);
  const accion = presentacionDelLector(lector);
  const cambiarCamara = lector.abierta ? lector.onCerrar : lector.onAbrir;
  return (
    <div className="escaneo-lector-acciones">
      <button
        type="button"
        className={lector.abierta ? 'btn secondary' : 'btn primary'}
        disabled={lector.buscando || lector.iniciando}
        aria-label={accion.etiqueta}
        onClick={cambiarCamara}
      >
        <Icon name="camera" />
        {accion.texto}
      </button>
      <button
        type="button"
        className="escaneo-accion-secundaria"
        disabled={lector.buscando}
        aria-label="Ingresar código · lector USB"
        title="Ingresar código o usar lector USB"
        onClick={panel.abrirCodigo}
      >
        <span aria-hidden="true">123</span>
      </button>
      <button
        type="button"
        className="escaneo-accion-secundaria"
        aria-label="Ajustes del lector"
        onClick={panel.abrirAjustes}
      >
        <Icon name="settings" />
      </button>
    </div>
  );
}
