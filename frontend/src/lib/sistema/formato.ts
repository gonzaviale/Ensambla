import { findBlock } from './core';
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

/** Lo que dice un registro en texto, para buscar y para el título de la celda. */
export function textoPlano(sistema: Sistema | undefined, campo: Campo, valor: Valor): string {
  if (valor == null || valor === '') return '';
  if (campo.type === 'relation') {
    const objetivo = findBlock(sistema, campo.target);
    const fila = objetivo?.rows.find((item) => item.id === valor);
    const clave = objetivo?.fields[0]?.key;
    return String((clave && fila?.[clave]) || 'Sin vínculo');
  }
  if (campo.type === 'checkbox') return valor ? 'Sí' : 'No';
  return String(valor);
}

export const ESTADOS_BUENOS = ['Entregado'];
export const ESTADOS_EN_CURSO = ['En preparación'];
