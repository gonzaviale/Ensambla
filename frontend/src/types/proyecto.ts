import { z } from 'zod';
import { tipoCampoSchema } from './campo';

export const campoSchema = z.object({
  id: z.string().uuid(),
  nombre: z.string(),
  tipo: tipoCampoSchema,
  obligatorio: z.boolean(),
  valorInicial: z.string().nullable(),
  opciones: z.array(z.string()),
  bloqueRelacionadoId: z.string().uuid().nullable().default(null),
  posicion: z.number(),
});
export const bloqueSchema = z.object({
  id: z.string().uuid(),
  nombre: z.string(),
  posicion: z.number(),
  campos: z.array(campoSchema),
});
export const proyectoSchema = z.object({
  id: z.string().uuid(),
  nombre: z.string(),
  descripcion: z.string().nullable(),
  plantillaId: z.string().uuid().nullable(),
  creadoEn: z.string(),
  actualizadoEn: z.string(),
  bloques: z.array(bloqueSchema),
});
export const crearProyectoSchema = z.object({
  nombre: z.string().trim().min(1, 'Ingresá un nombre.').max(120, 'Usá hasta 120 caracteres.'),
  descripcion: z.string().max(2000).nullable().optional(),
});
export const renombrarProyectoSchema = crearProyectoSchema.pick({ nombre: true });
export type Proyecto = z.infer<typeof proyectoSchema>;
export type Bloque = z.infer<typeof bloqueSchema>;
export type Campo = z.infer<typeof campoSchema>;
export type CrearProyecto = z.infer<typeof crearProyectoSchema>;
