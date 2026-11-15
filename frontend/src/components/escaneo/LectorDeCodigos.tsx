import { Icon } from '@/components/sistema/Icon';
import { guiaDelLector } from '@/lib/escaneo/presentacionLector';
import type { LectorInventario } from '@/types/lectorInventario';
import { VisorLectorInventario } from './VisorLectorInventario';
import { AccionesLectorInventario } from './AccionesLectorInventario';
import { PanelLectorInventario } from './PanelLectorInventario';

export function LectorDeCodigos(lector: LectorInventario) {
  const guia = guiaDelLector(lector);
  return (
    <section
      className="escaneo-panel escaneo-lector escaneo-lector-inventario"
      aria-label="Lector de códigos"
    >
      <div className="escaneo-lector-titulo">
        <span className="escaneo-icono">
          <Icon name="camera" />
        </span>
        <div>
          <h2>Escaneá el producto</h2>
          <p>Después revisás los datos y confirmás la carga.</p>
        </div>
      </div>
      <VisorLectorInventario lector={lector} />
      <p className="escaneo-guia-codigo" role={lector.errorCamara ? 'alert' : 'status'}>
        {guia}
      </p>
      <AccionesLectorInventario lector={lector} />
      <p className="escaneo-lector-nota">
        <Icon name="box" />
        Leer el código no cambia el stock.
      </p>
      <PanelLectorInventario lector={lector} />
    </section>
  );
}
