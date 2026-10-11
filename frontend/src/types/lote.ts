import { z } from 'zod';

const fotoLoteSchema = z.object({
  id: z.string().uuid(),
  posicion: z.number(),
  tipoContenido: z.string(),
  tamanioBytes: z.number(),
});

// Respuesta de POST /api/lotes y GET /api/lotes/{id} (T-090).
export const loteSchema = z.object({
  id: z.string().uuid(),
  estado: z.string(),
  creadoEn: z.string(),
  fotos: z.array(fotoLoteSchema),
});

export type Lote = z.infer<typeof loteSchema>;

// Límite del backend: lo que la IA lee de una vez.
export const MAX_FOTOS_POR_LOTE = 10;
