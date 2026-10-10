import type { Campo } from '@/types/proyecto';
import type { ValoresRegistro } from '@/types/registro';

export type ValoresFormulario = Record<string, string>;

export function valoresIniciales(campos: readonly Campo[]): ValoresFormulario {
  return Object.fromEntries(campos.map((campo) => [campo.id, campo.valorInicial ?? '']));
}

export function valoresParaGuardar(
  campos: readonly Campo[],
  valores: ValoresFormulario,
): ValoresRegistro {
  const resultado: ValoresRegistro = {};
  for (const campo of campos) {
    const valor = valores[campo.id]?.trim() ?? '';
    if (valor === '') continue;
    if (campo.tipo === 'NUMERO') resultado[campo.id] = Number(valor);
    else if (campo.tipo === 'SI_NO') resultado[campo.id] = valor === 'true';
    else resultado[campo.id] = valor;
  }
  return resultado;
}
