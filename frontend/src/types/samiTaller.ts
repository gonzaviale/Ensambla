import { z } from 'zod';
import type { EstadoCompacto } from '@/lib/sistema/acciones';

// POST /api/sami/taller: un turno de la charla con Sami en el taller. Ver docs/sami-asistente.md.

export const accionDeSamiSchema = z.object({
  herramienta: z.string(),
  argumentos: z.record(z.string(), z.unknown()),
});

/** Una pregunta de Sami con respuestas rápidas. Elegir una equivale a escribir su texto. */
export const opcionesDeSamiSchema = z.object({
  pregunta: z.string(),
  opciones: z.array(z.string()),
});

export const respuestaDelTallerSchema = z.object({
  respuesta: z.string(),
  acciones: z.array(accionDeSamiSchema),
  opciones: opcionesDeSamiSchema.nullish(),
});

export type OpcionesDeSami = z.infer<typeof opcionesDeSamiSchema>;
export type RespuestaDelTaller = z.infer<typeof respuestaDelTallerSchema>;

export type PedidoAlTaller = {
  mensaje: string;
  estado: EstadoCompacto;
  historial: { quien: 'sami' | 'you'; texto: string }[];
};
