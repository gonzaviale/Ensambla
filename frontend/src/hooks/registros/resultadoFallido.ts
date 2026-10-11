import { ApiError } from '@/api/apiError';
import type { ResultadoGuardado } from '@/types/registro';

// Un error de la API trae el mensaje y los errores por campo; cualquier otra cosa (corte de red,
// respuesta ilegible) no permite saber si se guardó, así que se avisa que se revise antes de repetir.
export function resultadoFallido(error: unknown): Extract<ResultadoGuardado, { exito: false }> {
  return {
    exito: false,
    mensaje:
      error instanceof ApiError
        ? error.message
        : 'No pudimos confirmar el guardado. Revisá antes de volver a intentar.',
    errores: error instanceof ApiError ? error.fieldErrors : {},
  };
}
