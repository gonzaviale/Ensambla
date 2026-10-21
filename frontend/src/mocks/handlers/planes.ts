import { delay, http, HttpResponse } from 'msw';
import { estado } from '../db/estado';
import { planesDe } from '../db/planes';
import { api, cuenta, LATENCIA_BACKEND_MS, pathDe } from './comun';

export const handlersPlanes = [
  http.get(api('/planes/renovacion'), async ({ request }) => {
    const sesion = cuenta(request);
    if (sesion.error) return sesion.error;
    return HttpResponse.json({ habilitada: false, periodoDias: 30, contrato: null });
  }),
  http.get(api('/planes'), async ({ request }) => {
    await delay(LATENCIA_BACKEND_MS);
    const sesion = cuenta(request);
    if (sesion.error) return sesion.error;
    return HttpResponse.json(planesDe(sesion.usuario.email), {
      headers: { 'Cache-Control': 'no-store' },
    });
  }),
  http.post(api('/planes/prueba'), async ({ request }) => {
    await delay(LATENCIA_BACKEND_MS);
    const sesion = cuenta(request, true);
    if (sesion.error) return sesion.error;
    if (estado.pruebasPremium.has(sesion.usuario.email)) {
      return HttpResponse.json(
        {
          timestamp: new Date().toISOString(),
          status: 403,
          error: 'Forbidden',
          message: 'Este negocio ya utilizó su prueba premium.',
          path: pathDe(request),
          traceId: crypto.randomUUID(),
          codigo: 'PRUEBA_UTILIZADA',
        },
        { status: 403 },
      );
    }
    const ahora = new Date();
    const prueba = {
      inicioEn: ahora.toISOString(),
      finEn: new Date(ahora.getTime() + 7 * 24 * 60 * 60 * 1000).toISOString(),
      paginasIaUtilizadas: 0,
      limitePaginasIa: 10,
    };
    estado.pruebasPremium.set(sesion.usuario.email, prueba);
    estado.alCambiar?.();
    return HttpResponse.json(prueba, { headers: { 'Cache-Control': 'no-store' } });
  }),
];
