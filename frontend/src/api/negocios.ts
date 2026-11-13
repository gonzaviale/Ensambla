import { z } from 'zod';
import {
  listaDeNegociosSchema,
  negocioDeLaCuentaSchema,
  resumenConjuntoSchema,
  type NuevoNegocio,
} from '@/types/negocios';
import { http } from './client';

/** Cuántos días mira el resumen conjunto si no se pide otra cosa; el servidor usa los mismos. */
export const DIAS_DEL_RESUMEN_CONJUNTO = 30;

/** Los negocios de la cuenta, cuál está activo y cuántos permite el plan. */
export const listarNegocios = (signal?: AbortSignal) =>
  http.get('/negocios', listaDeNegociosSchema, signal);

/** Crea un negocio. Con el cupo del plan agotado responde 403 con `codigo: LIMITE_NEGOCIOS`. */
export const crearNegocio = (negocio: NuevoNegocio) =>
  http.post('/negocios', negocio, negocioDeLaCuentaSchema);

/** Pasa a ser activo el negocio indicado; uno ajeno responde 404. Devuelve la lista ya actualizada. */
export const activarNegocio = (id: string) =>
  http.put(`/negocios/${encodeURIComponent(id)}/activo`, undefined, listaDeNegociosSchema);

/** Elimina el negocio con todo lo suyo; el último de la cuenta responde 409 y uno ajeno 404. */
export const eliminarNegocio = (id: string) =>
  http.delete(`/negocios/${encodeURIComponent(id)}`, z.undefined());

/** Lo vendido, gastado y pendiente de todos los negocios de la cuenta en los últimos días. */
export const obtenerResumenConjunto = (dias = DIAS_DEL_RESUMEN_CONJUNTO, signal?: AbortSignal) =>
  http.get(`/negocios/resumen?dias=${dias}`, resumenConjuntoSchema, signal);
