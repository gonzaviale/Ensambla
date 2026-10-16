const formatoPesos = new Intl.NumberFormat('es-AR', {
  style: 'currency',
  currency: 'ARS',
  maximumFractionDigits: 0,
});

const formatoPesosFino = new Intl.NumberFormat('es-AR', {
  style: 'currency',
  currency: 'ARS',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

/** $ 2.450 — sin centavos, para precios de góndola y resúmenes. */
export const pesos = (importe: number) => formatoPesos.format(Math.round(importe));

/** $ 2.150,50 — con centavos, para costos por unidad que salen de dividir un bulto. */
export const pesosConCentavos = (importe: number) => formatoPesosFino.format(importe);

/** Un costo: sin centavos si es redondo, con centavos si no. */
export const costo = (importe: number) =>
  Number.isInteger(importe) ? pesos(importe) : pesosConCentavos(importe);

/** 8 % — entero por defecto; el signo sólo si se pide. */
export function porcentaje(valor: number, { signo = false, decimales = 0 } = {}): string {
  const texto = Math.abs(valor).toLocaleString('es-AR', {
    minimumFractionDigits: decimales,
    maximumFractionDigits: decimales,
  });
  const prefijo = valor < 0 ? '−' : signo && valor > 0 ? '+' : '';
  return `${prefijo}${texto} %`;
}

/** «viernes» o «15 de octubre». */
export function nombreDelDia(iso: string | Date): string {
  return new Date(iso).toLocaleDateString('es-AR', { weekday: 'long' });
}

export function fechaLarga(iso: string | Date): string {
  return new Date(iso).toLocaleDateString('es-AR', { day: 'numeric', month: 'long' });
}

export function fechaCorta(iso: string | Date): string {
  return new Date(iso).toLocaleDateString('es-AR', { day: 'numeric', month: 'short' });
}

/** «1 producto» / «31 productos». */
export const plural = (cantidad: number, singular: string, plural: string) =>
  `${cantidad} ${cantidad === 1 ? singular : plural}`;
