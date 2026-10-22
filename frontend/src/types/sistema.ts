import { z } from 'zod';
import type { Sistema } from '@/lib/sistema/tipos';

const colorSchema = z.enum(['orange', 'coral', 'yellow', 'blue', 'petrol']);

export const campoSchema = z.object({
  key: z.string(),
  label: z.string(),
  type: z.enum([
    'text',
    'textarea',
    'number',
    'currency',
    'date',
    'select',
    'email',
    'tel',
    'checkbox',
    'relation',
    'time',
  ]),
  required: z.boolean(),
  options: z.array(z.string()).optional(),
  min: z.number().optional(),
  target: z.string().optional(),
});

// Una fila guarda los valores por la clave de cada campo.
const filaSchema = z
  .object({
    id: z.string(),
    __example: z.boolean().optional(),
    __source: z.string().optional(),
  })
  .catchall(z.union([z.string(), z.number(), z.boolean(), z.null()]));

const bloqueSchema = z.object({
  id: z.string(),
  type: z.enum([
    'inventory',
    'orders',
    'clients',
    'sales',
    'expenses',
    'summary',
    'form',
    'note',
    'entries',
    'exits',
  ]),
  name: z.string(),
  description: z.string(),
  color: colorSchema,
  width: z.enum(['third', 'half', 'wide', 'full']),
  fields: z.array(campoSchema),
  rows: z.array(filaSchema),
  content: z.string(),
  startRow: z.boolean().optional(),
  stockDirection: z.union([z.literal(1), z.literal(-1)]).optional(),
});

// El sistema que arma la persona: secciones con bloques, y cada bloque con sus campos y registros.
export const sistemaSchema = z.object({
  id: z.string().min(1),
  name: z.string().trim().min(1).max(100),
  createdAt: z.string(),
  updatedAt: z.string(),
  sections: z
    .array(z.object({ id: z.string(), name: z.string(), blocks: z.array(bloqueSchema) }))
    .min(1),
  template: z.enum(['warehouse', 'business', 'shop']).optional(),
  options: z.object({ movements: z.boolean(), expenses: z.boolean() }).optional(),
  layout: z.enum(['mosaic', 'single', 'two', 'three']).optional(),
  demoAlmacen: z.boolean().optional(),
}) satisfies z.ZodType<Sistema, z.ZodTypeDef, unknown>;

export const sistemasSchema = z.array(sistemaSchema);
