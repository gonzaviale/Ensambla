import { z } from 'zod';
import {
  metricaSchema,
  metricasSchema,
  resultadoDeMetricaSchema,
  sugeridasSchema,
  type DefinicionDeMetrica,
  type NuevaMetrica,
} from '@/types/metricas';
import { http } from './client';

const rutaDe = (sistemaId: string) => `/proyectos/${encodeURIComponent(sistemaId)}/metricas`;

// Cada cuaderno es de un negocio: el servidor lo comprueba con la sesión y uno ajeno responde 404.
export const listarMetricas = (sistemaId: string, signal?: AbortSignal) =>
  http.get(rutaDe(sistemaId), metricasSchema, signal);

/** Las que el catálogo del rubro sugiere y el cuaderno todavía no guardó, con lo que darían hoy. */
export const listarMetricasSugeridas = (sistemaId: string, signal?: AbortSignal) =>
  http.get(`${rutaDe(sistemaId)}/sugeridas`, sugeridasSchema, signal);

/** Calcula una receta con los datos del cuaderno sin guardarla: la vista previa antes de confirmar. */
export const previsualizarMetrica = (sistemaId: string, definicion: DefinicionDeMetrica) =>
  http.post(`${rutaDe(sistemaId)}/previsualizar`, definicion, resultadoDeMetricaSchema);

export const crearMetrica = (sistemaId: string, metrica: NuevaMetrica) =>
  http.post(rutaDe(sistemaId), metrica, metricaSchema);

export const eliminarMetrica = (sistemaId: string, metricaId: string) =>
  http.delete(`${rutaDe(sistemaId)}/${encodeURIComponent(metricaId)}`, z.undefined());

/** Cambia el nombre que se ve en el panel; lo que calcula queda igual. */
export const renombrarMetrica = (sistemaId: string, metricaId: string, nombre: string) =>
  http.patch(`${rutaDe(sistemaId)}/${encodeURIComponent(metricaId)}`, { nombre }, z.undefined());

/** Pone la métrica en otro lugar del panel (el primero es el 0); el servidor corre las demás. */
export const moverMetrica = (sistemaId: string, metricaId: string, lugar: number) =>
  http.patch(
    `${rutaDe(sistemaId)}/${encodeURIComponent(metricaId)}/lugar`,
    { lugar },
    z.undefined(),
  );
