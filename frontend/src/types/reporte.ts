import { z } from 'zod';
import { registroSchema, totalesRegistrosSchema } from './registro';

// Contrato de T-076: GET /api/bloques/{id}/reporte (filas y totales) y /reporte/descarga (el mismo reporte en XLSX).
export const reporteSchema = z.object({
  /** Las primeras filas del reporte (las más nuevas). */
  filas: z.array(registroSchema),
  /** Cuántas filas cumplen el filtro y cuánto suma cada columna numérica: son los de todas, no sólo los de `filas`. */
  totales: totalesRegistrosSchema,
  /** Hay más filas que las `maximoFilas` que se muestran. */
  truncado: z.boolean(),
  maximoFilas: z.number(),
});

export type Reporte = z.infer<typeof reporteSchema>;

/** Los filtros de un reporte; todos son opcionales. Las fechas son AAAA-MM-DD e incluyen el día entero. */
export type FiltroReporte = {
  q?: string;
  desde?: string;
  hasta?: string;
  /** Una columna de opción o de sí/no y el valor que tiene que tener (`true` o `false` en sí/no). */
  campoId?: string;
  valor?: string;
};
