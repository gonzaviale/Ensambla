import { usePanelLector } from '@/hooks/escaneo/usePanelLector';
import type { LectorInventario } from '@/types/lectorInventario';

export function SelectorCamaraInventario({ lector }: { lector: LectorInventario }) {
  const { elegirCamara } = usePanelLector(lector);
  if (!lector.camaras.length) return null;
  return (
    <label>
      Cámara del lector
      <select
        value={lector.camaraId || lector.camaraActualId}
        disabled={lector.iniciando}
        onChange={(e) => elegirCamara(e.target.value)}
      >
        <option value="">Elegir automáticamente</option>
        {lector.camaras.map((camara) => (
          <option key={camara.id} value={camara.id}>
            {camara.nombre}
          </option>
        ))}
      </select>
    </label>
  );
}
