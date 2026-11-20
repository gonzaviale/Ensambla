import { z } from 'zod';

// Cómo viene el negocio en los últimos 30 días, en pesos enteros, con el resultado de las últimas seis semanas. Lo
// calcula el servidor con lo anotado en todas las secciones (GET /objetivos/negocio), con las mismas cuentas que el
// resumen en PDF. Sin ventas, hayVentas es false, los importes vienen en cero y no hay semanas.
export const semanaDelNegocioSchema = z.object({
  /** «Hoy» o «Hace N sem.». */
  semana: z.string(),
  /** Lo que quedaba de los 30 días que terminan esa semana. */
  queda: z.number(),
});

export const comoVieneTuNegocioSchema = z.object({
  hayVentas: z.boolean(),
  vendiste: z.number(),
  mercaderia: z.number(),
  gastos: z.number(),
  /** Lo que queda de lo vendido después de pagar la mercadería y los gastos; puede ser negativo. */
  queda: z.number(),
  /** Lo vendido «fiado» en el período. */
  teDeben: z.number(),
  semanas: z.array(semanaDelNegocioSchema),
});

export type SemanaDelNegocio = z.infer<typeof semanaDelNegocioSchema>;
export type ComoVieneTuNegocio = z.infer<typeof comoVieneTuNegocioSchema>;
