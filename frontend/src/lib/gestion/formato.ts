import { formatoPesos } from '@/lib/sistema/formato';

/** Pesos sin centavos: «$ 59.195». */
export const pesos = (valor: number) => formatoPesos(Math.round(valor));

/** «0,237» como «24 %». */
export const porcentaje = (valor: number) => `${Math.round(valor * 100)} %`;

/** Lo que escribe la persona en un campo de plata: «150.000» o «$ 150000» → 150000. */
export const montoDe = (texto: string) => Number(texto.replace(/\D/g, '')) || 0;

/** Un monto para mostrar dentro de un campo de texto: 150000 → «150.000»; vacío si es cero. */
export const conPuntos = (monto: number) =>
  monto > 0 ? Math.round(monto).toLocaleString('es-AR') : '';
