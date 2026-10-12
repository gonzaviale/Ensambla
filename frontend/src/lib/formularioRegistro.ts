import type { Campo } from '@/types/proyecto';
import type { CambiosRegistro, Registro, ValoresRegistro } from '@/types/registro';

export type ValoresFormulario = Record<string, string>;
export type ErroresFormulario = Record<string, string>;

function esFechaValida(valor: string) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(valor)) return false;
  const fecha = new Date(`${valor}T00:00:00Z`);
  return Number.isFinite(fecha.getTime()) && fecha.toISOString().slice(0, 10) === valor;
}

// En pantalla se acepta coma o punto decimal (12,5 o 12.5); al validar y guardar se usa el punto.
const aNumero = (valor: string) => valor.replace(',', '.');

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
      // El id de una fila de otra tabla: que exista lo confirma el servidor.
      case 'RELACION':
        break;
    }
  }
  return errores;
}

export function valoresIniciales(campos: readonly Campo[]): ValoresFormulario {
  return Object.fromEntries(campos.map((campo) => [campo.id, campo.valorInicial ?? '']));
}

// Los datos de una fila ya guardada, como texto para precargar el formulario de edición.
export function valoresDeRegistro(
  campos: readonly Campo[],
  valores: Registro['valores'],
): ValoresFormulario {
  return Object.fromEntries(
    campos.map((campo) => {
      const valor = valores[campo.id];
      return [campo.id, valor === undefined || valor === null ? '' : String(valor)];
    }),
  );
}

export function valoresParaGuardar(
  campos: readonly Campo[],
  valores: ValoresFormulario,
): ValoresRegistro {
  const resultado: ValoresRegistro = {};
  for (const campo of campos) {
    const valor = valores[campo.id]?.trim() ?? '';
    if (valor === '') continue;
    if (campo.tipo === 'NUMERO') resultado[campo.id] = Number(aNumero(valor));
    else if (campo.tipo === 'SI_NO') resultado[campo.id] = valor === 'true';
    else resultado[campo.id] = valor;
  }
  return resultado;
}

// Al editar hay que mandar también lo que se vació: el backend conserva lo que no se envía y
// sólo borra un dato cuando llega `null`.
export function cambiosParaEditar(
  campos: readonly Campo[],
  cargados: ValoresRegistro,
): CambiosRegistro {
  return Object.fromEntries(campos.map((campo) => [campo.id, cargados[campo.id] ?? null]));
}
