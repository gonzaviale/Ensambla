import {
  agrupacionesDe,
  calculosDe,
  periodosDe,
  permiteFiado,
} from '@/lib/metricas/metricasAMedida';
import type { DefinicionDeMetrica, FuenteDeMetrica } from '@/types/metricas';

// Las mismas reglas que DefinicionMetrica.validar del servidor, con los mismos motivos.

const FILAS_DE: Record<FuenteDeMetrica, string> = {
  VENTAS: 'ventas',
  GASTOS: 'gastos',
  INVENTARIO: 'productos',
};

const REGLAS: {
  cumple: (receta: DefinicionDeMetrica) => boolean;
  motivo: (receta: DefinicionDeMetrica) => string;
}[] = [
  {
    cumple: (receta) => calculosDe(receta.fuente).includes(receta.calculo),
    motivo: () => 'Los gastos no tienen unidades: elegí sumar, promediar o contar',
  },
  {
    cumple: (receta) => agrupacionesDe(receta.fuente).includes(receta.agrupar),
    motivo: (receta) => `No se puede repartir ${FILAS_DE[receta.fuente]} de esa manera`,
  },
  {
    cumple: (receta) => !receta.soloFiado || permiteFiado(receta.fuente),
    motivo: () => 'Sólo las ventas se pueden marcar como fiadas',
  },
  {
    cumple: (receta) => periodosDe(receta.fuente).includes(receta.periodo),
    motivo: () => 'El stock es lo que hay hoy: no tiene períodos',
  },
];

/** Por qué no se puede calcular la receta, o {@code null} si se puede. */
export const motivoParaRechazar = (receta: DefinicionDeMetrica) =>
  REGLAS.find((regla) => !regla.cumple(receta))?.motivo(receta) ?? null;

/** Cómo se nombra a lo que se cuenta: «Todavía no cargaste gastos en este negocio». */
export const filasDe = (fuente: FuenteDeMetrica) => FILAS_DE[fuente];
