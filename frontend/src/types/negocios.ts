import { z } from 'zod';

// Los negocios de una cuenta (ver docs/negocios-de-la-cuenta.md). El plan es de la cuenta y fija cuántos
// negocios puede tener; uno de ellos es el activo y es el que usan todas las demás pantallas.

export const NOMBRE_NEGOCIO_MAX = 120;

export const negocioDeLaCuentaSchema = z.object({
  id: z.string().min(1),
  nombre: z.string().min(1).max(NOMBRE_NEGOCIO_MAX),
  rubro: z.string().min(1),
  activo: z.boolean(),
  creadoEn: z.string(),
});
export type NegocioDeLaCuenta = z.infer<typeof negocioDeLaCuentaSchema>;

export const listaDeNegociosSchema = z.object({
  activoId: z.string().min(1),
  /** Cuántos negocios permite el plan de la cuenta (Gratuito 1, Premium 6). */
  limite: z.number().int().positive(),
  puedeCrear: z.boolean(),
  negocios: z.array(negocioDeLaCuentaSchema).min(1),
});
export type ListaDeNegocios = z.infer<typeof listaDeNegociosSchema>;

export const nuevoNegocioSchema = z
  .object({
    nombre: z.string().trim().min(1).max(NOMBRE_NEGOCIO_MAX),
    rubro: z.string().min(1).optional(),
  })
  .strict();
export type NuevoNegocio = z.infer<typeof nuevoNegocioSchema>;

// El servidor no manda lo que no puede calcular (como en el panel de un solo negocio): llega como `null`.
const opcional = z
  .number()
  .nullish()
  .transform((valor) => valor ?? null);

export const resumenDeNegocioSchema = z.object({
  id: z.string().min(1),
  nombre: z.string().min(1),
  rubro: z.string().min(1),
  activo: z.boolean(),
  hayDatos: z.boolean(),
  vendiste: opcional,
  gastos: opcional,
  queda: opcional,
  teDeben: opcional,
  avisosPendientes: z.number().int().nonnegative(),
});
export type ResumenDeNegocio = z.infer<typeof resumenDeNegocioSchema>;

export const resumenConjuntoSchema = z.object({
  desde: z.string(),
  hasta: z.string(),
  dias: z.number().int().positive(),
  /** La suma de los negocios con datos; los que no tienen ventas en el período no aportan. */
  total: z.object({
    negocios: z.number().int().positive(),
    vendiste: z.number(),
    gastos: z.number(),
    queda: z.number(),
    teDeben: z.number(),
    avisosPendientes: z.number().int().nonnegative(),
  }),
  negocios: z.array(resumenDeNegocioSchema).min(1),
});
export type ResumenConjunto = z.infer<typeof resumenConjuntoSchema>;
