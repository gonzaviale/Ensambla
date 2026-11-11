import { z } from 'zod';

// El servidor no manda los campos que no tiene (un margen sin costos, gastos sin tabla): llegan como `null`.
const opcional = z
  .number()
  .nullish()
  .transform((valor) => valor ?? null);

const metricasSchema = z.object({
  vendiste: z.number(),
  mercaderia: opcional,
  gastos: opcional,
  queda: z.number(),
  teDeben: z.number(),
  margen: opcional,
  ticketPromedio: z.number(),
  variacion: opcional,
});

const semanaDeCajaSchema = z.object({
  desde: z.string(),
  hasta: z.string(),
  entradas: z.number(),
  salidas: z.number(),
  saldo: z.number(),
  saldoAcumulado: z.number(),
});

/** Las métricas y el flujo de caja de un sistema para la home. Sin ventas en el período no hay ni una ni otro. */
export const panelDelNegocioSchema = z.object({
  desde: z.string(),
  hasta: z.string(),
  dias: z.number(),
  hayDatos: z.boolean(),
  metricas: metricasSchema.nullish().transform((valor) => valor ?? null),
  flujoDeCaja: z.array(semanaDeCajaSchema),
});

export type PanelDelNegocio = z.infer<typeof panelDelNegocioSchema>;
export type MetricasDelPanel = z.infer<typeof metricasSchema>;
export type SemanaDeCaja = z.infer<typeof semanaDeCajaSchema>;
