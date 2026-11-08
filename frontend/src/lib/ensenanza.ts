// Las lecciones de «Sami te enseña». El orden es el del índice.

export const LECCIONES_DE_SAMI = [
  'resultado',
  'metricas',
  'recomendaciones',
  'escenario',
  'consejos',
] as const;

export type LeccionDeSami = (typeof LECCIONES_DE_SAMI)[number];

export const DATOS_DE_LECCION: Record<
  LeccionDeSami,
  { titulo: string; resumen: string; minutos: number }
> = {
  resultado: {
    titulo: 'Cómo se forma tu resultado',
    resumen: 'Vender, tener margen y ganar no son lo mismo. Mirá de dónde sale cada importe.',
    minutos: 3,
  },
  metricas: {
    titulo: 'Tus métricas, una por una',
    resumen: 'Margen, punto de equilibrio, ventas fiadas y calidad del análisis, con su cuenta.',
    minutos: 4,
  },
  recomendaciones: {
    titulo: 'Cómo recomienda Sami',
    resumen: 'Qué datos usa, cómo arma los caminos, cuánto confía y qué nunca hace.',
    minutos: 4,
  },
  escenario: {
    titulo: 'Probá un escenario',
    resumen:
      'Movés ventas, margen y gastos, y ves qué pasaría con tus objetivos sin tocar tus datos.',
    minutos: 5,
  },
  consejos: {
    titulo: 'Sugerencias para usar Ensambla',
    resumen: 'Hábitos simples para que las cuentas de Sami sean cada vez más confiables.',
    minutos: 2,
  },
};

export function esLeccion(valor: unknown): valor is LeccionDeSami {
  return LECCIONES_DE_SAMI.some((leccion) => leccion === valor);
}
