import { comportamientoDe } from '../comportamiento';
import type { Campo, Valor } from '../tipos';

export const MAXIMO_DE_EJEMPLOS = 5;
export const MAXIMO_DE_REGISTROS = 30;
export const MAXIMO_DE_NOMBRES = 100;
export const MAXIMO_DE_OPCIONES = 6;
export const LARGO_DE_NOMBRE = 60;
export const LARGO_DE_NOMBRE_DE_SISTEMA = 100;

export const texto = (valor: unknown) => (typeof valor === 'string' ? valor.trim() : '');

export const lista = (valor: unknown): unknown[] => (Array.isArray(valor) ? valor : []);

export const esObjeto = (valor: unknown): valor is Record<string, unknown> =>
  typeof valor === 'object' && valor !== null && !Array.isArray(valor);

export const nombreCorto = (valor: unknown, largo = LARGO_DE_NOMBRE) =>
  texto(valor).slice(0, largo);

export const estaVacio = (valor: Valor): valor is '' | null | undefined =>
  valor === undefined || valor === null || valor === '';

/** Si llegó un argumento optativo: un nulo, un texto en blanco o una lista vacía no cuentan (como en el backend). */
export const llego = (valor: unknown) =>
  valor !== undefined &&
  valor !== null &&
  !(typeof valor === 'string' && !valor.trim()) &&
  !(Array.isArray(valor) && !valor.length);

/** El valor de ejemplo convertido al tipo del dato; `undefined` si no sirve. */
export function valorDeEjemplo(campo: Campo, valor: unknown): Valor {
  if (valor === null || valor === undefined || typeof valor === 'object') return undefined;
  const crudo = typeof valor === 'string' ? valor.trim() : valor;
  return comportamientoDe(campo.type).interpretar(campo, crudo);
}

export const cuantos = (cantidad: number, uno: string, varios: string) =>
  cantidad === 1 ? uno : `${cantidad} ${varios}`;

export const cantidadDeRegistros = (cantidad: number) =>
  cuantos(cantidad, 'un registro', 'registros');
