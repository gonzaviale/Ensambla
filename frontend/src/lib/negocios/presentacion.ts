import type { Color } from '@/lib/sistema/tipos';
import type { ResumenDeNegocio } from '@/types/negocios';

// Cómo se dice cada negocio de la cuenta en pantalla (ENS-284). Los datos salen del resumen conjunto, que junta el
// panel de cada cuaderno (ver `lib/negocios/lista`).

/** Lo que hace falta de un negocio para elegirlo: sin montos ni juicios, sólo su nombre y qué tiene pendiente. */
export type NegocioParaElegir = {
  id: string;
  nombre: string;
  avisosPendientes: number | null;
};

/** Lo que el pop-up necesita de cada negocio del resumen conjunto; los montos quedan afuera. */
export const paraElegir = (resumen: ResumenDeNegocio): NegocioParaElegir => ({
  id: resumen.id,
  nombre: resumen.nombre,
  avisosPendientes: resumen.avisosPendientes,
});

export const textoDeAvisos = (cantidad: number) => {
  if (cantidad === 0) return 'Sin avisos pendientes';
  return cantidad === 1 ? '1 aviso pendiente' : `${cantidad} avisos pendientes`;
};

/** «KIOSCO» como «Kiosco», para cuando no está el nombre del rubro que da el servidor. */
export const rubroLegible = (codigo: string) => {
  const texto = codigo.replaceAll('_', ' ').toLowerCase();
  return texto.charAt(0).toUpperCase() + texto.slice(1);
};

const COLORES_DE_NEGOCIO: Color[] = ['petrol', 'orange', 'coral'];

/** Cada negocio tiene su color de la paleta, fijo por su lugar en la cuenta: así se reconoce de un vistazo. */
export const colorDelNegocio = (posicion: number): Color =>
  COLORES_DE_NEGOCIO[posicion % COLORES_DE_NEGOCIO.length] ?? 'petrol';

/** La letra del cuadrado de cada negocio. */
export const inicialDelNegocio = (nombre: string) => nombre.trim().charAt(0).toUpperCase() || 'N';

export const textoDeTablas = (cantidad: number) =>
  cantidad === 1 ? '1 tabla' : `${cantidad} tablas`;

/** «Kiosco · 4 tablas»: datos para distinguir un negocio de otro, sin juicios. */
export const detalleDelNegocio = (rubro: string | undefined, tablas: number) =>
  [rubro, textoDeTablas(tablas)].filter(Boolean).join(' · ');
