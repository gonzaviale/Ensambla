import { z } from 'zod';

// Contrato de T-074: POST/GET /api/bloques/{id}/movimientos y GET /api/bloques/{id}/stock.
// El stock no se guarda: es la suma de las entradas menos las salidas de cada producto (una fila de la tabla).
export const TIPOS_MOVIMIENTO = ['ENTRADA', 'SALIDA'] as const;
export const tipoMovimientoSchema = z.enum(TIPOS_MOVIMIENTO);

export const movimientoSchema = z.object({
  id: z.string().uuid(),
  registroId: z.string().uuid(),
  /** El nombre de la fila movida (la etiqueta con la que se muestra en una conexión). */
  producto: z.string(),
  tipo: tipoMovimientoSchema,
  cantidad: z.number(),
  /** AAAA-MM-DD */
  fecha: z.string(),
  nota: z.string().nullable(),
  creadoEn: z.string(),
});

export const movimientoRegistradoSchema = z.object({
  movimiento: movimientoSchema,
  /** El stock del producto después de aplicar el movimiento. */
  stockActual: z.number(),
});

export const stockProductoSchema = z.object({
  registroId: z.string().uuid(),
  producto: z.string(),
  stock: z.number(),
});

export const MAX_NOTA_MOVIMIENTO = 300;

export type TipoMovimiento = z.infer<typeof tipoMovimientoSchema>;
export type Movimiento = z.infer<typeof movimientoSchema>;
export type MovimientoRegistrado = z.infer<typeof movimientoRegistradoSchema>;
export type StockProducto = z.infer<typeof stockProductoSchema>;

/** Cuerpo de POST /bloques/{id}/movimientos. */
export type NuevoMovimiento = {
  registroId: string;
  tipo: TipoMovimiento;
  cantidad: number;
  fecha: string;
  nota: string | null;
};

export type FiltroStock = {
  q?: string;
  /** Desde 0. */
  pagina?: number;
  tamanio?: number;
};
