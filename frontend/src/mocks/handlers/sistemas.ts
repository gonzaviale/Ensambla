import { delay, http, HttpResponse } from 'msw';
import { preferenciasSchema } from '@/types/preferencias';
import { sistemaSchema } from '@/types/sistema';
import { guardarPreferenciasDe, guardarSistemasDe, preferenciasDe, sistemasDe } from '../db/estado';
import { errorResponse } from '../errores';
import { api, cuenta, LATENCIA_BACKEND_MS, pathDe } from './comun';

function erroresDeCampos(error: { issues: { path: PropertyKey[]; message: string }[] }) {
  const errores: Record<string, string> = {};
  for (const { path, message } of error.issues) errores[path.join('.') || 'cuerpo'] ??= message;
  return errores;
}

// Los sistemas de la persona. El backend real los guardará por negocio; acá, por cuenta.
export const handlersSistemas = [
  http.get(api('/sistemas'), async ({ request }) => {
    const { usuario, error } = cuenta(request);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    return HttpResponse.json(
      [...sistemasDe(usuario.email)].sort((a, b) => a.createdAt.localeCompare(b.createdAt)),
    );
  }),

  // Guarda el sistema entero: crea si no existe y reemplaza si existe (se puede reintentar sin duplicar).
  http.put(api('/sistemas/:id'), async ({ request, params }) => {
    const { usuario, error } = cuenta(request, true);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    const cuerpo = sistemaSchema.safeParse(await request.json());
    if (!cuerpo.success) {
      return errorResponse(
        400,
        'Revisá los datos del sistema',
        pathDe(request),
        erroresDeCampos(cuerpo.error),
      );
    }
    if (cuerpo.data.id !== params.id) {
      return errorResponse(400, 'El id del sistema no coincide con la dirección', pathDe(request));
    }
    const actuales = sistemasDe(usuario.email);
    const existe = actuales.some((sistema) => sistema.id === cuerpo.data.id);
    guardarSistemasDe(
      usuario.email,
      existe
        ? actuales.map((sistema) => (sistema.id === cuerpo.data.id ? cuerpo.data : sistema))
        : [...actuales, cuerpo.data],
    );
    return HttpResponse.json(cuerpo.data, { status: existe ? 200 : 201 });
  }),

  http.delete(api('/sistemas/:id'), async ({ request, params }) => {
    const { usuario, error } = cuenta(request, true);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    const actuales = sistemasDe(usuario.email);
    if (!actuales.some((sistema) => sistema.id === params.id)) {
      return errorResponse(404, 'Sistema no encontrado', pathDe(request));
    }
    guardarSistemasDe(
      usuario.email,
      actuales.filter((sistema) => sistema.id !== params.id),
    );
    return new HttpResponse(null, { status: 204 });
  }),

  http.get(api('/preferencias'), async ({ request }) => {
    const { usuario, error } = cuenta(request);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    return HttpResponse.json(preferenciasDe(usuario.email));
  }),

  http.put(api('/preferencias'), async ({ request }) => {
    const { usuario, error } = cuenta(request, true);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    const cuerpo = preferenciasSchema.safeParse(await request.json());
    if (!cuerpo.success) {
      return errorResponse(
        400,
        'Revisá las preferencias',
        pathDe(request),
        erroresDeCampos(cuerpo.error),
      );
    }
    guardarPreferenciasDe(usuario.email, cuerpo.data);
    return HttpResponse.json(cuerpo.data);
  }),
];
