import { z } from 'zod';

const fecha = z.string().datetime({ offset: true });
const cantidad = z.number().int().nonnegative();
export const codigoPlanSchema = z.enum(['GRATUITO', 'PREMIUM']);

export const planSchema = z.object({
  codigo: codigoPlanSchema,
  nombre: z.string().min(1),
  precioConfigurado: z.boolean(),
  limitesConfigurados: z.boolean(),
  precioMensual: z.number().nonnegative().nullable(),
  moneda: z
    .string()
    .regex(/^[A-Z]{3}$/)
    .nullable(),
  limiteProyectos: cantidad.nullable(),
  limiteRegistros: cantidad.nullable(),
  limitePaginasIaMensuales: cantidad.nullable(),
  funciones: z.array(z.string()),
});

export const pruebaPremiumSchema = z.object({
  inicioEn: fecha,
  finEn: fecha,
  paginasIaUtilizadas: cantidad,
  limitePaginasIa: cantidad,
});

export const planesSchema = z.object({
  consultadoEn: fecha,
  planes: z.array(planSchema).min(2),
  suscripcion: z.object({
    codigoPlanActual: codigoPlanSchema,
    pruebaActiva: z.boolean(),
    puedeIniciarPrueba: z.boolean(),
    inicioEn: fecha,
    finEn: fecha.nullable(),
  }),
  uso: z.object({
    proyectos: cantidad,
    registros: cantidad,
    paginasIa: cantidad,
    limitePaginasIa: cantidad.nullable(),
    periodoIa: z.enum(['MENSUAL', 'PRUEBA']),
    inicioPeriodoIa: fecha,
    finPeriodoIa: fecha,
  }),
  prueba: pruebaPremiumSchema.nullable(),
});

export type Plan = z.infer<typeof planSchema>;
export type Planes = z.infer<typeof planesSchema>;
export type PruebaPremium = z.infer<typeof pruebaPremiumSchema>;

export const catalogoPlanesSchema = z.array(planSchema).min(2);
