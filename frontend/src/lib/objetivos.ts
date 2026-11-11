// Los objetivos del negocio y las cuentas que Sami usa para recomendar caminos. El servidor guarda el texto y la
// meta; el historial es el resultado real de las últimas semanas (GET /objetivos/negocio). Las palancas parten de la
// base económica de referencia («Almacén de barrio») y se escalan a cada meta.

export type PuntoDeProgreso = { semana: string; valor: number };

export type Objetivo = {
  id: string;
  /** Lo que la persona le contó a Sami, con sus palabras. */
  texto: string;
  /** Resultado mensual que se quiere alcanzar. */
  meta: number;
  /** Resultado mensual estimado, semana a semana; el último es el de hoy. */
  historial: PuntoDeProgreso[];
};

/** Resultado mensual de la base de referencia: sólo para escalar las palancas y para la lección de ejemplo. */
const RESULTADO_DE_REFERENCIA = 180_000;
export const META_POR_DEFECTO = 500_000;

/** Con la brecha de referencia ($500.000 − $180.000), el camino combinado pide estas palancas. */
const BRECHA_DE_REFERENCIA = META_POR_DEFECTO - RESULTADO_DE_REFERENCIA;

export const EJEMPLOS_DE_OBJETIVO = [
  'Quiero ganar $400.000 por mes sin subir demasiado los precios',
  'Quiero recuperar mis gastos antes del día 20',
  'Quiero poder retirar $150.000 por mes sin afectar el negocio',
] as const;

/**
 * Un importe escrito como lo escribe la gente: «$400.000», «$ 400,000», «$400k», «400 mil», «1,5 millones». Los
 * miles van con punto o coma cada tres cifras; con un multiplicador, uno o dos decimales («1,5 M»).
 */
const IMPORTE =
  /(\$\s*)?(\d{1,3}(?:[.,]\d{3})+|\d+)(?:[.,](\d{1,2}))?\s*(k|mil|millones|millón|millon|m)?(?![\p{L}\d])/giu;

const MULTIPLICADOR: Record<string, number> = {
  k: 1_000,
  mil: 1_000,
  m: 1_000_000,
  millon: 1_000_000,
  millón: 1_000_000,
  millones: 1_000_000,
};

/** Menos que esto no es una meta mensual: es un día, una cantidad o un porcentaje («antes del día 20»). */
const META_MINIMA = 1_000;

/**
 * Sami busca el importe del texto: primero uno con «$» o con multiplicador («$400k», «400 mil»), y si no hay, un
 * número grande suelto («400.000»). Si no encuentra ninguno razonable, usa la meta de ejemplo.
 */
export function metaDesdeTexto(texto: string): number {
  const importes = [...texto.matchAll(IMPORTE)].map((encontrado) => {
    const [, peso, entero = '', decimales = '', sufijo] = encontrado;
    const multiplicador = sufijo ? MULTIPLICADOR[sufijo.toLowerCase()] : undefined;
    const valor = Number(`${entero.replace(/[.,]/g, '')}.${decimales || '0'}`) * (multiplicador ?? 1);
    return { valor: Math.round(valor), marcado: Boolean(peso) || multiplicador !== undefined };
  });
  const razonables = importes.filter((importe) => importe.valor >= META_MINIMA);
  return (
    (razonables.find((importe) => importe.marcado) ?? razonables[0])?.valor ?? META_POR_DEFECTO
  );
}

/** Lo que se gana hoy por mes; sin historial (todavía sin ventas) es cero, nunca un número de ejemplo. */
export function ultimoValor(objetivo: Objetivo): number {
  return objetivo.historial.at(-1)?.valor ?? 0;
}

/** Qué parte de la meta ya se logra hoy, de 0 a 1. */
export function avance(objetivo: Objetivo): number {
  return Math.min(1, Math.max(0, ultimoValor(objetivo) / objetivo.meta));
}

export function brecha(objetivo: Objetivo): number {
  return Math.max(0, objetivo.meta - ultimoValor(objetivo));
}

/** Cuánto cambió el resultado desde la primera semana medida. */
export function variacion(objetivo: Objetivo): number {
  const primero = objetivo.historial[0]?.valor ?? 0;
  return ultimoValor(objetivo) - primero;
}

export function nuevoObjetivo(texto: string): Objetivo {
  return {
    id: crypto.randomUUID(),
    texto: texto.trim(),
    meta: metaDesdeTexto(texto),
    historial: [],
  };
}

export const ID_OBJETIVO_DE_EJEMPLO = 'objetivo-ejemplo';
export const TEXTO_OBJETIVO_DE_EJEMPLO =
  'Quiero ganar $500.000 por mes sin subir demasiado los precios';

const HISTORIAL_DE_EJEMPLO: PuntoDeProgreso[] = [
  120_000,
  134_000,
  141_000,
  158_000,
  171_000,
  RESULTADO_DE_REFERENCIA,
].map((valor, indice, valores) => ({
  semana: indice === valores.length - 1 ? 'Hoy' : `Hace ${valores.length - 1 - indice} sem.`,
  valor,
}));

/** El objetivo de ejemplo de las lecciones de Sami: seis semanas de historia inventada hasta hoy. */
export function objetivoDeEjemplo(): Objetivo {
  return {
    id: ID_OBJETIVO_DE_EJEMPLO,
    texto: TEXTO_OBJETIVO_DE_EJEMPLO,
    meta: META_POR_DEFECTO,
    historial: HISTORIAL_DE_EJEMPLO.map((punto) => ({ ...punto })),
  };
}

/**
 * El objetivo que guardó el servidor (solo el texto y la meta), listo para mostrar. El historial no se guarda: lo
 * pone {@link conHistorialReal} con las semanas reales del negocio.
 */
export function objetivoDesdeServidor(guardado: Pick<Objetivo, 'id' | 'texto' | 'meta'>): Objetivo {
  const { id, texto, meta } = guardado;
  return { id, texto, meta, historial: [] };
}

/** El objetivo con el resultado real de las últimas semanas; sin ventas queda sin historial. */
export function conHistorialReal(
  objetivo: Objetivo,
  semanas: readonly { semana: string; queda: number }[],
): Objetivo {
  return { ...objetivo, historial: semanas.map(({ semana, queda }) => ({ semana, valor: queda })) };
}

/* Palancas: las tres cosas que Sami mueve para cerrar la brecha. */

export type ClavePalanca = 'ventas' | 'margen' | 'gastos';

export type Palanca = {
  clave: ClavePalanca;
  etiqueta: string;
  /** Qué parte de la brecha cierra esta palanca cuando se aplica completa. */
  aporte: number;
  /** Valor recomendado cuando la brecha es la de referencia. */
  referencia: number;
  paso: number;
  formato: (valor: number) => string;
};

const pesos = new Intl.NumberFormat('es-AR');
export const formatoPesos = (valor: number) => `$${pesos.format(Math.round(valor))}`;

export const PALANCA: Record<ClavePalanca, Palanca> = {
  ventas: {
    clave: 'ventas',
    etiqueta: 'Vender más',
    aporte: 0.4,
    referencia: 12,
    paso: 1,
    formato: (valor) => `+${valor}%`,
  },
  margen: {
    clave: 'margen',
    etiqueta: 'Ganar más por venta',
    aporte: 0.25,
    referencia: 3,
    paso: 0.5,
    formato: (valor) => `+${valor} pts de margen`,
  },
  gastos: {
    clave: 'gastos',
    etiqueta: 'Gastar menos',
    aporte: 0.35,
    referencia: 80_000,
    paso: 5_000,
    formato: (valor) => `−${formatoPesos(valor)}`,
  },
};

export const PALANCAS: readonly Palanca[] = [PALANCA.ventas, PALANCA.margen, PALANCA.gastos];

/** Cuánto hay que escalar el camino de referencia para cubrir la brecha de este objetivo. */
export function factorDeEscala(objetivo: Objetivo): number {
  return brecha(objetivo) / BRECHA_DE_REFERENCIA;
}

function redondear(valor: number, paso: number) {
  return Math.round(valor / paso) * paso;
}

/** El valor de una palanca que, por sí sola o combinada, cierra su parte de la brecha. */
export function valorRecomendado(palanca: Palanca, objetivo: Objetivo): number {
  return redondear(palanca.referencia * factorDeEscala(objetivo), palanca.paso);
}

export type Valores = Record<ClavePalanca, number>;

export function valoresRecomendados(objetivo: Objetivo): Valores {
  return {
    ventas: valorRecomendado(PALANCA.ventas, objetivo),
    margen: valorRecomendado(PALANCA.margen, objetivo),
    gastos: valorRecomendado(PALANCA.gastos, objetivo),
  };
}

/**
 * Cuánto aporta cada palanca al resultado: su parte de la brecha, en proporción a cuánto de lo recomendado se
 * aplica. Con las tres al valor recomendado, el resultado llega a la meta. Es una estimación de demostración.
 */
export function aportes(objetivo: Objetivo, valores: Valores): Record<ClavePalanca, number> {
  const total = brecha(objetivo);
  const recomendados = valoresRecomendados(objetivo);
  const parte = (palanca: Palanca) =>
    recomendados[palanca.clave] > 0
      ? total * palanca.aporte * (valores[palanca.clave] / recomendados[palanca.clave])
      : 0;
  return {
    ventas: parte(PALANCA.ventas),
    margen: parte(PALANCA.margen),
    gastos: parte(PALANCA.gastos),
  };
}

export function resultadoProyectado(objetivo: Objetivo, valores: Valores): number {
  const suma = Object.values(aportes(objetivo, valores)).reduce((a, b) => a + b, 0);
  return ultimoValor(objetivo) + suma;
}

/* Los tres caminos para llegar a la meta. */

export type Camino = {
  clave: 'combinado' | 'ventas' | 'margen';
  titulo: string;
  bajada: string;
  recomendado: boolean;
  confianza: string;
  datos: { nombre: string; valor: string }[];
};

export function caminos(objetivo: Objetivo): Camino[] {
  const factor = factorDeEscala(objetivo);
  const combinado = valoresRecomendados(objetivo);
  const soloVentas = redondear(27 * factor, 1);
  const margenNecesario = Math.round(34 + 9 * factor);
  return [
    {
      clave: 'combinado',
      titulo: 'Camino combinado',
      bajada: 'Reparte el esfuerzo entre ventas, margen y gastos.',
      recomendado: true,
      confianza: 'Confianza media-alta',
      datos: [
        { nombre: 'Vender más', valor: PALANCA.ventas.formato(combinado.ventas) },
        { nombre: 'Reducir gastos', valor: PALANCA.gastos.formato(combinado.gastos) },
        { nombre: 'Mejorar margen', valor: PALANCA.margen.formato(combinado.margen) },
      ],
    },
    {
      clave: 'ventas',
      titulo: 'Vender más',
      bajada: 'Mantiene costos y margen actuales.',
      recomendado: false,
      confianza: 'Requiere revisar sus supuestos',
      datos: [
        { nombre: 'Aumento de ventas', valor: `+${soloVentas}%` },
        { nombre: 'Ventas adicionales', valor: `≈${Math.round(145 * factor)}` },
        { nombre: 'Cambio de precio', valor: '0%' },
      ],
    },
    {
      clave: 'margen',
      titulo: 'Ganar más por venta',
      bajada: 'No exige vender más unidades.',
      recomendado: false,
      confianza: 'Requiere revisar sus supuestos',
      datos: [
        { nombre: 'Margen actual', valor: '34%' },
        { nombre: 'Margen necesario', valor: `${margenNecesario}%` },
        { nombre: 'Productos a revisar', valor: `${Math.max(1, Math.round(8 * factor))}` },
      ],
    },
  ];
}
