import { z } from 'zod';

// Las métricas a medida de un cuaderno. Lo que se guarda es la receta (qué tabla, qué cálculo, cómo repartir y de qué
// período); el número lo calcula siempre el servidor con las filas reales y viene con su «de dónde sale».
export const FUENTES_DE_METRICA = ['VENTAS', 'GASTOS', 'INVENTARIO'] as const;
export const CALCULOS_DE_METRICA = ['SUMA', 'PROMEDIO', 'CONTEO', 'UNIDADES'] as const;
export const AGRUPACIONES_DE_METRICA = [
  'NINGUNA',
  'PRODUCTO',
  'CLIENTE',
  'CATEGORIA',
  'DIA_DE_LA_SEMANA',
] as const;
export const PERIODOS_DE_METRICA = ['HOY', 'SEMANA', 'MES', 'TODO'] as const;
/** Cómo se dibuja una métrica en su solapa. */
export const GRAFICOS_DE_METRICA = ['NUMERO', 'BARRAS', 'DONA', 'LINEA'] as const;

export type FuenteDeMetrica = (typeof FUENTES_DE_METRICA)[number];
export type CalculoDeMetrica = (typeof CALCULOS_DE_METRICA)[number];
export type AgrupacionDeMetrica = (typeof AGRUPACIONES_DE_METRICA)[number];
export type PeriodoDeMetrica = (typeof PERIODOS_DE_METRICA)[number];
export type GraficoDeMetrica = (typeof GRAFICOS_DE_METRICA)[number];

export const definicionDeMetricaSchema = z.object({
  nombre: z.string(),
  fuente: z.enum(FUENTES_DE_METRICA),
  calculo: z.enum(CALCULOS_DE_METRICA),
  agrupar: z.enum(AGRUPACIONES_DE_METRICA),
  periodo: z.enum(PERIODOS_DE_METRICA),
  soloFiado: z.boolean(),
});

export type DefinicionDeMetrica = z.infer<typeof definicionDeMetricaSchema>;

export const resultadoDeMetricaSchema = z.object({
  formato: z.enum(['MONEDA', 'NUMERO']),
  /** El número total; llega vacío si no se pudo calcular (nunca un cero inventado). */
  valor: z.number().nullish(),
  /** El reparto (por producto, cliente, categoría o día); vacío si la métrica no se reparte. */
  filas: z.array(z.object({ etiqueta: z.string(), valor: z.number() })),
  /** Por qué no hay número, en palabras de la persona. */
  motivo: z.string().nullish(),
  deDondeSale: z.object({
    fuente: z.enum(FUENTES_DE_METRICA),
    filasUsadas: z.number().int(),
    filasSinDato: z.number().int(),
    desde: z.string().nullish(),
    hasta: z.string().nullish(),
  }),
});

export type ResultadoDeMetrica = z.infer<typeof resultadoDeMetricaSchema>;

export const metricaSchema = z.object({
  id: z.string(),
  definicion: definicionDeMetricaSchema,
  orden: z.number().int(),
  autoria: z.enum(['SUGERIDA', 'PROPIA']),
  resultado: resultadoDeMetricaSchema,
  // Opcionales: un servidor anterior no los manda y se usa el de siempre para su reparto.
  grafico: z.enum(GRAFICOS_DE_METRICA).optional(),
  graficosPosibles: z.array(z.enum(GRAFICOS_DE_METRICA)).optional(),
});

export const metricasSchema = z.array(metricaSchema);

export type Metrica = z.infer<typeof metricaSchema>;

/** Una métrica que el catálogo le sugiere al rubro, con lo que daría hoy. */
export const sugeridaSchema = z.object({
  definicion: definicionDeMetricaSchema,
  resultado: resultadoDeMetricaSchema,
});

/** En la lista de sugeridas: si ya está a la vista (guardada o en el panel de ventas), no se puede agregar. */
export const sugeridaDelRubroSchema = sugeridaSchema.extend({
  yaLaTenes: z.boolean().default(false),
});

export const sugeridasSchema = z.array(sugeridaDelRubroSchema);

export type MetricaSugerida = z.infer<typeof sugeridaSchema>;

export type SugeridaDelRubro = z.infer<typeof sugeridaDelRubroSchema>;

export const fuentesDelCuadernoSchema = z.array(z.enum(FUENTES_DE_METRICA));

/** Las partes de la receta que puede decir un pedido en palabras. */
export const PARTES_DE_LA_RECETA = [
  'FUENTE',
  'CALCULO',
  'AGRUPACION',
  'PERIODO',
  'SOLO_FIADO',
] as const;

export type ParteDeLaReceta = (typeof PARTES_DE_LA_RECETA)[number];

/** Cómo se entendió el pedido: qué dijo la persona, qué se tomó por defecto y qué palabras no se entendieron. */
export const lecturaDelPedidoSchema = z.object({
  delPedido: z.array(z.enum(PARTES_DE_LA_RECETA)),
  porDefecto: z.array(z.enum(PARTES_DE_LA_RECETA)),
  noEntendidas: z.array(z.string()),
});

export type LecturaDelPedido = z.infer<typeof lecturaDelPedidoSchema>;

/** La métrica que arma un pedido en palabras: la propuesta de siempre más cómo se lo entendió. */
// lectura y yaLaTenes son opcionales: un servidor anterior no los manda y la propuesta igual se muestra.
export const pedidoInterpretadoSchema = sugeridaSchema.extend({
  lectura: lecturaDelPedidoSchema.optional(),
  yaLaTenes: z.boolean().default(false),
});

export type PedidoInterpretado = z.infer<typeof pedidoInterpretadoSchema>;

export type AutoriaDeMetrica = Metrica['autoria'];

/** Lo que se manda para guardar: la receta y quién la eligió. */
export type NuevaMetrica = DefinicionDeMetrica & { autoria: AutoriaDeMetrica };
