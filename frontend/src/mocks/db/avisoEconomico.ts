import type { AvisoEconomico } from '@/types/avisoEconomico';

// El aviso económico del modo de demostración: el servidor lo calcula con las ventas reales de las dos últimas
// semanas; acá siempre es el mismo, con los textos que armaría el backend.
export const AVISO_ECONOMICO_DE_EJEMPLO: AvisoEconomico = {
  prioridad: 'ALTA',
  tendencia: 'EMPEORA',
  categoria: 'Rentabilidad',
  titulo: 'Las ventas subieron 12 %, pero la ganancia bajó 4 %',
  explicacion:
    'Vendiste más, pero te quedó menos ganancia: revisá los precios de los productos de abajo.',
  variacionVentas: 12,
  variacionResultadoBruto: -4,
  productos: ['Aceite', 'Leche', 'Yerba'],
  confianza: 'ALTA',
};
