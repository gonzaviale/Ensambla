import type { Campo } from '@/types/proyecto';
import type { ValoresRegistro } from '@/types/registro';

export type ValoresFormulario = Record<string, string>;
export type ErroresFormulario = Record<string, string>;

function esFechaValida(valor: string) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(valor)) return false;
  const fecha = new Date(`${valor}T00:00:00Z`);
  return Number.isFinite(fecha.getTime()) && fecha.toISOString().slice(0, 10) === valor;
}

export function validarFormulario(
  campos: readonly Campo[],
  valores: ValoresFormulario,
): ErroresFormulario {
  const errores: ErroresFormulario = {};
  for (const campo of campos) {
    const valor = valores[campo.id]?.trim() ?? '';
    if (!valor) {
      if (campo.obligatorio) errores[campo.id] = 'Este dato es obligatorio';
      continue;
    }
    switch (campo.tipo) {
      case 'TEXTO':
        if (valor.length > 1000) errores[campo.id] = 'Máximo 1000 caracteres';
        break;
      case 'NUMERO':
        if (
          !/^[+-]?(?:\d+\.?\d*|\.\d+)(?:e[+-]?\d+)?$/i.test(valor) ||
          !Number.isFinite(Number(valor))
        )
          errores[campo.id] = 'Tiene que ser un número';
        break;
      case 'FECHA':
        if (!esFechaValida(valor)) errores[campo.id] = 'Tiene que ser una fecha válida';
        break;
      case 'OPCION':
        if (!campo.opciones.includes(valor)) errores[campo.id] = 'Elegí una de las opciones';
        break;
      case 'SI_NO':
        if (valor !== 'true' && valor !== 'false') errores[campo.id] = 'Tiene que ser sí o no';
        break;
    }
  }
  return errores;
}

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
