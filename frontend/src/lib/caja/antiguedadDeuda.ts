import { diasDeCalendarioEntre } from '@/lib/diaDelNegocio';
import type { VentaCaja } from '@/types/caja';

const DIAS_PARA_SER_ANTIGUA = 30;

export function esDeudaAntigua(venta: VentaCaja, hoy = new Date()) {
  if ((venta.saldo ?? 0) <= 0) return false;
  return diasDeCalendarioEntre(new Date(venta.creadoEn), hoy) >= DIAS_PARA_SER_ANTIGUA;
}
