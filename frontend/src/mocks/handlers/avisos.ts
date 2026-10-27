import { contenidoDe } from '../db/contenidoPlan';
import { delay, http, HttpResponse } from 'msw';
import type { AvisoDelServidor } from '@/types/avisos';
import { estado } from '../db/estado';
import { errorResponse } from '../errores';
import { api, cuenta, LATENCIA_BACKEND_MS, pathDe } from './comun';

const avisosDe = (email: string) => estado.avisosDelServidor.get(email) ?? [];

// Los avisos viven en el backend (su motor corre solo): este mock sólo atiende la lista y las resoluciones de los
// que un test sembró en `estado.avisosDelServidor`. Un aviso ajeno responde igual que uno que no existe.
function resolver(nuevoEstado: 'APLICADO' | 'DESCARTADO') {
  return async ({ request, params }: { request: Request; params: Record<string, unknown> }) => {
    const { usuario, error } = cuenta(request, true);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    const aviso = avisosDe(usuario.email).find((item) => item.id === params.id);
    if (!aviso) return errorResponse(404, 'El aviso no existe', pathDe(request));
    if (!contenidoDe(usuario.email, 'AVISOS', [`servidor:${aviso.id}`]).autorizadas.length)
      return HttpResponse.json(
        {
          timestamp: new Date().toISOString(),
          status: 403,
          error: 'Forbidden',
          path: pathDe(request),
          traceId: crypto.randomUUID(),
          message: 'Usaste los tres avisos incluidos. Conocé Premium en Planes.',
          codigo: 'LIMITE_CONTENIDO_AVISOS',
        },
        { status: 403 },
      );
    if (aviso.estado !== 'PENDIENTE') {
      return errorResponse(400, 'El aviso ya fue resuelto', pathDe(request));
    }
    const resuelto: AvisoDelServidor = {
      ...aviso,
      estado: nuevoEstado,
      resueltoEn: new Date().toISOString(),
    };
    estado.avisosDelServidor.set(
      usuario.email,
      avisosDe(usuario.email).map((item) => (item.id === aviso.id ? resuelto : item)),
    );
    return HttpResponse.json(resuelto);
  };
}

export const handlersAvisos = [
  http.get(api('/avisos/contador'), ({ request }) => {
    const { usuario, error } = cuenta(request);
    if (error) return error;
    const pendientes = avisosDe(usuario.email).filter((aviso) => aviso.estado === 'PENDIENTE');
    return HttpResponse.json({
      pendientes: pendientes.length,
      urgentes: pendientes.filter((aviso) => aviso.prioridad >= 2).length,
    });
  }),
  http.get(api('/avisos'), async ({ request }) => {
    const { usuario, error } = cuenta(request);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    const pedido = new URL(request.url).searchParams.get('estado');
    const avisos = avisosDe(usuario.email).filter((aviso) => !pedido || aviso.estado === pedido);
    const acceso = contenidoDe(
      usuario.email,
      'AVISOS',
      avisos.map((aviso) => `servidor:${aviso.id}`),
    );
    return HttpResponse.json(
      avisos.filter((aviso) => acceso.autorizadas.includes(`servidor:${aviso.id}`)),
    );
  }),
  http.post(api('/avisos/:id/aplicar'), resolver('APLICADO')),
  http.post(api('/avisos/:id/descartar'), resolver('DESCARTADO')),
];
