import { estado } from '../db/estado';
import { planesDe } from '../db/planes';
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
    const { usuario, error } = cuenta(request, true);
    if (error) return error;
    await delay(DEMORA_DE_LECTURA_MS);
    const pedido = await leerPedido(request);
    if (!pedido?.success) {
      return errorResponse(400, 'No se pudo leer la imagen', pathDe(request));
    }
    const planes = planesDe(usuario.email);
    if (planes.uso.limitePaginasIa !== null && planes.uso.paginasIa >= planes.uso.limitePaginasIa)
      return HttpResponse.json(
        {
          timestamp: new Date().toISOString(),
          status: 403,
          error: 'Forbidden',
          path: pathDe(request),
          traceId: crypto.randomUUID(),
          message: 'Alcanzaste las páginas de IA de tu plan. Conocé Premium en Planes.',
          codigo: 'LIMITE_IA_MENSUAL',
        },
        { status: 403 },
      );
    const prueba = estado.pruebasPremium.get(usuario.email);
    if (planes.suscripcion.pruebaActiva && prueba) {
      estado.pruebasPremium.set(usuario.email, {
        ...prueba,
        paginasIaUtilizadas: prueba.paginasIaUtilizadas + 1,
      });
    } else {
      const clave = `${usuario.email}|${new Date().toISOString().slice(0, 7)}`;
      estado.paginasIa.set(clave, (estado.paginasIa.get(clave) ?? 0) + 1);
    }
    estado.alCambiar?.();
    return HttpResponse.json(lecturaDeEjemplo(pedido.data));
  }),
];
