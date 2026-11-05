import { formatoPesos } from '@/lib/sistema/formato';

/** Pesos sin centavos: «$ 59.195». */
export const pesos = (valor: number) => formatoPesos(Math.round(valor));

/** «0,237» como «24 %». */
export const porcentaje = (valor: number) => `${Math.round(valor * 100)} %`;
