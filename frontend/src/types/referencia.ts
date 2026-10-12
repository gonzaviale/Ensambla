import { z } from 'zod';

// GET /api/bloques/{id}/referencias: filas de una tabla con el texto que las nombra (RELACION).
export const referenciaSchema = z.object({
  id: z.string().uuid(),
  etiqueta: z.string(),
});
export const referenciasSchema = z.array(referenciaSchema);

export type Referencia = z.infer<typeof referenciaSchema>;
