import { z } from 'zod';
import { ventaDelDiaSchema } from './resumenInicio';

export const PERIODOS_DEL_SERVIDOR = ['HOY', 'SEMANA', 'MES'] as const;

const productoSchema = z.object({
  nombre: z.string(),
  cantidad: z.number(),
});

// Las métricas detalladas de un cuaderno en un período, para «Mis métricas». Las mismas cuentas que el PDF.
export const metricasSchema = z.object({
  sistema: z.string(),
  periodo: z.enum(PERIODOS_DEL_SERVIDOR),
  desde: z.string(),
  hasta: z.string(),
  hayVentas: z.boolean(),
  ventas: z.object({
    ingresos: z.number(),
    unidades: z.number(),
    cantidad: z.number().int(),
    sinPrecio: z.number().int(),
    ingresoPorDia: z.number(),
    ingresosAnteriores: z.number(),
    /** Ventas menos el costo actual de lo vendido; sin costos cargados no se puede calcular. */
    gananciaEstimada: z.number().nullish(),
  }),
  porDia: z.array(ventaDelDiaSchema),
  masVendidos: z.array(
    z.object({
      nombre: z.string(),
      unidades: z.number(),
      ingresos: z.number(),
      ganancia: z.number().nullish(),
    }),
  ),
  parados: z.array(productoSchema.extend({ capital: z.number().nullish() })),
  stock: z.object({
    productos: z.number().int(),
    totalFaltantes: z.number().int(),
    faltantes: z.array(
      productoSchema.extend({ minimo: z.number().nullish(), pedir: z.number().nullish() }),
    ),
    valorACosto: z.number(),
    valorAPrecio: z.number(),
  }),
  clientes: z.object({
    top: z.array(z.object({ nombre: z.string(), compras: z.number().int(), ingresos: z.number() })),
    /** Lo vendido a fiado en el período, no lo que todavía se debe. */
    fiado: z.number(),
    ventasFiadas: z.number().int(),
  }),
  gastos: z
    .object({
      total: z.number(),
      resultado: z.number(),
      porCategoria: z.array(z.object({ categoria: z.string(), total: z.number() })),
    })
    .nullish(),
});

export type Metricas = z.infer<typeof metricasSchema>;
