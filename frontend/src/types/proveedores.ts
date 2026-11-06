import { z } from 'zod';

const importe = z.number().finite().nonnegative().max(1_000_000_000);
const cantidad = z.number().int().nonnegative().max(1_000_000);
const fecha = z
  .string()
  .regex(/^\d{4}-\d{2}-\d{2}$/)
  .refine((valor) => {
    const instante = new Date(`${valor}T12:00:00`);
    return Number.isFinite(instante.getTime()) && instante.toISOString().slice(0, 10) === valor;
  }, 'Fecha inválida');
const proveedorSchema = z.object({
  id: z.string(),
  nombre: z.string().min(1),
  telefono: z.string(),
});
const productoSchema = z.object({
  id: z.string(),
  nombre: z.string().min(1),
  presentacion: z.string().min(1),
  proveedorId: z.string(),
  stock: cantidad,
  minimo: cantidad,
  costo: importe,
  precio: importe,
});
const lineaSchema = z.object({
  productoId: z.string(),
  cantidad: cantidad.positive(),
  costo: importe.positive(),
});
const compraSchema = z.object({
  id: z.string(),
  proveedorId: z.string(),
  numero: z.string().min(1),
  fecha,
  archivo: z.string(),
  lineas: z.array(lineaSchema).min(1),
  extras: importe,
  descuento: importe,
  total: importe.positive(),
});
// Mismas claves y categorías que el bloque expenses de Ensambla. La compra vinculada evita duplicados.
const gastoSchema = z.object({
  id: z.string(),
  name: z.string().min(1),
  date: fecha,
  total: importe.positive(),
  category: z.enum(['Mercadería', 'Servicios', 'Alquiler', 'Otros']),
  compraId: z.string().optional(),
});
const ventaSchema = z.object({
  fecha,
  cantidad: cantidad.positive(),
  precio: importe,
  costo: importe,
});
const pedidoSchema = z.object({ productoId: z.string(), cantidad });
export const demoProveedoresSchema = z.object({
  version: z.literal(1),
  proveedores: z.array(proveedorSchema).min(1),
  productos: z.array(productoSchema),
  compras: z.array(compraSchema),
  gastos: z.array(gastoSchema),
  ventas: z.array(ventaSchema),
  pedido: z.array(pedidoSchema),
});
export type Proveedor = z.infer<typeof proveedorSchema>;
export type ProductoProveedor = z.infer<typeof productoSchema>;
export type LineaCompra = z.infer<typeof lineaSchema>;
export type CompraProveedor = z.infer<typeof compraSchema>;
export type GastoProveedor = z.infer<typeof gastoSchema>;
export type DemoProveedores = z.infer<typeof demoProveedoresSchema>;
export type LineaPedido = z.infer<typeof pedidoSchema>;
export type RevisionCompra = Omit<CompraProveedor, 'total'> & { totalDeclarado: number };
export type VistaProveedor = 'pedido' | 'comprobantes' | 'costos' | 'stock';
export type ComparacionCosto = {
  compra: CompraProveedor;
  producto: ProductoProveedor;
  linea: LineaCompra;
  anterior: number | null;
  variacion: number | null;
  margenActual: number | null;
  precioSugerido: number | null;
};
