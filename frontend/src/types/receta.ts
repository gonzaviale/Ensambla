import { z } from 'zod';

// Contrato de T-075: GET/PUT /api/registros/{id}/receta. La receta dice qué insumos (filas de una tabla) y cuánto
// de cada uno lleva un producto (otra fila). El costo no se guarda: lo calcula la API con el precio actual de cada insumo.
export const lineaRecetaSchema = z.object({
  insumoId: z.string().uuid(),
  /** El nombre del insumo; «Insumo eliminado» si la fila ya no existe. */
  insumo: z.string(),
  cantidad: z.number(),
  /** Sólo una etiqueta («kg»): la cantidad va en la misma unidad que el precio. */
  unidad: z.string().nullable(),
  /** Nulo si al insumo le falta el precio o ya no existe. */
  precioUnitario: z.number().nullable(),
  subtotal: z.number().nullable(),
});

export const recetaSchema = z.object({
  /** La tabla de los insumos y su columna numérica con el precio; nulos si no hay receta. */
  bloqueInsumosId: z.string().uuid().nullable(),
  campoPrecioId: z.string().uuid().nullable(),
  lineas: z.array(lineaRecetaSchema),
  /** La suma de cantidad × precio de los insumos que se pudieron calcular. */
  costo: z.number(),
  /** A algún insumo le falta el precio (o ya no existe): el costo no está completo. */
  incompleta: z.boolean(),
});

export const MAX_UNIDAD_RECETA = 30;
export const MAX_LINEAS_RECETA = 50;

export type LineaReceta = z.infer<typeof lineaRecetaSchema>;
export type Receta = z.infer<typeof recetaSchema>;

/** Cuerpo de PUT /registros/{id}/receta: reemplaza la receta; sin líneas, la quita. */
export type GuardarReceta = {
  campoPrecioId: string | null;
  lineas: { insumoId: string; cantidad: number; unidad: string | null }[];
};
