import { delay, http, HttpResponse } from 'msw';
import { lecturaDeEjemplo } from '@/lib/sistema/lectura';
import { pedidoDeLecturaSchema } from '@/types/importacion';
import { errorResponse } from '../errores';
import { api, cuenta, pathDe } from './comun';

const DEMORA_DE_LECTURA_MS = import.meta.env.MODE === 'test' ? 0 : 650;

// El pedido llega en un multipart: la parte `imagen` y la parte `pedido` (JSON), como en el backend.
async function leerPedido(request: Request) {
  try {
    const cuerpo = await request.formData();
    const parte = cuerpo.get('pedido');
    const texto = typeof parte === 'string' ? parte : await parte?.text();
    return pedidoDeLecturaSchema.safeParse(JSON.parse(texto ?? 'null'));
  } catch {
    return null;
  }
}

// En modo completo la lectura de una foto es simulada: devuelve tres registros de ejemplo para revisar, una
// columna que el bloque no tiene (si ya existe en el sistema) y algún dato dudoso. Con el backend (parcial) la
// lee la IA y este handler no se usa.
export const handlersImportaciones = [
  http.post(api('/importaciones/lectura'), async ({ request }) => {
    const { error } = cuenta(request, true);
    if (error) return error;
    await delay(DEMORA_DE_LECTURA_MS);
    const pedido = await leerPedido(request);
    if (!pedido?.success) {
      return errorResponse(400, 'No se pudo leer la imagen', pathDe(request));
    }
    return HttpResponse.json(lecturaDeEjemplo(pedido.data));
  }),
];
