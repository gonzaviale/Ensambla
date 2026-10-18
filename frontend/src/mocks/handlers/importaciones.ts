import { delay, http, HttpResponse } from 'msw';
import { DESCRIPCION_DE_EJEMPLO, filasDeEjemplo } from '@/lib/sistema/lectura';
import { pedidoDeLecturaSchema } from '@/types/importacion';
import { errorResponse } from '../errores';
import { api, cuenta, pathDe } from './comun';

const DEMORA_DE_LECTURA_MS = import.meta.env.MODE === 'test' ? 0 : 650;

// La lectura de una foto es simulada: devuelve tres registros de ejemplo para revisar y una descripción
// de negocio de muestra. Cuando exista el servicio real, cambia sólo este handler.
export const handlersImportaciones = [
  http.post(api('/importaciones/lectura'), async ({ request }) => {
    const { error } = cuenta(request, true);
    if (error) return error;
    await delay(DEMORA_DE_LECTURA_MS);
    const pedido = pedidoDeLecturaSchema.safeParse(await request.json());
    if (!pedido.success) {
      return errorResponse(400, 'No se pudo leer la imagen', pathDe(request));
    }
    const { tipo, campos, vinculos } = pedido.data;
    return HttpResponse.json({
      filas: filasDeEjemplo(tipo, campos, vinculos),
      descripcion: DESCRIPCION_DE_EJEMPLO,
    });
  }),
];
