import type { AutoriaDeMetrica, DefinicionDeMetrica } from '@/types/metricas';

/** Lo que recibe cada pestaña del panel para agregar: el cuaderno y cómo guardar la métrica elegida. */
export type PropsDeLaPestana = {
  sistemaId: string;
  guardando: boolean;
  onAgregar: (definicion: DefinicionDeMetrica, autoria: AutoriaDeMetrica) => void;
};
