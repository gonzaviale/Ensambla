import { delay, http, HttpResponse } from 'msw';
import { MAX_FOTOS_POR_LOTE, type Lote } from '@/types/lote';
import { errorResponse } from '../errores';
import { api, exigirCsrf, exigirSesion, LATENCIA_BACKEND_MS } from './comun';

// Simula POST /api/lotes (T-090): valida la cantidad y crea un lote pendiente. No guarda los archivos.
export const handlersLotes = [
  http.post(api('/lotes'), async ({ request }) => {
    await delay(LATENCIA_BACKEND_MS);
    const error = exigirCsrf(request) ?? exigirSesion(request);
    if (error) return error;
    const fotos = (await request.formData()).getAll('fotos');
    if (fotos.length === 0) {
      return errorResponse(400, 'Falta el parámetro: fotos', '/api/lotes');
    }
    if (fotos.length > MAX_FOTOS_POR_LOTE) {
      return errorResponse(400, 'Podés subir hasta 10 fotos por lote', '/api/lotes');
    }
    const lote: Lote = {
      id: crypto.randomUUID(),
      estado: 'PENDIENTE',
      creadoEn: new Date().toISOString(),
      fotos: fotos.map((foto, posicion) => ({
        id: crypto.randomUUID(),
        posicion,
        tipoContenido: foto instanceof Blob ? foto.type : 'image/jpeg',
        tamanioBytes: foto instanceof Blob ? foto.size : 0,
      })),
    };
    return HttpResponse.json(lote, {
      status: 201,
      headers: { Location: `/api/lotes/${lote.id}` },
    });
  }),
];
