import { z } from 'zod';

// Una orden de compra que un cliente confirmó por WhatsApp y que el dueño gestiona desde la app.
export const ESTADOS_DE_ORDEN = [
  'PENDIENTE',
  'ACEPTADA',
  'RECHAZADA',
  'EN_PREPARACION',
  'LISTA',
] as const;

export const estadoDeOrdenSchema = z.enum(ESTADOS_DE_ORDEN);

export const itemDeOrdenSchema = z.object({
  nombre: z.string(),
  cantidad: z.number(),
  precioUnitario: z.number(),
  subtotal: z.number(),
});

export const ordenDelServidorSchema = z.object({
  id: z.string(),
  // El WhatsApp del cliente: es lo que el dueño ve para saber quién pidió.
  clienteTelefono: z.string().nullable(),
  estado: estadoDeOrdenSchema,
  items: z.array(itemDeOrdenSchema),
  total: z.number(),
  motivoRechazo: z.string().nullable().optional(),
  // Sólo al cambiar el estado: si Sami le pudo avisar al cliente por WhatsApp (false si no salió). Al listar no viene.
  clienteAvisado: z.boolean().nullable().optional(),
  creadoEn: z.string(),
  actualizadoEn: z.string(),
});

export const ordenesDelServidorSchema = z.array(ordenDelServidorSchema);

export type EstadoDeOrden = z.infer<typeof estadoDeOrdenSchema>;
export type ItemDeOrden = z.infer<typeof itemDeOrdenSchema>;
export type OrdenDelServidor = z.infer<typeof ordenDelServidorSchema>;

/** El paso que da el dueño; el motivo sólo cuenta al rechazar. */
export type CambioDeEstado = { estado: EstadoDeOrden; motivo?: string };
