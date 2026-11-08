import type { AvisoEconomico } from '@/types/avisoEconomico';

// Mientras el backend no calcula la rentabilidad por período, el inicio muestra este aviso de ejemplo (el mismo del
// Laboratorio de decisiones), marcado como «Ejemplo» para que no se confunda con los números reales del cuaderno.
export const AVISO_ECONOMICO_DE_EJEMPLO: AvisoEconomico = {
  prioridad: 'ALTA',
  categoria: 'Rentabilidad',
  titulo: 'Las ventas crecieron 12 %, pero el resultado bruto bajó 4 %',
  explicacion:
    'El costo de los productos aumentó más que los precios. Aceite, leche y yerba explican la mayor parte de la diferencia.',
  variacionVentas: 12,
  variacionResultadoBruto: -4,
  productos: ['Aceite', 'Leche', 'Yerba'],
  confianza: 'ALTA',
};
