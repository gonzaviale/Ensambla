import { z } from 'zod';

// Lo que la persona decidió sobre los avisos y la bienvenida; viaja con la cuenta.
export const preferenciasSchema = z.object({
  /** Ids de los avisos que descartó. */
  descartadas: z.array(z.string()).max(200),
  /** Grupos de avisos que apagó (por defecto todos están prendidos). */
  grupos: z.record(z.string(), z.boolean()),
  /** Ya vio la bienvenida y eligió explorar o hacer la guía. */
  bienvenidaVista: z.boolean(),
  /** Cómo quiere que Sami lo llame; si falta, se usa el nombre de la cuenta. */
  nombre: z.string().trim().min(1).max(40).optional(),
});

export type Preferencias = z.infer<typeof preferenciasSchema>;

export const PREFERENCIAS_INICIALES: Preferencias = {
  descartadas: [],
  grupos: {},
  bienvenidaVista: false,
};
