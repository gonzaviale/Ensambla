import { z } from 'zod';

/** Cómo se mide un objetivo: con una meta en pesos, con datos del negocio, sin datos o mezclado. */
export const tipoObjetivoSchema = z.enum(['MONETARIO', 'CON_DATOS', 'SIN_DATOS', 'HIBRIDO']);

// Un objetivo guardado en el servidor: lo que la persona le contó a Sami, su tipo y la meta mensual, que sólo tiene el
// monetario (sin importe no hay meta). El historial de avance no viaja: lo calcula el frontend con lo que hay en las
// secciones.
export const objetivoDelServidorSchema = z.object({
  id: z.string(),
  texto: z.string(),
  tipo: tipoObjetivoSchema,
  meta: z.number().nullable(),
  armadoConSami: z.boolean(),
  creadoEn: z.string(),
});

export const objetivosDelServidorSchema = z.array(objetivoDelServidorSchema);

export type TipoObjetivo = z.infer<typeof tipoObjetivoSchema>;
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

const miniBaseSchema = {
  id: z.string(),
  posicion: z.number().int(),
  estado: estadoMiniObjetivoSchema,
  titulo: z.string(),
  justificacion: z.string(),
  semanaInicio: z.string(),
  textoConIa: z.boolean(),
};

/** Lo mide el servidor: tiene una métrica, un valor de partida, el de hoy y una meta. */
export const miniMedidoSchema = z.object({
  ...miniBaseSchema,
  modo: z.literal('MEDIDO'),
  tipo: tipoMiniObjetivoSchema,
  sentido: sentidoSchema,
  referencia: z.string().nullable(),
  registroId: z.string().nullable(),
  valorInicial: z.number(),
  valorActual: z.number(),
  meta: z.number(),
  /** De 0 a 1. */
  avance: z.number(),
  aporteEstimado: z.number(),
});

/** Lo responde la persona: no tiene números, sólo la acción. */
export const miniManualSchema = z.object({ ...miniBaseSchema, modo: z.literal('MANUAL') });

export const miniObjetivoSchema = z.discriminatedUnion('modo', [
  miniMedidoSchema,
  miniManualSchema,
]);

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
  /** Calculado con las secciones, de antes de que Sami empezara a medir. */
  estimado: z.boolean(),
});

export const progresoObjetivoSchema = z.object({
  /** Sin meta en pesos no hay contra qué comparar el resultado. */
  meta: z.number().nullable(),
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
  'MINI_HECHO',
  'MINI_EN_PARTE',
  'MINI_NO_HECHO',
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

const miniDeReajusteSchema = z.object({ titulo: z.string(), meta: z.number().nullable() });

export const hizoSchema = z.enum(['SI', 'EN_PARTE', 'NO']);
export const quePasoSchema = z.enum(['FUNCIONO', 'NO_TUVE_TIEMPO', 'NO_FUNCIONO', 'OTRA']);

/** Un manual de una semana que ya cerró; si la persona no contó cómo le fue, el servidor manda `hizo` y `quePaso` vacíos. */
const manualDeLaSemanaSchema = z.object({
  miniObjetivoId: z.string(),
  titulo: z.string(),
  hizo: hizoSchema.nullable(),
  quePaso: quePasoSchema.nullable(),
});

type ManualDeLaSemana = z.infer<typeof manualDeLaSemanaSchema>;
type ManualContado = ManualDeLaSemana & { hizo: z.infer<typeof hizoSchema> };

const estaContado = (manual: ManualDeLaSemana): manual is ManualContado => manual.hizo !== null;

const soloLosContados = (manuales: ManualDeLaSemana[]): ManualContado[] =>
  manuales.filter(estaContado);

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
      /** Lo que la persona contó de sus manuales esa semana; sin manuales (o sin respuestas) queda vacío. */
      manuales: z.array(manualDeLaSemanaSchema).transform(soloLosContados).optional(),
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
export type MiniMedido = z.infer<typeof miniMedidoSchema>;
export type MiniManual = z.infer<typeof miniManualSchema>;
export type MiniObjetivos = z.infer<typeof miniObjetivosSchema>;
export type CandidatoMiniObjetivo = z.infer<typeof candidatoMiniObjetivoSchema>;
export type ProgresoObjetivo = z.infer<typeof progresoObjetivoSchema>;
export type TipoDecision = z.infer<typeof tipoDecisionSchema>;
export type DecisionObjetivo = z.infer<typeof decisionObjetivoSchema>;
export type DecisionesObjetivo = z.infer<typeof decisionesObjetivoSchema>;
export type SemanasObjetivo = z.infer<typeof semanasObjetivoSchema>;
export type SemanaObjetivo = SemanasObjetivo['semanas'][number];
export type DecisionDePaso = 'ACEPTADO' | 'IGNORADO';

// --- Armar un objetivo conversando con Sami (H-60) ---

/** Cuántos objetivos con IA lleva el negocio en el mes y cuántos le quedan según su plan. */
export const cuotaObjetivosSchema = z.object({
  usadas: z.number().int().nonnegative(),
  limite: z.number().int().nonnegative(),
  restantes: z.number().int().nonnegative(),
});

/** Así la manda el servidor: un plan sin tope trae `limite` y `restantes` vacíos y entonces no hay cuota que mostrar. */
const cuotaDelServidorSchema = z
  .object({
    usadas: z.number().int().nonnegative(),
    limite: z.number().int().nonnegative().nullable(),
    restantes: z.number().int().nonnegative().nullable(),
  })
  .transform((cuota) =>
    cuota.limite === null || cuota.restantes === null
      ? null
      : { usadas: cuota.usadas, limite: cuota.limite, restantes: cuota.restantes },
  );

export const fuenteDeDatosSchema = z.enum(['VENTAS', 'MARGEN', 'INVENTARIO', 'GASTOS', 'ZONA']);

/** Un mini-objetivo que Sami propone: medido (con los números del servidor) o manual (sólo la acción). */
export const miniPropuestoSchema = z.object({
  modo: z.enum(['MEDIDO', 'MANUAL']),
  tipo: tipoMiniObjetivoSchema.nullable(),
  registroId: z.string().nullable(),
  referencia: z.string().nullable(),
  titulo: z.string(),
  justificacion: z.string(),
  valorInicial: z.number().nullable(),
  meta: z.number().nullable(),
  aporteEstimado: z.number().nullable(),
  textoConIa: z.boolean(),
});

export const propuestaObjetivoSchema = z.object({
  /** Lo que Sami le dice a la persona. */
  respuesta: z.string(),
  /** Cómo entendió el objetivo; el servidor manda `null` si todavía necesita aclarar, y acá queda vacío. */
  entendido: z
    .string()
    .nullish()
    .transform((texto) => texto ?? ''),
  /** El tipo que quedaría al guardar; null si no hay mini-objetivos o es una meta en pesos. */
  tipo: tipoObjetivoSchema.nullable(),
  esMonetario: z.boolean(),
  minis: z.array(miniPropuestoSchema),
  datos: z.array(z.object({ fuente: fuenteDeDatosSchema, hay: z.boolean() })),
  origen: z.enum(['IA', 'RESPALDO']),
  mensajesRestantes: z.number().int().nonnegative(),
  puedeGuardar: z.boolean(),
  /** La cuota del mes; vacía si el plan no tiene tope. */
  cuota: cuotaDelServidorSchema.nullish(),
});

export type TipoMiniObjetivoPropuesto = z.infer<typeof miniPropuestoSchema>;
export type PropuestaObjetivo = z.infer<typeof propuestaObjetivoSchema>;
export type CuotaObjetivos = z.infer<typeof cuotaObjetivosSchema>;
export type FuenteDeDatos = z.infer<typeof fuenteDeDatosSchema>;

export type MensajeDeConversacion = { rol: 'PERSONA' | 'SAMI'; texto: string };

/** Lo que la persona aceptó: de un medido sólo se manda cuál es (el servidor recalcula); el manual lleva su texto. */
export type MiniParaGuardar =
  | { modo: 'MEDIDO'; tipo: TipoMiniObjetivo; registroId: string | null; referencia: string | null }
  | { modo: 'MANUAL'; titulo: string; justificacion: string };

export type ObjetivoConSami = {
  texto: string;
  seguimientoManual?: boolean;
  minis: MiniParaGuardar[];
};

// --- Responder un mini-objetivo manual (H-60) ---

export type Hizo = z.infer<typeof hizoSchema>;
export type QuePaso = z.infer<typeof quePasoSchema>;

/** La respuesta de la semana: se manda una vez y no se pisa. El comentario es opcional (hasta 300 caracteres). */
export type RespuestaDeMini = { hizo: Hizo; quePaso: QuePaso; comentario?: string };

export const respuestaMiniObjetivoSchema = z.object({
  id: z.string(),
  miniObjetivoId: z.string(),
  hizo: hizoSchema,
  quePaso: quePasoSchema,
  comentario: z.string().nullish(),
  semanaInicio: z.string(),
  respondidaEn: z.string(),
});

/** Un manual en curso sin respuesta, con el objetivo al que pertenece; del más viejo al más nuevo. */
export const respuestaPendienteSchema = z.object({
  objetivoId: z.string(),
  objetivoTexto: z.string(),
  miniObjetivo: miniManualSchema.extend({ justificacion: z.string().default('') }),
});

export const respuestasPendientesSchema = z.array(respuestaPendienteSchema);

export type RespuestaMiniObjetivo = z.infer<typeof respuestaMiniObjetivoSchema>;
export type RespuestaPendiente = z.infer<typeof respuestaPendienteSchema>;
