import { z } from 'zod';
import {
  candidatosMiniObjetivoSchema,
  propuestaObjetivoSchema,
  respuestaMiniObjetivoSchema,
  respuestasPendientesSchema,
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
  type CuotaObjetivos,
  type DecisionDePaso,
  type MensajeDeConversacion,
  type NuevoObjetivo,
  type ObjetivoConSami,
  type RespuestaDeMini,
  type ProductoParaPaso,
} from '@/types/objetivos';
import { http } from './client';
import { consultarPlanes } from './planes';

// Cada negocio ve solo los suyos: el servidor los busca por la sesión, nunca por un dato que mande el navegador.
export const listarObjetivos = (signal?: AbortSignal) =>
  http.get('/objetivos', objetivosDelServidorSchema, signal);

export const crearObjetivo = (objetivo: NuevoObjetivo) =>
  http.post('/objetivos', objetivo, objetivoDelServidorSchema);

/** Sami lee lo que la persona escribió y propone mini-objetivos; no guarda nada ni gasta cuota. */
export const proponerObjetivo = (mensajes: MensajeDeConversacion[]) =>
  http.post('/objetivos/sami/propuesta', { mensajes }, propuestaObjetivoSchema);

/** Guarda lo que la persona aceptó; con `seguimientoManual` el servidor arma uno solo, sin IA ni cuota. */
export const guardarObjetivoConSami = (objetivo: ObjetivoConSami) =>
  http.post('/objetivos/sami', objetivo, objetivoDelServidorSchema);

/**
 * Cuántos objetivos con IA lleva el negocio este mes: el servidor lo informa dentro de `/planes` (uso y tope del plan),
 * y es `null` si el plan no tiene tope.
 */
export const obtenerCuotaObjetivos = async (
  signal?: AbortSignal,
): Promise<CuotaObjetivos | null> => {
  const { uso } = await consultarPlanes(signal);
  if (uso.objetivosIa === undefined || uso.limiteObjetivosIa == null) return null;
  return {
    usadas: uso.objetivosIa,
    limite: uso.limiteObjetivosIa,
    restantes: Math.max(0, uso.limiteObjetivosIa - uso.objetivosIa),
  };
};

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

/** Un manual se responde una sola vez por semana: la segunda respuesta es un 409 y la primera queda como estaba. */
export const responderMiniObjetivo = (
  objetivoId: string,
  miniId: string,
  respuesta: RespuestaDeMini,
) =>
  http.post(
    `${rutaDeMini(objetivoId, miniId)}/respuesta`,
    {
      hizo: respuesta.hizo,
      quePaso: respuesta.quePaso,
      ...(respuesta.comentario?.trim() && { comentario: respuesta.comentario.trim() }),
    },
    respuestaMiniObjetivoSchema,
  );

/** Los manuales en curso sin respuesta de todos los objetivos del negocio, del más viejo al más nuevo. */
export const listarRespuestasPendientes = (signal?: AbortSignal) =>
  http.get('/objetivos/respuestas-pendientes', respuestasPendientesSchema, signal);
