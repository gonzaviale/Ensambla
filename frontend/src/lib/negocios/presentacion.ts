// Cómo se dice cada negocio de la cuenta en pantalla (ENS-284). Las formas siguen el contrato de
// `GET /api/negocios/resumen` (docs/api-negocios.md); cuando lleguen los tipos de T-194 son las mismas.

export type EstadoDelNegocio = 'BIEN' | 'ATENCION' | 'SIN_DATOS';

/** Lo que hace falta de un negocio para elegirlo: sin montos, sólo cómo viene y qué tiene pendiente. */
export type NegocioParaElegir = {
  id: string;
  nombre: string;
  rubro: string;
  avisosPendientes: number;
  estado: EstadoDelNegocio;
};

export const ESTADO_DEL_NEGOCIO: Record<EstadoDelNegocio, string> = {
  BIEN: 'Viene bien',
  ATENCION: 'Atención',
  SIN_DATOS: 'Sin ventas',
};

export const textoDeAvisos = (cantidad: number) => {
  if (cantidad === 0) return 'Sin avisos pendientes';
  return cantidad === 1 ? '1 aviso pendiente' : `${cantidad} avisos pendientes`;
};

/** «KIOSCO» como «Kiosco», para cuando no está el nombre del rubro que da el servidor. */
export const rubroLegible = (codigo: string) => {
  const texto = codigo.replaceAll('_', ' ').toLowerCase();
  return texto.charAt(0).toUpperCase() + texto.slice(1);
};
