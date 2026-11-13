import type { Publico } from './dondeAbrir';

export type Alcance = 'cerca' | 'toda';
export type Prioridad = 'equilibrado' | 'escuelas';
/** Tamaño aproximado del local; de ahí sale qué trámite le corresponde. */
export type Tamano = 'chico' | 'mediano' | 'grande' | 'muy-grande';

export type RespuestasDeSami = {
  alcance: Alcance;
  prioridad: Prioridad;
  publico: Publico;
  tamano: Tamano;
};

/** Superficie que se supone para cada tamaño, en m². */
export const SUPERFICIE_DE_TAMANO: Record<Tamano, number> = {
  chico: 50,
  mediano: 120,
  grande: 350,
  'muy-grande': 600,
};
