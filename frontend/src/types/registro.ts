import { z } from 'zod';

const valorRegistroSchema = z.union([z.string(), z.number().finite(), z.boolean()]);
export const crearRegistroSchema = z.object({ valores: z.record(valorRegistroSchema) });
export const registroSchema = crearRegistroSchema.extend({
  id: z.string().uuid(),
  bloqueId: z.string().uuid(),
  creadoEn: z.string(),
  actualizadoEn: z.string(),
});

export type Registro = z.infer<typeof registroSchema>;
export type ValorRegistro = z.infer<typeof valorRegistroSchema>;
export type ValoresRegistro = Record<string, ValorRegistro>;
export type ResultadoGuardado =
  { exito: true } | { exito: false; mensaje: string; errores: Record<string, string> };
