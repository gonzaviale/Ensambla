import { z } from 'zod';

// Un aviso del motor de monitoreo del backend. A diferencia de los que Sami calcula en el navegador, éstos viven
// en el servidor: el motor corre solo, y aplicar o descartar se guarda en la cuenta.
export const accionDelServidorSchema = z
  .object({
    tipo: z.enum(['EDITAR_REGISTRO', 'CREAR_REGISTRO', 'REGISTRAR', 'ABRIR', 'VER_ZONA']),
    texto: z.string().optional(),
    /** El producto de la zona que abre «Ver en Mi zona». */
    producto: z.string().optional(),
    proyectoId: z.string().optional(),
    bloqueId: z.string().optional(),
    registroId: z.string().optional(),
  })
  .passthrough();

export const tipoDeAvisoSchema = z.enum([
  'STOCK',
  'PRECIO_ZONA',
  'ESTANCADO',
  'MUY_VENDIDO',
  'COMBO',
  'CONSUMO_ZONA',
  'REVISAR_PRECIO',
]);

export const avisoDelServidorSchema = z.object({
  id: z.string(),
  proyectoId: z.string().nullable(),
  tipo: tipoDeAvisoSchema,
  grupo: z.enum(['STOCK', 'VENTAS', 'ZONA']),
  prioridad: z.number().int().min(1).max(3),
  estado: z.enum(['PENDIENTE', 'APLICADO', 'DESCARTADO', 'VENCIDO']),
  entidad: z.string(),
  titulo: z.string(),
  detalle: z.string(),
  motivos: z.array(z.string()),
  acciones: z.array(accionDelServidorSchema),
  // Los números con los que el motor armó el aviso (cantidad y mínimo, precio y precio sugerido…).
  datos: z.record(z.string(), z.unknown()).optional(),
  creadoEn: z.string(),
  resueltoEn: z.string().nullable(),
});

export const avisosDelServidorSchema = z.array(avisoDelServidorSchema);

export type AccionDelServidor = z.infer<typeof accionDelServidorSchema>;
export type AvisoDelServidor = z.infer<typeof avisoDelServidorSchema>;

// Qué pasó con las ventas después de aplicar un aviso: una comparación entre los días anteriores y los posteriores.
export const impactoAvisosSchema = z.object({
  aplicados: z.number().int(),
  descartados: z.number().int(),
  ingresoSemanalDelta: z.number(),
  medidos: z.array(
    z.object({
      avisoId: z.string(),
      tipo: tipoDeAvisoSchema,
      titulo: z.string(),
      aplicadoEn: z.string().nullable(),
      ventanaDias: z.number().int(),
      unidadesPorDiaAntes: z.number(),
      unidadesPorDiaDespues: z.number(),
      ingresoPorDiaAntes: z.number(),
      ingresoPorDiaDespues: z.number(),
      ingresoSemanalDelta: z.number(),
    }),
  ),
});

export type ImpactoAvisos = z.infer<typeof impactoAvisosSchema>;
