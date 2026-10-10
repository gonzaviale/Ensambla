import { env } from '@/config/env';
import { estado } from '../db/estado';
import { errorResponse } from '../errores';

export const api = (path: string) => `${env.apiUrl}${path}`;

export const LATENCIA_BACKEND_MS = 100;

export const HEADER_CSRF = 'X-CSRF-TOKEN';
export const TOKEN_CSRF = 'csrf-token-de-prueba';

const pathDe = (request: Request) => new URL(request.url).pathname;

// El backend real rechaza con 403 toda mutación sin el token CSRF: el mock hace lo mismo para
// que el cliente HTTP no pueda "olvidarse" de mandarlo sin que algún test falle.
export function exigirCsrf(request: Request) {
  if (request.headers.get(HEADER_CSRF) === TOKEN_CSRF) return null;
  return errorResponse(403, 'Acceso no permitido', pathDe(request));
}

export function exigirSesion(request: Request) {
  if (estado.sesionUsuarioId) return null;
  return errorResponse(401, 'Se requiere una sesión válida', pathDe(request));
}
