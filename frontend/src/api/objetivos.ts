import { z } from 'zod';
import {
  candidatosMiniObjetivoSchema,
  decisionesObjetivoSchema,
  decisionObjetivoSchema,
  miniObjetivoSchema,
  miniObjetivosSchema,
  objetivoDelServidorSchema,
  objetivosDelServidorSchema,
  productosParaPasosSchema,
  progresoObjetivoSchema,
  semanasObjetivoSchema,
  type CandidatoMiniObjetivo,
  type DecisionDePaso,
  type NuevoObjetivo,
  type ProductoParaPaso,
} from '@/types/objetivos';
import { http } from './client';

// Cada negocio ve solo los suyos: el servidor los busca por la sesión, nunca por un dato que mande el navegador.
export const listarObjetivos = (signal?: AbortSignal) =>
  http.get('/objetivos', objetivosDelServidorSchema, signal);

export const crearObjetivo = (objetivo: NuevoObjetivo) =>
  http.post('/objetivos', objetivo, objetivoDelServidorSchema);

// Un objetivo ajeno responde 404, igual que uno que no existe.
export const eliminarObjetivo = (id: string) =>
  http.delete(`/objetivos/${encodeURIComponent(id)}`, z.undefined());

/** Pide al servidor que mida ya los objetivos del negocio, sin esperar al repaso de cada hora. */
export const medirObjetivos = () => http.post('/objetivos/medir', undefined, z.undefined());

export const obtenerPasosObjetivo = (signal?: AbortSignal): Promise<ProductoParaPaso[]> =>
  http.get('/objetivos/pasos', productosParaPasosSchema, signal);

const rutaDe = (objetivoId: string) => `/objetivos/${encodeURIComponent(objetivoId)}`;
const rutaDeMini = (objetivoId: string, miniId: string) =>
  `${rutaDe(objetivoId)}/mini-objetivos/${encodeURIComponent(miniId)}`;

export const obtenerMiniObjetivos = (objetivoId: string, signal?: AbortSignal) =>
  http.get(`${rutaDe(objetivoId)}/mini-objetivos`, miniObjetivosSchema, signal);

export const obtenerCandidatos = (objetivoId: string, miniId: string, signal?: AbortSignal) =>
  http.get(`${rutaDeMini(objetivoId, miniId)}/candidatos`, candidatosMiniObjetivoSchema, signal);

// Se manda el candidato tal cual vino de /candidatos: el servidor lo vuelve a calcular y lo guarda.
export const reemplazarMiniObjetivo = (
  objetivoId: string,
  miniId: string,
  candidato: Pick<CandidatoMiniObjetivo, 'tipo' | 'registroId' | 'referencia'>,
) =>
  http.post(
    `${rutaDeMini(objetivoId, miniId)}/reemplazo`,
    { tipo: candidato.tipo, registroId: candidato.registroId, referencia: candidato.referencia },
    miniObjetivoSchema,
  );

/** «Quitar»: el mini pasa a ABANDONADO y el domingo Sami propone otro en su lugar. */
export const abandonarMiniObjetivo = (objetivoId: string, miniId: string) =>
  http.delete(rutaDeMini(objetivoId, miniId), z.undefined());

export const decidirPaso = (objetivoId: string, miniId: string, tipo: DecisionDePaso) =>
  http.post(`${rutaDeMini(objetivoId, miniId)}/decisiones`, { tipo }, decisionObjetivoSchema);

export const obtenerProgresoObjetivo = (objetivoId: string, signal?: AbortSignal) =>
  http.get(`${rutaDe(objetivoId)}/progreso`, progresoObjetivoSchema, signal);

export const obtenerDecisionesObjetivo = (objetivoId: string, signal?: AbortSignal) =>
  http.get(`${rutaDe(objetivoId)}/decisiones`, decisionesObjetivoSchema, signal);

export const obtenerSemanasObjetivo = (objetivoId: string, signal?: AbortSignal) =>
  http.get(`${rutaDe(objetivoId)}/semanas`, semanasObjetivoSchema, signal);
