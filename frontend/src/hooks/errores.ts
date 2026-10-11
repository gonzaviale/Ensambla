import { ApiError } from '@/api/apiError';

export type ErrorServidor = {
  status: number;
  mensaje: string;
  /** `errors` del backend: nombre del campo del cuerpo → mensaje. */
  campos: Record<string, string>;
};

const MENSAJE_GENERICO = 'Ocurrió un error inesperado. Probá de nuevo en unos minutos.';

// La UI no conoce ApiError (capa api): recibe el error ya traducido para ubicarlo junto a cada input.
export function leerErrorServidor(error: unknown): ErrorServidor | undefined {
  if (!error) return undefined;
  if (error instanceof ApiError) {
    return { status: error.status, mensaje: error.message, campos: error.fieldErrors };
  }
  return { status: 0, mensaje: MENSAJE_GENERICO, campos: {} };
}

/**
 * Reparte el error entre los inputs de un formulario: cada `errors.<campo>` va junto a su input y
 * el 409 (nombre repetido) junto al nombre. Lo que no tiene dónde mostrarse queda como `general`.
 */
export function ubicarErrores<C extends string>(
  error: ErrorServidor | undefined,
  inputs: readonly C[],
): { porInput: Partial<Record<C, string>>; general?: string } {
  if (!error) return { porInput: {} };
  const porInput: Partial<Record<C, string>> = {};
  for (const [clave, mensaje] of Object.entries(error.campos)) {
    // El backend puede indexar listas: "opciones[1]" → opciones.
    const input = inputs.find((item) => clave === item || clave.startsWith(`${item}[`));
    if (input) porInput[input] ??= mensaje;
  }
  const nombre = inputs.find((item) => item === 'nombre');
  if (error.status === 409 && nombre) porInput[nombre] ??= error.mensaje;
  const ubicados = Object.keys(porInput).length > 0;
  return ubicados ? { porInput } : { porInput, general: error.mensaje };
}
