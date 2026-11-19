import type { DefinicionDeMetrica, PeriodoDeMetrica } from '@/types/metricas';

// Igual que el servidor: dos recetas son la misma si piden lo mismo, aunque se llamen distinto.
const LO_QUE_PIDE_UNA_RECETA = ['fuente', 'calculo', 'agrupar', 'periodo', 'soloFiado'] as const;

export const mismaReceta = (una: DefinicionDeMetrica, otra: DefinicionDeMetrica) =>
  LO_QUE_PIDE_UNA_RECETA.every((dato) => una[dato] === otra[dato]);

const PERIODOS_DEL_PANEL_DE_VENTAS: PeriodoDeMetrica[] = ['HOY', 'SEMANA', 'MES'];

/** Lo que el panel de ventas del inicio ya muestra siempre: lo vendido hoy, esta semana y este mes. */
const EL_PANEL_DE_VENTAS: DefinicionDeMetrica[] = PERIODOS_DEL_PANEL_DE_VENTAS.map((periodo) => ({
  nombre: 'Ventas',
  fuente: 'VENTAS',
  calculo: 'SUMA',
  agrupar: 'NINGUNA',
  periodo,
  soloFiado: false,
}));

/** {@code true} si ya está a la vista: en el panel de ventas del inicio o entre las guardadas. */
export const yaEstaALaVista = (receta: DefinicionDeMetrica, guardadas: DefinicionDeMetrica[]) =>
  [...EL_PANEL_DE_VENTAS, ...guardadas].some((otra) => mismaReceta(otra, receta));

export const YA_LA_TENES = 'Esa ya la tenés en el panel: probá con otra';
