import { zonaConAcceso, type TipoDeCupo } from '../db/contenidoPlan';
import { delay, http, HttpResponse } from 'msw';
import { productosPropios } from '@/lib/sistema/zona';
import { pedidoDeAnalisisSchema } from '@/types/zona';
import { sistemasDe } from '../db/estado';
import {
  ordenadaParaVista,
  zonaDeLaPersona,
  zonaParaPunto,
  zonaParaTexto,
  type VistaDeLaZona,
} from '../db/zona';
import { errorResponse } from '../errores';
import { api, cuenta, LATENCIA_BACKEND_MS, pathDe } from './comun';

const DEMORA_DEL_ANALISIS_MS = import.meta.env.MODE === 'test' ? 0 : 650;

// Cada pantalla de Mi zona ordena los productos a su manera y gasta su propio cupo gratuito.
const VISTAS: Record<string, { vista: VistaDeLaZona; cupo: TipoDeCupo }> = {
  precios: { vista: 'precios', cupo: 'ZONA' },
  cerca: { vista: 'cerca', cupo: 'ZONA_CERCA' },
};

// La zona llega siempre agregada: el mínimo de tres comercios se aplica acá, antes de entregar nada.
export const handlersZona = [
  http.get(api('/zona/agregados'), async ({ request }) => {
    const { usuario, error } = cuenta(request);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    const pedida = VISTAS[new URL(request.url).searchParams.get('vista') ?? 'precios'];
    if (!pedida) {
      return errorResponse(400, 'La vista tiene que ser «precios» o «cerca»', pathDe(request));
    }
    const zona = ordenadaParaVista(
      zonaDeLaPersona(),
      pedida.vista,
      productosPropios(sistemasDe(usuario.email)),
    );
    return HttpResponse.json(zonaConAcceso(usuario.email, zona, pedida.cupo));
  }),

  http.post(api('/zona/analisis'), async ({ request }) => {
    const { usuario, error } = cuenta(request, true);
    if (error) return error;
    await delay(DEMORA_DEL_ANALISIS_MS);
    const pedido = pedidoDeAnalisisSchema.safeParse(await request.json());
    if (!pedido.success) {
      return errorResponse(400, 'Indicá un lugar o un punto del mapa', pathDe(request));
    }
    const otraZona = (zona: ReturnType<typeof zonaParaTexto>) =>
      HttpResponse.json(
        zonaConAcceso(usuario.email, ordenadaParaVista(zona, 'otra', []), 'ZONA_OTRA'),
      );
    if ('lugar' in pedido.data) return otraZona(zonaParaTexto(pedido.data.lugar));
    const zona = zonaParaPunto(pedido.data);
    return zona
      ? otraZona(zona)
      : errorResponse(404, 'Todavía no tenemos datos de esa zona', pathDe(request));
  }),
];
