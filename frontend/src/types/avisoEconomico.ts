import { z } from 'zod';

// Un aviso económico del cuaderno: un cambio en la rentabilidad con los números que lo muestran. Por ahora el servidor
// no lo calcula y el inicio usa un ejemplo fijo (ver lib/sistema/avisoEconomicoDeEjemplo); el esquema queda listo para
// cuando llegue del backend.
export const avisoEconomicoSchema = z.object({
  prioridad: z.enum(['ALTA', 'MEDIA', 'BAJA']),
  /** De qué se trata, en pocas palabras: «Rentabilidad». */
  categoria: z.string(),
  titulo: z.string(),
  explicacion: z.string(),
  /** Cuánto cambiaron las ventas frente al período anterior, en puntos porcentuales (12 = +12 %). */
  variacionVentas: z.number(),
  /** Cuánto cambió el resultado bruto (ventas menos costo de lo vendido), en puntos porcentuales. */
  variacionResultadoBruto: z.number(),
  /** Los productos que explican la mayor parte del cambio. */
  productos: z.array(z.string()),
  confianza: z.enum(['ALTA', 'MEDIA', 'BAJA']),
});

export type AvisoEconomico = z.infer<typeof avisoEconomicoSchema>;
