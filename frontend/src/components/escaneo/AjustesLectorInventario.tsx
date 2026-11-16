import { Icon } from '@/components/sistema/Icon';
import { usePanelLector } from '@/hooks/escaneo/usePanelLector';
import type { LectorInventario } from '@/types/lectorInventario';
import { AtribucionCatalogos } from './AtribucionCatalogos';
import { SelectorCamaraInventario } from './SelectorCamaraInventario';

export function AjustesLectorInventario({ lector }: { lector: LectorInventario }) {
  const { reiniciarLector } = usePanelLector(lector);
  return (
    <>
      {lector.errorCamara && (
        <p className="escaneo-aviso-error" role="alert">
          {lector.errorCamara}
        </p>
      )}
      <SelectorCamaraInventario lector={lector} />
      {lector.camaraActiva && <p>Cámara actual: {lector.camaraActiva}</p>}
      <p>
        Mostrá un código por vez. Si la imagen no enfoca, probá otra cámara o la selección
        automática.
      </p>
      <button
        type="button"
        className="btn primary"
        disabled={lector.buscando}
        onClick={reiniciarLector}
      >
        <Icon name="camera" />
        Reiniciar lector
      </button>
      <AtribucionCatalogos />
    </>
  );
}
