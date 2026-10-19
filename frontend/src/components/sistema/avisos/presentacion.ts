import type { AccionDeAviso, TipoDeAviso } from '@/lib/sistema/alertas';
import type { NombreDeIcono } from '@/lib/sistema/iconos';

export const ICONO_DE_AVISO: Record<TipoDeAviso, NombreDeIcono> = {
  stock: 'box',
  precio_zona: 'chart',
  estancado: 'clock',
  muy_vendido: 'spark',
  combo: 'orders',
  consumo_zona: 'users',
};

/** Lo que dice el botón: lo que modifica los datos arranca con «Aplicar». */
export function textoDeAccion(accion: AccionDeAviso) {
  if (accion.tipo === 'aplicar' || accion.tipo === 'crear' || accion.tipo === 'servidor') {
    return `Aplicar: ${accion.texto.charAt(0).toLowerCase()}${accion.texto.slice(1)}`;
  }
  return accion.texto;
}
