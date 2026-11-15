import { Icon } from '@/components/sistema/Icon';
import { presentacionDelLector } from '@/lib/escaneo/presentacionLector';
import type { LectorInventario } from '@/types/lectorInventario';

export function VisorLectorInventario({ lector }: { lector: LectorInventario }) {
  const { estadoVisible } = presentacionDelLector(lector);
  const clase = lector.abierta ? 'escaneo-visor escaneo-visor-activo' : 'escaneo-visor';
  return (
    <div className={clase}>
      {lector.abierta && (
        <video
          key={lector.sesion}
          ref={lector.video}
          muted
          playsInline
          autoPlay
          aria-label="Vista previa del lector"
        />
      )}
      <div className="escaneo-visor-estado" role="status">
        <Icon name="camera" />
        <span>{estadoVisible}</span>
      </div>
      <div className="escaneo-marco-codigo" aria-hidden="true">
        <span className="escaneo-barras-ejemplo" />
      </div>
    </div>
  );
}
