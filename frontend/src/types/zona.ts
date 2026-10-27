import { z } from 'zod';
import type { Zona } from '@/lib/sistema/zona';
import { puntoSchema } from '@/types/ubicacion';
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

/**
 * Lo que la persona pide para ver otra zona: un punto del mapa (el servidor busca la zona que lo contiene)
 * o, si no hay punto, un barrio o una dirección escritos.
 */
export const pedidoDeAnalisisSchema = z.union([
  puntoSchema,
  z.object({ lugar: z.string().trim().min(2).max(120) }),
]);

export type PedidoDeAnalisis = z.infer<typeof pedidoDeAnalisisSchema>;
