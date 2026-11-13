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
export type MedioCaja = 'EFECTIVO' | 'TRANSFERENCIA' | 'TARJETA';
export type PagoCaja = { medio: MedioCaja; importe: number };
export type SolicitudCobro = {
  id: string;
  proyectoId: string;
  inventarioId: string;
  ventasId: string;
  medio: MedioCaja | 'MIXTO';
  recibido: number | null;
  pagos?: PagoCaja[];
  lineas: { productoId: string; cantidad: number; precio: number; modo: 'UNIDAD' | 'PESO' }[];
};
export const ventaCajaSchema = z.object({
  id: z.string(),
  lineas: z.array(
    z.object({
      productoId: z.string(),
      nombre: z.string(),
      modo: z.string(),
      cantidad: z.number(),
      precio: z.number(),
      importe: z.number(),
    }),
  ),
  medio: z.string(),
  total: z.number(),
  recibido: z.number(),
  vuelto: z.number(),
  creadoEn: z.string(),
  pagos: z
    .array(
      z.object({ medio: z.enum(['EFECTIVO', 'TRANSFERENCIA', 'TARJETA']), importe: z.number() }),
    )
    .default([]),
});
export type VentaCaja = z.infer<typeof ventaCajaSchema>;

export const resumenVentasCajaSchema = z.object({
  ventas: z.array(ventaCajaSchema),
  total: z.number(),
  efectivo: z.number(),
  transferencia: z.number(),
  tarjeta: z.number(),
  desde: z.string().nullable(),
  revision: z.string(),
});
export const cierreCajaSchema = z.object({
  id: z.string(),
  resumen: resumenVentasCajaSchema,
  fondoInicial: z.number(),
  entradas: z.number(),
  retiros: z.number(),
  esperado: z.number(),
  contado: z.number().nullable(),
  diferencia: z.number().nullable(),
  nota: z.string(),
  cerradoEn: z.string(),
});
export const consultaCajaSchema = z.object({
  fecha: z.string(),
  dia: resumenVentasCajaSchema,
  abierta: resumenVentasCajaSchema,
  cierres: z.array(
    z.object({
      id: z.string(),
      desde: z.string(),
      cerradoEn: z.string(),
      total: z.number(),
      cantidadVentas: z.number(),
      diferencia: z.number().nullable(),
    }),
  ),
  pagina: z.number(),
  hayMas: z.boolean(),
});
export type ResumenVentasCaja = z.infer<typeof resumenVentasCajaSchema>;
export type CierreCaja = z.infer<typeof cierreCajaSchema>;
export type ConsultaCaja = z.infer<typeof consultaCajaSchema>;
export type SolicitudCierreCaja = {
  id: string;
  proyectoId: string;
  inventarioId: string;
  revision: string;
  fondoInicial: number;
  entradas: number;
  retiros: number;
  contado: number | null;
  nota: string;
};
