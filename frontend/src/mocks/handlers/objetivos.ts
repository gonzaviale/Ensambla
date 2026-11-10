import { delay, http, HttpResponse } from 'msw';
import { z } from 'zod';
import { objetivosDelServidorSchema, type ObjetivoDelServidor } from '@/types/objetivos';
import { guardarObjetivosDe, objetivosDe } from '../db/estado';
import { errorResponse } from '../errores';
import { api, cuenta, LATENCIA_BACKEND_MS, pathDe } from './comun';

const nuevoSchema = z.object({
  texto: z.string().trim().min(1, 'El texto es obligatorio').max(500, 'Máximo 500 caracteres'),
  meta: z.number().min(1, 'La meta tiene que ser mayor o igual a 1'),
});

// Igual que el backend: cada cuenta ve solo sus objetivos, y uno ajeno responde igual que uno que no existe.
export const handlersObjetivos = [
  http.get(api('/objetivos'), async ({ request }) => {
    const { usuario, error } = cuenta(request);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    return HttpResponse.json(objetivosDelServidorSchema.parse(objetivosDe(usuario.email)));
  }),
  http.post(api('/objetivos'), async ({ request }) => {
    const { usuario, error } = cuenta(request, true);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    const datos = nuevoSchema.safeParse(await request.json());
    if (!datos.success)
      return errorResponse(
        400,
        'Revisá los datos del objetivo.',
        pathDe(request),
        Object.fromEntries(
          datos.error.issues.map((problema) => [String(problema.path[0]), problema.message]),
        ),
      );
    const creado: ObjetivoDelServidor = {
      id: crypto.randomUUID(),
      texto: datos.data.texto,
      meta: datos.data.meta,
      creadoEn: new Date().toISOString(),
    };
    guardarObjetivosDe(usuario.email, [...objetivosDe(usuario.email), creado]);
    return HttpResponse.json(creado, {
      status: 201,
      headers: { Location: `/api/objetivos/${creado.id}` },
    });
  }),
  http.delete(api('/objetivos/:id'), async ({ request, params }) => {
    const { usuario, error } = cuenta(request, true);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    const propios = objetivosDe(usuario.email);
    if (!propios.some((objetivo) => objetivo.id === params.id))
      return errorResponse(404, 'Objetivo no encontrado', pathDe(request));
    guardarObjetivosDe(
      usuario.email,
      propios.filter((objetivo) => objetivo.id !== params.id),
    );
    return new HttpResponse(null, { status: 204 });
  }),
];
