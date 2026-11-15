import { comportamientoDe } from './comportamiento';
import type { Campo, Sistema, Valor } from './tipos';

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
