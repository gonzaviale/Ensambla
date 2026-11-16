import type { ResumenDeNegocio } from '@/types/negocios';

// Cómo se dice cada negocio de la cuenta en pantalla (ENS-284). Los datos salen del resumen conjunto, que junta el
// panel de cada cuaderno (ver `lib/negocios/lista`).

export type EstadoDelNegocio = 'BIEN' | 'ATENCION' | 'SIN_DATOS';

/** Lo que hace falta de un negocio para elegirlo: sin montos, sólo cómo viene y qué tiene pendiente. */
export type NegocioParaElegir = {
  id: string;
  nombre: string;
  avisosPendientes: number | null;
  estado: EstadoDelNegocio;
};

export const ESTADO_DEL_NEGOCIO: Record<EstadoDelNegocio, string> = {
  BIEN: 'Viene bien',
  ATENCION: 'Atención',
  SIN_DATOS: 'Sin ventas',
};

/** Sin ventas en el período no hay con qué juzgarlo; si lo que queda es negativo, pide atención. */
export const estadoDelNegocio = ({
  hayDatos,
  queda,
}: Pick<ResumenDeNegocio, 'hayDatos' | 'queda'>): EstadoDelNegocio => {
  if (!hayDatos) return 'SIN_DATOS';
  return queda !== null && queda < 0 ? 'ATENCION' : 'BIEN';
};

/** Lo que el pop-up necesita de cada negocio del resumen conjunto; los montos quedan afuera. */
export const paraElegir = (resumen: ResumenDeNegocio): NegocioParaElegir => ({
  id: resumen.id,
  nombre: resumen.nombre,
  avisosPendientes: resumen.avisosPendientes,
  estado: estadoDelNegocio(resumen),
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
