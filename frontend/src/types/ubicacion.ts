import { z } from 'zod';

export const puntoSchema = z.object({
  latitud: z.number().finite().min(-90).max(90),
  longitud: z.number().finite().min(-180).max(180),
});

export const direccionSchema = puntoSchema.extend({ direccion: z.string().trim().min(1) });
export const direccionesSchema = z.object({ resultados: z.array(direccionSchema).max(5) });
export const rubrosSchema = z.object({
  rubros: z.array(z.object({ codigo: z.string().min(1), nombre: z.string().min(1) })),
});

export type Punto = z.infer<typeof puntoSchema>;
export type Direccion = z.infer<typeof direccionSchema>;
export type Rubro = z.infer<typeof rubrosSchema>['rubros'][number];
export type ConfiguracionUbicacion = {
  direccion: string;
  punto: Punto;
  rubro: Rubro;
  ajustado: boolean;
};
