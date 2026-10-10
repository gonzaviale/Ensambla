import { z } from 'zod';

export const plantillaSchema = z.object({
  id: z.string(),
  nombre: z.string(),
  descripcion: z.string(),
  icono: z.string().nullable(),
  // { bloques: [{ nombre, campos: [{ nombre, tipo, obligatorio, valorInicial, opciones }] }] }
  estructura: z.record(z.unknown()),
});

export type Plantilla = z.infer<typeof plantillaSchema>;
