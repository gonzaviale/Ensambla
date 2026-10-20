import { z } from 'zod';
import type { Campo, TipoBloque, Valor } from '@/lib/sistema/tipos';
import { campoSchema } from './sistema';

const tiposDeBloque = [
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
] as const;

// Lo que se le pide a la lectura de una imagen: a qué bloque se llevan los datos y, para los campos que
// apuntan a otro bloque, qué registros hay para elegir. `destinoExistente` avisa que el bloque ya está
// en el sistema: sólo ahí tiene sentido detectar columnas que le faltan. La imagen viaja aparte, en el mismo
// multipart, y `nombre` es el del bloque de destino.
export const pedidoDeLecturaSchema = z.object({
  nombre: z.string().default(''),
  contexto: z.enum(['registros', 'negocio']),
  tipo: z.enum(tiposDeBloque),
  campos: z.array(campoSchema),
  vinculos: z.record(z.string(), z.array(z.string())),
  destinoExistente: z.boolean().default(false),
});

export type PedidoDeLectura = {
  nombre: string;
  contexto: 'registros' | 'negocio';
  tipo: TipoBloque;
  campos: Campo[];
  vinculos: Record<string, string[]>;
  destinoExistente?: boolean;
};

const valorSchema = z.union([z.string(), z.number(), z.boolean(), z.null()]);

// Un dato que la lectura no pudo leer con seguridad: de qué registro (desde 0), de qué campo y por qué.
const dudaSchema = z.object({
  fila: z.number().int().min(0),
  campo: z.string(),
  motivo: z.string(),
});

export const lecturaSchema = z.object({
  filas: z.array(z.record(z.string(), valorSchema)),
  descripcion: z.string(),
  // Columnas de la foto que el bloque destino no tiene. Sus valores vienen en cada fila, por la clave
  // del campo propuesto.
  camposNuevos: z.array(campoSchema).default([]),
  dudas: z.array(dudaSchema).default([]),
});

export type DudaDeLectura = { fila: number; campo: string; motivo: string };

export type Lectura = {
  filas: Record<string, Valor>[];
  descripcion: string;
  camposNuevos: Campo[];
  dudas: DudaDeLectura[];
};
