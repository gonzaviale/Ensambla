import { z } from 'zod';

// Las métricas del inicio de un cuaderno, calculadas por el servidor con las ventas y el inventario que la persona
// cargó. Lo que no se puede calcular con certeza llega vacío (`null` o lista vacía) y la pantalla no lo muestra.
export const resumenInicioSchema = z.object({
  sistema: z.string(),
  hoy: z.string(),
  hayVentas: z.boolean(),
  hayProductos: z.boolean(),
  ventasDeHoy: z.object({
    ingresos: z.number(),
    unidades: z.number(),
    cantidad: z.number().int(),
    /** Ventas de hoy sin precio: cuentan como unidades pero no suman plata. */
    sinPrecio: z.number().int(),
    ingresosSemanaPasada: z.number(),
  }),
  ultimoDiaConVentas: z.object({ fecha: z.string(), ingresos: z.number() }).nullish(),
  masVendido: z
    .object({
      nombre: z.string(),
      unidades: z.number(),
      ingresos: z.number(),
      ganancia: z.number().nullish(),
    })
    .nullish(),
  faltantes: z.object({
    total: z.number().int(),
    primeros: z.array(
      z.object({
        nombre: z.string(),
        cantidad: z.number(),
        minimo: z.number().nullish(),
        pedir: z.number().nullish(),
      }),
    ),
  }),
  parados: z.array(
    z.object({ nombre: z.string(), cantidad: z.number(), capital: z.number().nullish() }),
  ),
});

export type ResumenInicio = z.infer<typeof resumenInicioSchema>;
