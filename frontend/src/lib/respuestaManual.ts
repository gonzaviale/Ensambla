import type { Hizo, QuePaso } from '@/types/objetivos';

// Las respuestas rápidas de un mini-objetivo manual, en las palabras de la persona.

export const HIZO: readonly { valor: Hizo; etiqueta: string }[] = [
  { valor: 'SI', etiqueta: 'Sí' },
  { valor: 'EN_PARTE', etiqueta: 'En parte' },
  { valor: 'NO', etiqueta: 'No' },
];

export const QUE_PASO: readonly { valor: QuePaso; etiqueta: string }[] = [
  { valor: 'FUNCIONO', etiqueta: 'Funcionó' },
  { valor: 'NO_TUVE_TIEMPO', etiqueta: 'No tuve tiempo' },
  { valor: 'NO_FUNCIONO', etiqueta: 'No funcionó' },
  { valor: 'OTRA', etiqueta: 'Otra' },
];

/** Si lo hiciste, tuviste tiempo: «no tuve tiempo» sólo va con «en parte» o «no». */
export const quePasoPosible = (hizo: Hizo, quePaso: QuePaso): boolean =>
  !(hizo === 'SI' && quePaso === 'NO_TUVE_TIEMPO');
