import { comportamientoDe } from './comportamiento';
import type { Campo, Sistema, Valor } from './tipos';

export { formatoFecha, formatoPesos } from './formatoDeValores';

const textoSegunElTipo = (campo: Campo, valor: Valor, sistema: Sistema | undefined) =>
  comportamientoDe(campo.type).textoPlano(campo, valor, sistema);

/** Lo que dice un registro en texto, para buscar y para el título de la celda. */
export function textoPlano(sistema: Sistema | undefined, campo: Campo, valor: Valor): string {
  return textoSegunElTipo(campo, valor, sistema);
}

export const ESTADOS_BUENOS = ['Entregado'];
export const ESTADOS_EN_CURSO = ['En preparación'];

/** La fecha de hoy como «aaaa-mm-dd», que es lo que guardan los campos de fecha. */
export function hoy() {
  const fecha = new Date();
  return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}`;
}
