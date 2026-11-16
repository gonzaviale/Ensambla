import { z } from 'zod';

export const TIPOS_DE_CARTEL = [
  'PROMO_PRECIO',
  'DOS_POR_UNO',
  'COMBO',
  'NOVEDAD',
  'HORARIOS',
  'LISTA_PRECIOS',
] as const;
export const tipoDeCartelSchema = z.enum(TIPOS_DE_CARTEL);
export type TipoDeCartel = z.infer<typeof tipoDeCartelSchema>;

export const FORMATOS_DE_CARTEL = ['A4', 'A5', 'CUADRADO', 'VERTICAL'] as const;
export const formatoDeCartelSchema = z.enum(FORMATOS_DE_CARTEL);
export type FormatoDeCartel = z.infer<typeof formatoDeCartelSchema>;

export const varianteDeCartelSchema = z.enum(['BLOQUE', 'FRANJA', 'MARCO']);
export type VarianteDeCartel = z.infer<typeof varianteDeCartelSchema>;

export const promocionSugeridaSchema = z.object({
  tipo: tipoDeCartelSchema,
  productos: z.array(z.string()),
  precioAnterior: z.number(),
  precioNuevo: z.number(),
  ahorro: z.number(),
  descuentoPorcentaje: z.number(),
});
export type PromocionSugerida = z.infer<typeof promocionSugeridaSchema>;

export const promocionesSugeridasSchema = z.object({
  promociones: z.array(promocionSugeridaSchema),
});

export const textosDeCartelSchema = z.object({
  plantilla: tipoDeCartelSchema,
  titular: z.string(),
  bajada: z.string(),
  textoPosteo: z.string(),
  redactadoConIa: z.boolean(),
});
export type TextosDeCartel = z.infer<typeof textosDeCartelSchema>;

export const opcionDeCartelSchema = z.object({
  variante: varianteDeCartelSchema,
  dibujo: z.string(),
});
export type OpcionDeCartel = z.infer<typeof opcionDeCartelSchema>;

export const opcionesDeCartelSchema = z.object({ opciones: z.array(opcionDeCartelSchema) });

/** Lo que cuenta un cartel: textos editables y el protagonista (precio) que sale de los datos. */
export type ContenidoDeCartel = {
  tipo: TipoDeCartel;
  formato: FormatoDeCartel;
  titular: string;
  bajada: string;
  protagonista: string;
  detalle: string;
  lineas: string[];
};

export type PedidoDeTextos = {
  tiposPosibles: TipoDeCartel[];
  productos: string[];
  numeros: number[];
};

export type PedidoDeExportacion = ContenidoDeCartel & { variante: VarianteDeCartel };
