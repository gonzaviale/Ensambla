import type { LectorInventario, PanelLector } from '@/types/lectorInventario';

export function usePanelLector(lector: LectorInventario) {
  return {
    abrirCodigo: () => abrirPanel(lector, 'codigo'),
    abrirAjustes: () => abrirPanel(lector, 'ajustes'),
    cerrarPanel: () => lector.onPanel(null),
    reiniciarLector: () => reiniciarYVolverAlLector(lector),
    elegirCamara: (id: string) => seleccionarYVolverAlLector(lector, id),
  };
}

function abrirPanel(lector: LectorInventario, panel: PanelLector) {
  lector.onCerrar();
  lector.onPanel(panel);
}

function reiniciarYVolverAlLector(lector: LectorInventario) {
  lector.onReiniciar();
  lector.onPanel(null);
}

function seleccionarYVolverAlLector(lector: LectorInventario, id: string) {
  lector.onCamara(id);
  lector.onPanel(null);
}
