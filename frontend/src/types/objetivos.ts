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

export const precioZonaObjetivoSchema = z.object({
  minimo: z.number().positive(),
  promedio: z.number().positive(),
  maximo: z.number().positive(),
});

export const productoParaPasoSchema = z.object({
  tipo: z.enum(['SUBIR_PRECIO', 'MOVER_PARADO', 'PONER_A_LA_VISTA']),
  nombre: z.string().min(1),
  precio: z.number().positive(),
  costo: z.number().positive(),
  stock: z.number().min(0),
  vendidas30Dias: z.number().min(0),
  precioZona: precioZonaObjetivoSchema.nullable(),
});

export const productosParaPasosSchema = z.array(productoParaPasoSchema).max(3);

export type PrecioZonaObjetivo = z.infer<typeof precioZonaObjetivoSchema>;
export type ProductoParaPaso = z.infer<typeof productoParaPasoSchema>;
