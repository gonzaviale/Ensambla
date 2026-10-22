import { z } from 'zod';
import type { Zona } from '@/lib/sistema/zona';
import { accesoContenidoSchema } from './contenidoPlan';

const productoDeZonaSchema = z.object({
  nombre: z.string(),
  comercios: z.number().int().min(0),
  precio: z.object({ minimo: z.number(), promedio: z.number(), maximo: z.number() }),
  ventas: z.object({ comercios: z.number().int().min(0), unidadesPorSemana: z.number().min(0) }),
});

// Lo que devuelve el servidor: datos agregados y anónimos. Ningún comercio aparece solo ni con su lugar.
export const zonaSchema = z.object({
  nombre: z.string(),
  comercios: z.number().int().min(0),
  productos: z.array(productoDeZonaSchema),
  acceso: accesoContenidoSchema.nullish(),
}) satisfies z.ZodType<Zona, z.ZodTypeDef, unknown>;

/** Lo que la persona escribe para ver otra zona: un barrio o una dirección. */
export const pedidoDeAnalisisSchema = z.object({ lugar: z.string().trim().min(2).max(120) });

export type PedidoDeAnalisis = z.infer<typeof pedidoDeAnalisisSchema>;
