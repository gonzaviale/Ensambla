import { z } from 'zod';

// Un aviso del motor de monitoreo del backend. A diferencia de los que Sami calcula en el navegador, éstos viven
// en el servidor: el motor corre solo, y aplicar o descartar se guarda en la cuenta.
export const accionDelServidorSchema = z
  .object({
    tipo: z.enum(['EDITAR_REGISTRO', 'REGISTRAR', 'ABRIR']),
    texto: z.string().optional(),
    proyectoId: z.string().optional(),
    bloqueId: z.string().optional(),
    registroId: z.string().optional(),
  })
  .passthrough();

export const avisoDelServidorSchema = z.object({
  id: z.string(),
  proyectoId: z.string().nullable(),
  tipo: z.enum(['STOCK', 'PRECIO_ZONA', 'ESTANCADO', 'MUY_VENDIDO', 'COMBO', 'CONSUMO_ZONA']),
  grupo: z.enum(['STOCK', 'VENTAS', 'ZONA']),
  prioridad: z.number().int().min(1).max(3),
  estado: z.enum(['PENDIENTE', 'APLICADO', 'DESCARTADO', 'VENCIDO']),
  entidad: z.string(),
  titulo: z.string(),
  detalle: z.string(),
  motivos: z.array(z.string()),
  acciones: z.array(accionDelServidorSchema),
  creadoEn: z.string(),
  resueltoEn: z.string().nullable(),
});

export const avisosDelServidorSchema = z.array(avisoDelServidorSchema);

export type AccionDelServidor = z.infer<typeof accionDelServidorSchema>;
export type AvisoDelServidor = z.infer<typeof avisoDelServidorSchema>;
