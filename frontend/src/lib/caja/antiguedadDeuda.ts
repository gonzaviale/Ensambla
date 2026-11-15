import type { VentaCaja } from '@/types/caja';

const fechaNegocio = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'America/Argentina/Buenos_Aires',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
});

function diaDelNegocio(instante: Date) {
  const partes = fechaNegocio.formatToParts(instante);
  const leer = (tipo: Intl.DateTimeFormatPartTypes) =>
    Number(partes.find((parte) => parte.type === tipo)?.value);
  return Date.UTC(leer('year'), leer('month') - 1, leer('day'));
}

export function esDeudaAntigua(venta: VentaCaja, hoy = new Date()) {
  if ((venta.saldo ?? 0) <= 0) return false;
  const dias = (diaDelNegocio(hoy) - diaDelNegocio(new Date(venta.creadoEn))) / 86_400_000;
  return dias >= 30;
}
