import { z } from 'zod';

// Un objetivo guardado en el servidor: lo que la persona le contó a Sami y la meta mensual. El historial de avance
// no viaja: lo calcula el frontend con lo que hay en los cuadernos.
export const objetivoDelServidorSchema = z.object({
  id: z.string(),
  texto: z.string(),
  meta: z.number(),
  creadoEn: z.string(),
});

export const objetivosDelServidorSchema = z.array(objetivoDelServidorSchema);

export type ObjetivoDelServidor = z.infer<typeof objetivoDelServidorSchema>;

export type NuevoObjetivo = { texto: string; meta: number };
