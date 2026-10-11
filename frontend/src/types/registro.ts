import { z } from 'zod';

// `valores` va indexado por el id del campo: TEXTO y OPCION → string, NUMERO → number,
// FECHA → 'AAAA-MM-DD', SI_NO → boolean. Un dato que no se cargó no aparece.
export const valorSchema = z.union([z.string(), z.number(), z.boolean(), z.null()]);

const valorRegistroSchema = z.union([z.string(), z.number().finite(), z.boolean()]);
export const crearRegistroSchema = z.object({ valores: z.record(valorRegistroSchema) });
// Al editar, un campo que no se envía conserva su valor y un `null` borra el dato.
export const editarRegistroSchema = z.object({
  valores: z.record(valorRegistroSchema.nullable()),
});

export const registroSchema = z.object({
  id: z.string().uuid(),
  bloqueId: z.string().uuid(),
  valores: z.record(valorSchema),
  // Foto del cuaderno de la que salió; null si se cargó a mano.
  fotoOrigenId: z.string().uuid().nullish(),
  creadoEn: z.string(),
  actualizadoEn: z.string(),
});

// Del listado de un bloque: cuántas filas cumplen la búsqueda y cuánto suma cada campo numérico
// (indexado por id de campo). Cuenta todas las páginas, no sólo la que se ve.
export const totalesRegistrosSchema = z.object({
  cantidad: z.number(),
  sumas: z.record(z.number()),
});

export type Valor = z.infer<typeof valorSchema>;
export type TotalesRegistros = z.infer<typeof totalesRegistrosSchema>;
export type Registro = z.infer<typeof registroSchema>;
export type ValorRegistro = z.infer<typeof valorRegistroSchema>;
export type ValoresRegistro = Record<string, ValorRegistro>;
export type CambiosRegistro = Record<string, ValorRegistro | null>;
export type ResultadoGuardado =
  { exito: true } | { exito: false; mensaje: string; errores: Record<string, string> };
export type ResultadoEdicion =
  { exito: true; registro: Registro } | Extract<ResultadoGuardado, { exito: false }>;

export type FiltroRegistros = {
  q?: string;
  /** Id de un campo del bloque, o `creadoEn` (por defecto). */
  orden?: string;
  direccion?: 'asc' | 'desc';
  /** Desde 0. */
  pagina?: number;
  tamanio?: number;
};
