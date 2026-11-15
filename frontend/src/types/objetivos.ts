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

// --- Objetivos que se autogestionan (H-52) ---
// Los mismos contratos que el backend: importes en pesos, semanas por su lunes (YYYY-MM-DD).

export const tipoMiniObjetivoSchema = z.enum([
  'SUBIR_PRECIO',
  'MOVER_PARADO',
  'PONER_A_LA_VISTA',
  'RECORTAR_GASTO',
  'MEJORAR_MARGEN',
  'VENTAS_SEMANALES',
]);

export const estadoMiniObjetivoSchema = z.enum([
  'EN_CURSO',
  'LOGRADO',
  'ABANDONADO',
  'REEMPLAZADO',
]);

const sentidoSchema = z.enum(['SUBE', 'BAJA']);

export const miniObjetivoSchema = z.object({
  id: z.string(),
  posicion: z.number().int(),
  tipo: tipoMiniObjetivoSchema,
  sentido: sentidoSchema,
  estado: estadoMiniObjetivoSchema,
  titulo: z.string(),
  justificacion: z.string(),
  referencia: z.string().nullable(),
  registroId: z.string().nullable(),
  valorInicial: z.number(),
  valorActual: z.number(),
  meta: z.number(),
  /** De 0 a 1. */
  avance: z.number(),
  aporteEstimado: z.number(),
  semanaInicio: z.string(),
  textoConIa: z.boolean(),
});

export const miniObjetivosSchema = z.object({
  minis: z.array(miniObjetivoSchema),
  historial: z.array(miniObjetivoSchema),
  faltanDatos: z.enum(['POCOS_DATOS', 'OBJETIVO_ALCANZADO']).nullable(),
});

export const candidatoMiniObjetivoSchema = z.object({
  tipo: tipoMiniObjetivoSchema,
  sentido: sentidoSchema,
  titulo: z.string(),
  justificacion: z.string(),
  referencia: z.string().nullable(),
  registroId: z.string().nullable(),
  valorInicial: z.number(),
  meta: z.number(),
  aporteEstimado: z.number(),
});

export const candidatosMiniObjetivoSchema = z.array(candidatoMiniObjetivoSchema);

const hitoSchema = z.enum(['OBJETIVO_ALCANZADO', 'MINI_LOGRADO', 'SIN_AVANCE']).nullable();

const puntoSemanalSchema = z.object({
  semana: z.string(),
  valor: z.number(),
  /** Calculado con los cuadernos, de antes de que Sami empezara a medir. */
  estimado: z.boolean(),
});

export const progresoObjetivoSchema = z.object({
  meta: z.number(),
  hito: hitoSchema,
  semanas: z.array(puntoSemanalSchema),
  minis: z.array(
    z.object({ mini: miniObjetivoSchema, hito: hitoSchema, semanas: z.array(puntoSemanalSchema) }),
  ),
});

export const tipoDecisionSchema = z.enum([
  'AVISO_APLICADO',
  'AVISO_DESCARTADO',
  'PASO_ACEPTADO',
  'PASO_IGNORADO',
]);

export const decisionObjetivoSchema = z.object({
  id: z.string(),
  tipo: tipoDecisionSchema,
  miniObjetivoId: z.string().nullable(),
  avisoId: z.string().nullable(),
  registroId: z.string().nullable(),
  descripcion: z.string(),
  tomadaEn: z.string(),
  semanaInicio: z.string(),
  efecto: z.enum(['MEDIDO', 'MIDIENDO', 'SIN_MEDICION']),
  deltaIngresoSemanal: z.number().nullable().optional(),
});

export const decisionesObjetivoSchema = z.object({
  semanas: z.array(
    z.object({ semanaInicio: z.string(), decisiones: z.array(decisionObjetivoSchema) }),
  ),
});

const miniDeReajusteSchema = z.object({ titulo: z.string(), meta: z.number() });

export const semanasObjetivoSchema = z.object({
  ultimoCierre: z.string().nullable(),
  semanas: z.array(
    z.object({
      semana: z.string(),
      resultado: z.number(),
      avance: z.number(),
      decisiones: z.object({
        aplicadas: z.number(),
        descartadas: z.number(),
        aceptadas: z.number(),
        ignoradas: z.number(),
      }),
      queFunciono: z.string(),
      textoConIa: z.boolean(),
      reajustes: z.array(
        z.object({
          tipo: z.enum(['SUBIR_META', 'REEMPLAZAR', 'PROBAR_OTRO_CAMINO']),
          motivo: z.string(),
          de: miniDeReajusteSchema,
          a: miniDeReajusteSchema.nullable(),
        }),
      ),
    }),
  ),
});

export type TipoMiniObjetivo = z.infer<typeof tipoMiniObjetivoSchema>;
export type MiniObjetivo = z.infer<typeof miniObjetivoSchema>;
export type MiniObjetivos = z.infer<typeof miniObjetivosSchema>;
export type CandidatoMiniObjetivo = z.infer<typeof candidatoMiniObjetivoSchema>;
export type ProgresoObjetivo = z.infer<typeof progresoObjetivoSchema>;
export type TipoDecision = z.infer<typeof tipoDecisionSchema>;
export type DecisionObjetivo = z.infer<typeof decisionObjetivoSchema>;
export type DecisionesObjetivo = z.infer<typeof decisionesObjetivoSchema>;
export type SemanasObjetivo = z.infer<typeof semanasObjetivoSchema>;
export type SemanaObjetivo = SemanasObjetivo['semanas'][number];
export type DecisionDePaso = 'ACEPTADO' | 'IGNORADO';
