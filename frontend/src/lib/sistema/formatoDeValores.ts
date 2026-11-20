import type { Valor } from './tipos';

const pesos = new Intl.NumberFormat('es-AR', {
  style: 'currency',
  currency: 'ARS',
  maximumFractionDigits: 0,
});

export const formatoPesos = (valor: Valor) => pesos.format(Number(valor) || 0);

/** «2026-10-16» como «16/10/2026»; si no es una fecha completa se deja como vino. */
export function formatoFecha(valor: Valor) {
  const partes = String(valor).split('-');
  return partes.length === 3 ? `${partes[2]}/${partes[1]}/${partes[0]}` : String(valor);
}
