import { z } from 'zod';
import type { Sistema } from '@/lib/sistema/tipos';
import { sistemaSchema } from './sistema';

// Lo que Sami recibe: la descripción de un negocio para armar una base, o un pedido de ajuste sobre la
// propuesta que ya está a la vista.
export const pedidoDePropuestaSchema = z.discriminatedUnion('tipo', [
  z.object({
    tipo: z.literal('crear'),
    texto: z.string().trim().min(5).max(2500),
    nombre: z.string().max(100),
    actividad: z.enum(['', 'warehouse', 'orders', 'shop']),
  }),
  z.object({
    tipo: z.literal('ajustar'),
    texto: z.string().trim().min(5).max(2500),
    sistema: sistemaSchema,
  }),
]);

// Con el tipo del dominio (no el inferido del esquema) para que la pantalla pueda mandar su `Sistema` tal cual.
export type PedidoDePropuesta =
  | {
      tipo: 'crear';
      texto: string;
      nombre: string;
      actividad: '' | 'warehouse' | 'orders' | 'shop';
    }
  | { tipo: 'ajustar'; texto: string; sistema: Sistema };

export const propuestaSchema = z.object({
  sistema: sistemaSchema,
  /** Lo que Sami le dice a la persona sobre la propuesta. */
  respuesta: z.string(),
});

export type Propuesta = z.infer<typeof propuestaSchema>;
