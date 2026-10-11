import { z } from 'zod';

// Contrato de T-017: GET/POST /api/proyectos/{id}/bloques, PATCH/DELETE /api/bloques/{id}.
// El listado no trae los campos: se piden por bloque (types/campo.ts).
export const bloqueSchema = z.object({
  id: z.string().uuid(),
  proyectoId: z.string().uuid(),
  nombre: z.string(),
  posicion: z.number(),
  creadoEn: z.string(),
  actualizadoEn: z.string(),
});

export const MAX_NOMBRE_BLOQUE = 120;

export const nombreBloqueSchema = z.object({
  nombre: z
    .string()
    .trim()
    .min(1, 'Escribí un nombre para la tabla.')
    .max(MAX_NOMBRE_BLOQUE, `Usá hasta ${MAX_NOMBRE_BLOQUE} caracteres.`),
});

export type Bloque = z.infer<typeof bloqueSchema>;
export type NombreBloque = z.infer<typeof nombreBloqueSchema>;
