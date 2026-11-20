import {
  agrupacionesDe,
  calculosDe,
  cuentaCadaProducto,
  periodosDe,
  permiteFiado,
} from '@/lib/metricas/metricasAMedida';
import type { CalculoDeMetrica, DefinicionDeMetrica, FuenteDeMetrica } from '@/types/metricas';

// Las mismas reglas que DefinicionMetrica.validar del servidor, con los mismos motivos.

const FILAS_DE: Record<FuenteDeMetrica, string> = {
  VENTAS: 'ventas',
  GASTOS: 'gastos',
  INVENTARIO: 'productos',
};

// Lo mismo que CalculoMetrica.motivoSiNoSirvePara; sólo las cuentas que no sirven para alguna tabla tienen el suyo.
const MOTIVO_SI_NO_SIRVE: Record<CalculoDeMetrica, string> = {
  SUMA: 'Esa cuenta no se puede hacer con esa tabla',
  PROMEDIO: 'Esa cuenta no se puede hacer con esa tabla',
  CONTEO: 'Esa cuenta no se puede hacer con esa tabla',
  UNIDADES: 'Los gastos no tienen unidades: elegí sumar, promediar o contar',
  GANANCIA: 'La ganancia se calcula con tus ventas: elegí ventas',
  COSTO: 'El costo de lo vendido se calcula con tus ventas: elegí ventas',
};

const REGLAS: {
  cumple: (receta: DefinicionDeMetrica) => boolean;
  motivo: (receta: DefinicionDeMetrica) => string;
}[] = [
  {
    cumple: (receta) => calculosDe(receta.fuente).includes(receta.calculo),
    motivo: (receta) => MOTIVO_SI_NO_SIRVE[receta.calculo],
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
  {
    cumple: (receta) => !cuentaCadaProducto(receta),
    motivo: () =>
      'Cada producto es uno solo: contarlos por producto siempre da 1. Elegí la cantidad o el valor al costo',
  },
];

/** Por qué no se puede calcular la receta, o {@code null} si se puede. */
export const motivoParaRechazar = (receta: DefinicionDeMetrica) =>
  REGLAS.find((regla) => !regla.cumple(receta))?.motivo(receta) ?? null;

/** Cómo se nombra a lo que se cuenta: «Todavía no cargaste gastos en este negocio». */
export const filasDe = (fuente: FuenteDeMetrica) => FILAS_DE[fuente];
