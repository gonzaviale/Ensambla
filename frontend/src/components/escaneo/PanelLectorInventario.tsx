import { usePanelLector } from '@/hooks/escaneo/usePanelLector';
import type { LectorInventario } from '@/types/lectorInventario';
import { DialogoLector } from './DialogoLector';
import { CodigoManualInventario } from './CodigoManualInventario';
import { AjustesLectorInventario } from './AjustesLectorInventario';

const paneles = {
  codigo: { titulo: 'Escribir código', Contenido: CodigoManualInventario },
  ajustes: { titulo: 'Ajustes del lector', Contenido: AjustesLectorInventario },
};

export function PanelLectorInventario({ lector }: { lector: LectorInventario }) {
  const { cerrarPanel } = usePanelLector(lector);
  if (!lector.panel) return null;
  const { titulo, Contenido } = paneles[lector.panel];
  return (
    <DialogoLector titulo={titulo} onCerrar={cerrarPanel}>
      <Contenido lector={lector} />
    </DialogoLector>
  );
}
