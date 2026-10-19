import { env } from '@/config/env';
import { estado, usuarioActual, type UsuarioMock } from '../db/estado';
import { errorResponse } from '../errores';

export const api = (path: string) => `${env.apiUrl}${path}`;

export const LATENCIA_BACKEND_MS = 100;

export const HEADER_CSRF = 'X-CSRF-TOKEN';
export const TOKEN_CSRF = 'csrf-token-de-prueba';

export const pathDe = (request: Request) => new URL(request.url).pathname;

// En modo parcial el login, la sesión y el CSRF son los del backend real: el mock no los conoce y atiende
// lo demás para esa sesión con una cuenta propia.
const modoParcial = () => env.mocks === 'parcial';
const CUENTA_DE_LA_SESION_REAL: UsuarioMock = {
  id: 'sesion-real',
  nombre: 'Sesión real',
  email: 'sesion-real@ensambla.local',
  contrasenia: '',
};

// El backend real rechaza con 403 toda mutación sin el token CSRF: el mock hace lo mismo para
// que el cliente HTTP no pueda "olvidarse" de mandarlo sin que algún test falle.
export function exigirCsrf(request: Request) {
  if (modoParcial()) return null;
  if (request.headers.get(HEADER_CSRF) === TOKEN_CSRF) return null;
  return errorResponse(403, 'Acceso no permitido', pathDe(request));
}

export function exigirSesion(request: Request) {
  if (modoParcial()) return null;
  if (estado.sesionUsuarioId) return null;
  return errorResponse(401, 'Se requiere una sesión válida', pathDe(request));
}

/** La cuenta de la sesión, o la respuesta de error si no hay (y el CSRF si se va a escribir). */
export function cuenta(request: Request, escritura = false) {
  const rechazo = (escritura && exigirCsrf(request)) || exigirSesion(request);
  if (rechazo) return { error: rechazo };
  const usuario = modoParcial() ? CUENTA_DE_LA_SESION_REAL : usuarioActual();
  if (!usuario) {
    return { error: errorResponse(401, 'Se requiere una sesión válida', pathDe(request)) };
  }
  return { usuario };
}
