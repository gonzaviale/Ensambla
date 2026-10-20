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

/** Los formatos de foto que Sami puede mirar. */
export const TIPOS_DE_IMAGEN_PARA_SAMI = ['image/jpeg', 'image/png', 'image/webp'] as const;

/** Una foto para Sami: `datos` es la imagen en base64, sin el prefijo `data:`. */
export type ImagenParaSami = {
  tipo: (typeof TIPOS_DE_IMAGEN_PARA_SAMI)[number];
  datos: string;
};

export type PedidoAlTaller = {
  mensaje: string;
  estado: EstadoCompacto;
  historial: { quien: 'sami' | 'you'; texto: string }[];
  /** La foto que la persona adjuntó a este mensaje, si adjuntó una. No queda en el historial. */
  imagen?: ImagenParaSami;
};
