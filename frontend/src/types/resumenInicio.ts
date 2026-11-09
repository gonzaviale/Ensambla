import { z } from 'zod';

// Las métricas del inicio de un cuaderno, calculadas por el servidor con las ventas y el inventario que la persona
// cargó. Lo que no se puede calcular con certeza llega vacío (`null` o lista vacía) y la pantalla no lo muestra.
// Lo vendido en un período y con qué se compara: para hoy, el mismo día de la semana pasada; para la semana y el mes,
// los 7 o 30 días anteriores.
const ventasDelPeriodoSchema = z.object({
  ingresos: z.number(),
  unidades: z.number(),
  cantidad: z.number().int(),
  /** Ventas sin precio: cuentan como unidades pero no suman plata. */
  sinPrecio: z.number().int(),
  ingresosAnteriores: z.number(),
  /** Lo vendido a fiado en el período. */
  fiado: z.number(),
  ventasFiadas: z.number().int(),
});

/** Lo que entró un día; los días sin ventas valen cero. */
export const ventaDelDiaSchema = z.object({ fecha: z.string(), ingresos: z.number() });

export type VentaDelDia = z.infer<typeof ventaDelDiaSchema>;

export type VentasDelPeriodo = z.infer<typeof ventasDelPeriodoSchema>;

export const resumenInicioSchema = z.object({
  sistema: z.string(),
  hoy: z.string(),
  hayVentas: z.boolean(),
  hayProductos: z.boolean(),
  ventas: z.object({
    hoy: ventasDelPeriodoSchema,
    semana: ventasDelPeriodoSchema,
    mes: ventasDelPeriodoSchema,
  }),
  /** Lo que entró cada uno de los últimos 30 días, del más viejo a hoy. */
  ventasPorDia: z.array(ventaDelDiaSchema),
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
