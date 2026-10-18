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
// apuntan a otro bloque, qué registros hay para elegir. La imagen en sí todavía no viaja.
export const pedidoDeLecturaSchema = z.object({
  contexto: z.enum(['registros', 'negocio']),
  tipo: z.enum(tiposDeBloque),
  campos: z.array(campoSchema),
  vinculos: z.record(z.string(), z.array(z.string())),
});

export type PedidoDeLectura = {
  contexto: 'registros' | 'negocio';
  tipo: TipoBloque;
  campos: Campo[];
  vinculos: Record<string, string[]>;
};

export const lecturaSchema = z.object({
  filas: z.array(z.record(z.string(), z.union([z.string(), z.number(), z.boolean(), z.null()]))),
  descripcion: z.string(),
});

export type Lectura = { filas: Record<string, Valor>[]; descripcion: string };
