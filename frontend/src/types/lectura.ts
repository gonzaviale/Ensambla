import { z } from 'zod';

const campoSchema = z.object({
  clave: z.string(),
  nombre: z.string(),
  tipo: z.string(),
  opciones: z.array(z.string()).nullish(),
});

const datoSchema = z.object({
  valor: z.string().nullable(),
  confianza: z.number(),
});

const filaSchema = z.object({
  pagina: z.number(),
  renglon: z.number(),
  datos: z.record(z.string(), datoSchema),
});

const propuestaSchema = z.object({
  tipoRegistro: z.string().nullish(),
  nombre: z.string().nullish(),
  campos: z.array(campoSchema),
  filas: z.array(filaSchema),
  advertencias: z.array(z.string()).nullish(),
});

// Respuesta de GET /api/lotes/{id}/lectura. La propuesta sólo viene cuando el estado es LISTO.
export const lecturaSchema = z.object({
  loteId: z.string().uuid(),
  estado: z.string(),
  motivoError: z.string().nullish(),
  intentos: z.number(),
  propuesta: propuestaSchema.nullish(),
});

export type Lectura = z.infer<typeof lecturaSchema>;
export type PropuestaLectura = NonNullable<Lectura['propuesta']>;

// Por debajo de este valor la IA no está segura de lo que leyó (igual que ensambla.ia.umbral-confianza).
export const UMBRAL_CONFIANZA = 0.8;

export const ESTADOS_EN_CURSO = ['PENDIENTE', 'PROCESANDO'];
