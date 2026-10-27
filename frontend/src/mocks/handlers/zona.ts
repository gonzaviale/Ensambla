import { zonaConAcceso } from '../db/contenidoPlan';
import { delay, http, HttpResponse } from 'msw';
import { pedidoDeAnalisisSchema } from '@/types/zona';
import { zonaDeLaPersona, zonaParaPunto, zonaParaTexto } from '../db/zona';
import { errorResponse } from '../errores';
import { api, cuenta, LATENCIA_BACKEND_MS, pathDe } from './comun';

const DEMORA_DEL_ANALISIS_MS = import.meta.env.MODE === 'test' ? 0 : 650;

// La zona llega siempre agregada: el mínimo de tres comercios se aplica acá, antes de entregar nada.
export const handlersZona = [
  http.get(api('/zona/agregados'), async ({ request }) => {
    const { usuario, error } = cuenta(request);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    return HttpResponse.json(zonaConAcceso(usuario.email, zonaDeLaPersona()));
  }),

  http.post(api('/zona/analisis'), async ({ request }) => {
    const { usuario, error } = cuenta(request, true);
    if (error) return error;
    await delay(DEMORA_DEL_ANALISIS_MS);
    const pedido = pedidoDeAnalisisSchema.safeParse(await request.json());
    if (!pedido.success) {
      return errorResponse(400, 'Indicá un lugar o un punto del mapa', pathDe(request));
    }
    if ('lugar' in pedido.data) {
      return HttpResponse.json(zonaConAcceso(usuario.email, zonaParaTexto(pedido.data.lugar)));
    }
    const zona = zonaParaPunto(pedido.data);
    return zona
      ? HttpResponse.json(zonaConAcceso(usuario.email, zona))
      : errorResponse(404, 'Todavía no tenemos datos de esa zona', pathDe(request));
  }),
];
