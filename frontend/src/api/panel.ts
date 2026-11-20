import { panelDelNegocioSchema } from '@/types/panel';
import { http } from './client';

/** Cuántos días mira el panel de la home si no se pide otra cosa; el servidor usa los mismos. */
export const DIAS_DEL_PANEL = 30;

// Las métricas y el flujo de caja por semana de un sistema, calculados en el servidor con las mismas
// reglas que el resumen en PDF. Un sistema de otro negocio responde 404.
export const obtenerPanelDelNegocio = (
  sistemaId: string,
  dias = DIAS_DEL_PANEL,
  signal?: AbortSignal,
) =>
  http.get(
    `/negocio/panel?proyectoId=${encodeURIComponent(sistemaId)}&dias=${dias}`,
    panelDelNegocioSchema,
    signal,
  );
