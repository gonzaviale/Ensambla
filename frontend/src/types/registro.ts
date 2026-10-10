import { z } from 'zod';

// `valores` va indexado por el id del campo: TEXTO y OPCION → string, NUMERO → number,
// FECHA → 'AAAA-MM-DD', SI_NO → boolean. Un dato que no se cargó no aparece.
export const valorSchema = z.union([z.string(), z.number(), z.boolean(), z.null()]);

export const registroSchema = z.object({
  id: z.string().uuid(),
  bloqueId: z.string().uuid(),
  valores: z.record(valorSchema),
  creadoEn: z.string(),
  actualizadoEn: z.string(),
});

export type Valor = z.infer<typeof valorSchema>;
export type Registro = z.infer<typeof registroSchema>;

export type FiltroRegistros = {
  q?: string;
  /** Id de un campo del bloque, o `creadoEn` (por defecto). */
  orden?: string;
  direccion?: 'asc' | 'desc';
  /** Desde 0. */
  pagina?: number;
  tamanio?: number;
};
