import { z } from 'zod';
import { puntoSchema } from './ubicacion';

export const ubicacionNegocioSchema = puntoSchema.extend({
  direccion: z.string().nullable(),
  localidad: z.string().nullable(),
});

export const configuracionNegocioSchema = z.object({
  nombre: z.string().min(1).max(120),
  rubro: z.string().min(1),
  ubicacion: ubicacionNegocioSchema.nullable(),
  comparteDatosZona: z.boolean(),
});

export type ConfiguracionNegocio = z.infer<typeof configuracionNegocioSchema>;
export const cambiosNegocioSchema = z
  .object({
    nombre: z.string().trim().min(1).max(120).optional(),
    rubro: z.string().min(1).optional(),
    ubicacion: puntoSchema
      .extend({
        direccion: z.string().trim().min(1).max(500),
        localidad: z.string().trim().min(1).max(120),
      })
      .optional(),
    comparteDatosZona: z.boolean().optional(),
  })
  .strict();
export type CambiosNegocio = z.infer<typeof cambiosNegocioSchema>;
