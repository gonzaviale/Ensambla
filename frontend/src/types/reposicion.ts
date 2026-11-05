import { z } from 'zod';

// Lo que hay que reponer en un sistema, agrupado por proveedor. `proveedor` es null en el grupo de los
// productos que todavía no tienen uno cargado.
export const productoFaltanteSchema = z.object({
  id: z.string(),
  nombre: z.string(),
  cantidadActual: z.number(),
  cantidadAPedir: z.number(),
  situacion: z.string(),
});

export const grupoDeReposicionSchema = z.object({
  proveedor: z.string().nullable(),
  productos: z.array(productoFaltanteSchema),
});

export const reposicionSchema = z.object({ grupos: z.array(grupoDeReposicionSchema) });

export type ProductoFaltante = z.infer<typeof productoFaltanteSchema>;
export type GrupoDeReposicion = z.infer<typeof grupoDeReposicionSchema>;
export type Reposicion = z.infer<typeof reposicionSchema>;

/** Cuánto se pide de un producto del inventario. */
export type LineaDelPedido = { productoId: string; cantidad: number };

/** Lo que se le manda al servidor para armar el PDF: a qué proveedor (si hay) y qué productos. */
export type PedidoAlProveedor = { proveedor: string | null; productos: LineaDelPedido[] };
