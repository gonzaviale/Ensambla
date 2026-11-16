import { delay, http, HttpResponse } from 'msw';
import { opcionesDe, PROMOCION_DE_PRUEBA, TEXTOS_DE_PRUEBA } from '../db/carteles';
import { errorResponse } from '../errores';
import { api, cuenta, exigirCsrf, LATENCIA_BACKEND_MS, pathDe } from './comun';

const PNG_DE_EJEMPLO = new Uint8Array([0x89, 0x50, 0x4e, 0x47]);

type Pedido = { titular?: string; formato?: string };

export const handlersCarteles = [
  http.get(api('/carteles/promociones-sugeridas'), async ({ request }) => {
    await delay(LATENCIA_BACKEND_MS);
    const sesion = cuenta(request);
    if (sesion.error) return sesion.error;
    return HttpResponse.json({ promociones: [PROMOCION_DE_PRUEBA] });
  }),
  http.post(api('/carteles/textos'), async ({ request }) => {
    const rechazo = exigirCsrf(request);
    if (rechazo) return rechazo;
    const sesion = cuenta(request);
    if (sesion.error) return sesion.error;
    return HttpResponse.json(TEXTOS_DE_PRUEBA);
  }),
  http.post(api('/carteles/opciones'), async ({ request }) => {
    const rechazo = exigirCsrf(request);
    if (rechazo) return rechazo;
    const sesion = cuenta(request);
    if (sesion.error) return sesion.error;
    const pedido = (await request.json()) as Pedido;
    if (!pedido.titular) return errorResponse(400, 'El titular es obligatorio', pathDe(request));
    return HttpResponse.json({ opciones: opcionesDe(pedido.titular) });
  }),
  http.post(api('/carteles/exportar'), async ({ request }) => {
    const rechazo = exigirCsrf(request);
    if (rechazo) return rechazo;
    const sesion = cuenta(request);
    if (sesion.error) return sesion.error;
    return new HttpResponse(PNG_DE_EJEMPLO, {
      headers: {
        'Content-Type': 'image/png',
        'Content-Disposition': 'attachment; filename="cartel-promo-precio-a4.pdf"',
      },
    });
  }),
];
