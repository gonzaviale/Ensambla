import { delay, http, HttpResponse } from 'msw';
import { hoy } from '@/lib/sistema/formato';
import { MAXIMO_DIAS_DEL_RESUMEN } from '@/lib/sistema/periodoResumen';
import { DIAS_DEL_PANEL } from '@/api/panel';
import { panelDe } from '../db/panel';
import { sistemasDe } from '../db/estado';
import { errorResponse } from '../errores';
import { api, cuenta, LATENCIA_BACKEND_MS, pathDe } from './comun';

// Las métricas y el flujo de caja de la home: sólo del sistema de la persona; uno ajeno responde 404.
export const handlersPanel = [
  http.get(api('/negocio/panel'), async ({ request }) => {
    await delay(LATENCIA_BACKEND_MS);
    const sesion = cuenta(request);
    if (sesion.error) return sesion.error;
    const consulta = new URL(request.url).searchParams;
    const dias = consulta.has('dias') ? Number(consulta.get('dias')) : DIAS_DEL_PANEL;
    if (!Number.isInteger(dias) || dias < 1 || dias > MAXIMO_DIAS_DEL_RESUMEN) {
      return errorResponse(400, 'La cantidad de días no es válida', pathDe(request));
    }
    const sistema = sistemasDe(sesion.usuario.email).find(
      (otro) => otro.id === consulta.get('proyectoId'),
    );
    if (!sistema) return errorResponse(404, 'Proyecto no encontrado', pathDe(request));
    return HttpResponse.json(panelDe(sistema, hoy(), dias));
  }),
];
