import { delay, http, HttpResponse } from 'msw';
import { z } from 'zod';
import { pasosPara } from '@/lib/pedidos';
import { estadoDeOrdenSchema, ordenesDelServidorSchema } from '@/types/pedidos';
import { guardarOrdenesDe, ordenesDe } from '../db/estado';
import { errorResponse } from '../errores';
import { api, cuenta, LATENCIA_BACKEND_MS, pathDe } from './comun';

const cambioSchema = z.object({
  estado: estadoDeOrdenSchema,
  motivo: z.string().max(300).nullish(),
});

// Igual que el backend: cada cuenta ve solo sus órdenes, una ajena responde igual que una que no existe y los pasos
// son los permitidos (aceptar o rechazar, preparar, dejar lista). El mock no maneja el stock: lo descuenta el servidor real.
export const handlersPedidos = [
  http.get(api('/ordenes'), async ({ request }) => {
    const { usuario, error } = cuenta(request);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    const ordenadas = [...ordenesDe(usuario.email)].sort((a, b) =>
      b.creadoEn.localeCompare(a.creadoEn),
    );
    return HttpResponse.json(ordenesDelServidorSchema.parse(ordenadas));
  }),
  http.patch(api('/ordenes/:id/estado'), async ({ request, params }) => {
    const { usuario, error } = cuenta(request, true);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    const cambio = cambioSchema.safeParse(await request.json());
    if (!cambio.success)
      return errorResponse(400, 'Revisá el paso que querés dar.', pathDe(request));
    const propias = ordenesDe(usuario.email);
    const orden = propias.find((posible) => posible.id === params.id);
    if (!orden) return errorResponse(404, 'La orden no existe', pathDe(request));
    if (!pasosPara(orden.estado).some((paso) => paso.estado === cambio.data.estado))
      return errorResponse(400, 'Esa orden ya no puede dar ese paso.', pathDe(request));
    const motivo = cambio.data.motivo?.trim();
    const actualizada = {
      ...orden,
      estado: cambio.data.estado,
      motivoRechazo: cambio.data.estado === 'RECHAZADA' && motivo ? motivo : null,
      actualizadoEn: new Date().toISOString(),
    };
    guardarOrdenesDe(
      usuario.email,
      propias.map((posible) => (posible.id === orden.id ? actualizada : posible)),
    );
    return HttpResponse.json(actualizada);
  }),
];
