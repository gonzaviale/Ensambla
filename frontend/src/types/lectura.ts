import { z } from 'zod';
import { tipoCampoSchema } from './campo';

const campoSchema = z.object({
  clave: z.string(),
  nombre: z.string(),
  tipo: tipoCampoSchema,
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
  // Bloque del negocio al que conviene sumar las filas; null si la IA propone uno nuevo.
  bloqueExistenteId: z.string().uuid().nullish(),
  campos: z.array(campoSchema),
  filas: z.array(filaSchema),
  advertencias: z.array(z.string()).nullish(),
  // Clave de cada campo leído → id del campo del bloque existente que le corresponde.
  mapeoCampos: z.record(z.string(), z.string().uuid()).nullish(),
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

// Respuesta de POST /api/lotes/{id}/confirmar (T-103).
export const confirmacionSchema = z.object({
  proyectoId: z.string().uuid(),
  bloqueId: z.string().uuid(),
  registros: z.number(),
});

export type Confirmacion = z.infer<typeof confirmacionSchema>;

/** Valor ya tipado de una celda: número, sí/no o texto (fecha ISO u opción). */
export type ValorConfirmado = string | number | boolean;

/** Cuerpo de POST /api/lotes/{id}/confirmar. */
export type ConfirmarLectura = {
  proyectoId: string | null;
  nombreProyecto: string | null;
  bloqueId: string | null;
  nombreBloque: string | null;
  campos: {
    clave: string;
    campoId: string | null;
    nombre: string;
    tipo: string;
    opciones: string[];
  }[];
  filas: { pagina: number | null; valores: Record<string, ValorConfirmado> }[];
};

// Por debajo de este valor la IA no está segura de lo que leyó (igual que ensambla.ia.umbral-confianza).
export const UMBRAL_CONFIANZA = 0.8;

export const ESTADOS_EN_CURSO = ['PENDIENTE', 'PROCESANDO'];

export const ESTADO_CONFIRMADO = 'CONFIRMADO';
