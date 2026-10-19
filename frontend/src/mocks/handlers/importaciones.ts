import { delay, http, HttpResponse } from 'msw';
import { lecturaDeEjemplo } from '@/lib/sistema/lectura';
import { pedidoDeLecturaSchema } from '@/types/importacion';
import { errorResponse } from '../errores';
import { api, cuenta, pathDe } from './comun';

const DEMORA_DE_LECTURA_MS = import.meta.env.MODE === 'test' ? 0 : 650;

// La lectura de una foto es simulada: devuelve tres registros de ejemplo para revisar, una columna que
// el bloque no tiene (si ya existe en el sistema), algún dato dudoso y una descripción de negocio de
// muestra. Cuando exista el servicio real, cambia sólo este handler.
export const handlersImportaciones = [
  http.post(api('/importaciones/lectura'), async ({ request }) => {
    const { error } = cuenta(request, true);
    if (error) return error;
    await delay(DEMORA_DE_LECTURA_MS);
    const pedido = pedidoDeLecturaSchema.safeParse(await request.json());
    if (!pedido.success) {
      return errorResponse(400, 'No se pudo leer la imagen', pathDe(request));
    }
    return HttpResponse.json(lecturaDeEjemplo(pedido.data));
  }),
];
