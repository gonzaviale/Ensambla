import { z } from 'zod';

// Un aviso económico del cuaderno: cómo le fue esta semana frente a la anterior, en ventas y en ganancia, con los
// productos que más explican el cambio. Lo calcula el servidor (GET /proyectos/{id}/resumen/aviso-economico).
export const avisoEconomicoSchema = z.object({
  prioridad: z.enum(['ALTA', 'MEDIA', 'BAJA']),
  /** MEJORA cuando la ganancia (o, sin costos, las ventas) subió; EMPEORA cuando bajó. */
  tendencia: z.enum(['MEJORA', 'EMPEORA']),
  /** De qué se trata, en pocas palabras: «Rentabilidad» o, sin costos cargados, «Ventas». */
  categoria: z.string(),
  titulo: z.string(),
  explicacion: z.string(),
  /** Cuánto cambiaron las ventas frente a la semana anterior, en % (12 = +12 %). */
  variacionVentas: z.number(),
  /** Cuánto cambió la ganancia (ventas menos costo de lo vendido), en %; null si faltan costos para calcularla. */
  variacionResultadoBruto: z.number().nullable(),
  /** Los productos que explican la mayor parte del cambio. */
  productos: z.array(z.string()),
  confianza: z.enum(['ALTA', 'MEDIA', 'BAJA']),
});

export type AvisoEconomico = z.infer<typeof avisoEconomicoSchema>;

/** Sin una semana anterior con ventas no hay con qué comparar y el aviso llega vacío. */
export const avisoEconomicoResponseSchema = z.object({
  aviso: avisoEconomicoSchema.nullable(),
});
