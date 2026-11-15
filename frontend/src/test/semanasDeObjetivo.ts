import type { Hizo, SemanaObjetivo } from '@/types/objetivos';

type ManualContado = NonNullable<SemanaObjetivo['manuales']>[number];

export const manualContado = (titulo: string, hizo: Hizo): ManualContado => ({
  miniObjetivoId: titulo,
  titulo,
  hizo,
  quePaso: hizo === 'SI' ? 'FUNCIONO' : 'OTRA',
});

export const semanaCerrada = (
  lunes: string,
  manuales: SemanaObjetivo['manuales'],
): SemanaObjetivo => ({
  semana: lunes,
  resultado: 0,
  avance: 0,
  decisiones: { aplicadas: 0, descartadas: 0, aceptadas: 0, ignoradas: 0 },
  queFunciono: 'Cerró la semana.',
  textoConIa: false,
  manuales,
  reajustes: [],
});
