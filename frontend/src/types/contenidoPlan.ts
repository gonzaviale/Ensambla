import { z } from 'zod';

export const accesoContenidoSchema = z.object({
  autorizadas: z.array(z.string()),
  usadas: z.number().int().nonnegative(),
  limite: z.number().int().nonnegative().nullable(),
  total: z.number().int().nonnegative(),
  bloqueadas: z.number().int().nonnegative(),
  finPeriodo: z.string().datetime({ offset: true }),
});

export type AccesoContenido = z.infer<typeof accesoContenidoSchema>;
