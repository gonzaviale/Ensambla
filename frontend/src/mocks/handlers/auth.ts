import { HttpResponse, delay, http } from 'msw';
import { loginSchema, registroSchema } from '@/types/auth';
import { estado, type UsuarioMock } from '../db/estado';
import { errorResponse } from '../errores';
import {
  HEADER_CSRF,
  LATENCIA_BACKEND_MS,
  TOKEN_CSRF,
  api,
  exigirCsrf,
  exigirSesion,
} from './comun';

const pathDe = (request: Request) => new URL(request.url).pathname;
const normalizar = (email: string) => email.trim().toLowerCase();

const aUsuarioResponse = ({ id, nombre, email }: UsuarioMock) => ({ id, nombre, email });

function erroresDeCampos(error: { issues: { path: PropertyKey[]; message: string }[] }) {
  const errores: Record<string, string> = {};
  for (const { path, message } of error.issues) {
    const campo = String(path[0] ?? 'cuerpo');
    errores[campo] ??= message;
  }
  return errores;
}

// Imitan la sesión por cookie + CSRF del backend. El CSRF ya existe en el backend real; el resto
// (registro, login, sesión) llega con T-005 y T-006: cuando estén, este archivo sale de 'parcial'.
export const handlersAuth = [
  http.get(api('/auth/csrf'), async () => {
    await delay(LATENCIA_BACKEND_MS);
    return HttpResponse.json(
      { token: TOKEN_CSRF, headerName: HEADER_CSRF },
      { headers: { 'Cache-Control': 'no-store' } },
    );
  }),

  http.post(api('/auth/register'), async ({ request }) => {
    await delay(LATENCIA_BACKEND_MS);
    const rechazo = exigirCsrf(request);
    if (rechazo) return rechazo;

    const cuerpo = registroSchema.safeParse(await request.json());
    if (!cuerpo.success) {
      return errorResponse(
        400,
        'Error de validación en los datos enviados',
        pathDe(request),
        erroresDeCampos(cuerpo.error),
      );
    }
    const email = normalizar(cuerpo.data.email);
    if (estado.usuarios.has(email)) {
      return errorResponse(409, 'Ya existe una cuenta con ese email', pathDe(request));
    }
    const usuario: UsuarioMock = {
      id: crypto.randomUUID(),
      nombre: cuerpo.data.nombre,
      email,
      contrasenia: cuerpo.data.contrasenia,
    };
    estado.usuarios.set(email, usuario);
    estado.alCambiar?.();
    return HttpResponse.json(aUsuarioResponse(usuario), { status: 201 });
  }),

  http.post(api('/auth/login'), async ({ request }) => {
    await delay(LATENCIA_BACKEND_MS);
    const rechazo = exigirCsrf(request);
    if (rechazo) return rechazo;

    const cuerpo = loginSchema.safeParse(await request.json());
    const usuario = cuerpo.success ? estado.usuarios.get(normalizar(cuerpo.data.email)) : undefined;
    if (!cuerpo.success || usuario?.contrasenia !== cuerpo.data.contrasenia) {
      return errorResponse(401, 'No se pudo autenticar el acceso', pathDe(request));
    }
    estado.sesionUsuarioId = usuario.id;
    estado.alCambiar?.();
    return HttpResponse.json(aUsuarioResponse(usuario));
  }),

  http.get(api('/auth/me'), async ({ request }) => {
    await delay(LATENCIA_BACKEND_MS);
    const rechazo = exigirSesion(request);
    if (rechazo) return rechazo;
    const usuario = [...estado.usuarios.values()].find((u) => u.id === estado.sesionUsuarioId);
    if (!usuario) return errorResponse(401, 'Se requiere una sesión válida', pathDe(request));
    return HttpResponse.json(aUsuarioResponse(usuario));
  }),

  http.post(api('/auth/logout'), async ({ request }) => {
    await delay(LATENCIA_BACKEND_MS);
    const rechazo = exigirCsrf(request);
    if (rechazo) return rechazo;
    estado.sesionUsuarioId = null;
    estado.alCambiar?.();
    return new HttpResponse(null, { status: 204 });
  }),
];
