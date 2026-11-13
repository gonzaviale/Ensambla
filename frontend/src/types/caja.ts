import { z } from 'zod';
import type { Valor } from '@/lib/sistema/tipos';

export type ModoVentaCaja = 'unidad' | 'peso';
export type LineaCaja = {
  id: string;
  productoId: string;
  nombre: string;
  modo: ModoVentaCaja;
  cantidad: number;
  precio: number;
  importeCentavos: number;
};
export type DatosArticuloCaja = {
  nombre: string;
  precio: string;
  modo: ModoVentaCaja;
  favorito: boolean;
  controlarStock: boolean;
  cantidad: string;
  extras: Record<string, Valor>;
};
export type SolicitudCobro = {
  id: string;
  proyectoId: string;
  inventarioId: string;
  ventasId: string;
  medio: 'EFECTIVO' | 'TRANSFERENCIA' | 'TARJETA';
  recibido: number | null;
  lineas: { productoId: string; cantidad: number; precio: number; modo: 'UNIDAD' | 'PESO' }[];
};
export const ventaCajaSchema = z.object({
  id: z.string(),
  lineas: z.array(z.object({ productoId: z.string(), nombre: z.string(), modo: z.string(),
    cantidad: z.number(), precio: z.number(), importe: z.number() })),
  medio: z.string(), total: z.number(), recibido: z.number(), vuelto: z.number(), creadoEn: z.string(),
});
export type VentaCaja = z.infer<typeof ventaCajaSchema>;
