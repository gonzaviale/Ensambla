import { z } from 'zod';
import type { Zona } from '@/lib/sistema/zona';
import { puntoSchema } from '@/types/ubicacion';
import { accesoContenidoSchema } from './contenidoPlan';

const productoDeZonaSchema = z.object({
  nombre: z.string(),
  comercios: z.number().int().min(0),
  precio: z.object({ minimo: z.number(), promedio: z.number(), maximo: z.number() }),
  ventas: z.object({ comercios: z.number().int().min(0), unidadesPorSemana: z.number().min(0) }),
});

// Lo que devuelve el servidor: datos agregados y anónimos. Ningún comercio aparece solo ni con su lugar.
export const zonaSchema = z.object({
  nombre: z.string(),
  comercios: z.number().int().min(0),
  productos: z.array(productoDeZonaSchema),
  acceso: accesoContenidoSchema.nullish(),
}) satisfies z.ZodType<Zona, z.ZodTypeDef, unknown>;

/**
 * Qué pantalla de «Mi zona» pide los datos. Cada una ordena los productos a su manera y gasta su propio cupo del plan
 * gratuito: «precios» muestra primero lo que ya tenés y «cerca» lo más vendido que todavía no tenés.
 */
export type VistaDeZona = 'precios' | 'cerca';

/**
 * Lo que la persona pide para ver otra zona: un punto del mapa (el servidor busca la zona que lo contiene)
 * o, si no hay punto, un barrio o una dirección escritos.
 */
export const pedidoDeAnalisisSchema = z.union([
  puntoSchema,
  z.object({ lugar: z.string().trim().min(2).max(120) }),
]);

export type PedidoDeAnalisis = z.infer<typeof pedidoDeAnalisisSchema>;

export const factorDondeAbrirSchema = z.enum([
  'COMPETENCIA',
  'ACTIVIDAD_ACCESO',
  'DENSIDAD',
  'ESCUELAS',
  'OFICINAS',
  'TRANSPORTE',
  'PARQUES',
  'SALUD',
  'COMPLEMENTARIOS',
  'PERFIL_SOCIOECONOMICO',
  'DEMANDA_ENSAMBLA',
  'ALQUILER',
]);

const fuenteDondeAbrirSchema = z.object({
  codigo: z.string(),
  nombre: z.string(),
  tipo: z.string(),
  url: z.string().url().nullable(),
  licencia: z.string().nullable(),
});

const puntoDondeAbrirSchema = z.object({
  clave: z.string(),
  tipo: z.enum([
    'COMPETENCIA',
    'ESCUELA',
    'OFICINA',
    'TRANSPORTE',
    'PARQUE',
    'SALUD',
    'COMPLEMENTARIO',
  ]),
  rubro: z.string().nullable(),
  nombre: z.string(),
  latitud: z.number(),
  longitud: z.number(),
  fuente: fuenteDondeAbrirSchema,
  observadaEn: z.string(),
});

const aporteDondeAbrirSchema = z.object({
  factor: factorDondeAbrirSchema,
  etiqueta: z.string(),
  valor: z.number(),
  unidad: z.string(),
  peso: z.number().min(0).max(1),
  puntaje: z.number().min(0).max(10),
  aporte: z.number().min(0).max(10),
  fuente: fuenteDondeAbrirSchema,
  observadaEn: z.string().nullable(),
});

const zonaBaseDondeAbrirSchema = z.object({
  id: z.string().uuid(),
  nombre: z.string(),
  localidad: z.string(),
  jurisdiccion: z.enum(['CABA', 'La Matanza']),
  latitud: z.number(),
  longitud: z.number(),
  distanciaKm: z.number().nullable(),
});

export const dondeAbrirSchema = z.object({
  negocioActual: z.object({
    nombre: z.string(),
    rubro: z.string(),
    direccion: z.string().nullable(),
    latitud: z.number().nullable(),
    longitud: z.number().nullable(),
    zonaId: z.string().uuid().nullable(),
  }),
  evaluadas: z.number().int().min(0),
  ranking: z.array(
    zonaBaseDondeAbrirSchema.extend({
      puntaje: z.number().min(0).max(10),
      coincidencia: z.enum(['BUENA', 'INTERMEDIA', 'MALA']),
      aportes: z.array(aporteDondeAbrirSchema),
      porQue: z.array(z.string()),
      riesgos: z.array(z.string()),
      puntos: z.array(puntoDondeAbrirSchema),
    }),
  ),
  descartadas: z.array(zonaBaseDondeAbrirSchema.extend({ motivos: z.array(z.string()) })),
  avisos: z.array(z.string()),
});

export type DondeAbrirResponse = z.infer<typeof dondeAbrirSchema>;

export type PedidoDondeAbrir = {
  rubro: 'ALMACEN' | 'PAPELERIA' | 'KIOSCO';
  publico: 'VECINOS' | 'ESTUDIANTES' | 'OFICINAS';
  importancia: Partial<Record<z.infer<typeof factorDondeAbrirSchema>, 0 | 1 | 2>>;
  limites: {
    distanciaMaxKm?: number;
    competenciaMax?: number;
    escuelasMin?: number;
    parquesMin?: number;
    transporteMin?: number;
    alquilerMax?: number;
  };
  alquileres: { zonaId: string; valor: number }[];
};
