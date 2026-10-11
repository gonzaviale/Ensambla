import { delay, http, HttpResponse } from 'msw';
import type { Lectura } from '@/types/lectura';
import { MAX_FOTOS_POR_LOTE, type Lote } from '@/types/lote';
import { errorResponse } from '../errores';
import { api, exigirCsrf, exigirSesion, LATENCIA_BACKEND_MS } from './comun';

// Cantidad de veces que se consultó la lectura de cada lote creado por el mock.
const consultas = new Map<string, number>();

const PROPUESTA_EJEMPLO: NonNullable<Lectura['propuesta']> = {
  tipoRegistro: 'VENTAS',
  nombre: 'Ventas',
  campos: [
    { clave: 'producto', nombre: 'Producto', tipo: 'TEXTO' },
    { clave: 'monto', nombre: 'Monto', tipo: 'NUMERO' },
  ],
  filas: [
    {
      pagina: 1,
      renglon: 1,
      datos: {
        producto: { valor: 'Pan', confianza: 0.98 },
        monto: { valor: '1200', confianza: 0.95 },
      },
    },
    {
      pagina: 1,
      renglon: 2,
      datos: {
        producto: { valor: 'Leche', confianza: 0.9 },
        monto: { valor: '850', confianza: 0.4 },
      },
    },
  ],
  advertencias: [],
};

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
    consultas.set(lote.id, 0);
    return HttpResponse.json(lote, {
      status: 201,
      headers: { Location: `/api/lotes/${lote.id}` },
    });
  }),

  // Simula GET /api/lotes/{id}/lectura: la primera consulta responde PROCESANDO y las siguientes LISTO.
  http.get(api('/lotes/:id/lectura'), async ({ request, params }) => {
    await delay(LATENCIA_BACKEND_MS);
    const error = exigirSesion(request);
    if (error) return error;
    const loteId = String(params.id);
    const vistas = consultas.get(loteId);
    if (vistas === undefined) {
      return errorResponse(404, 'No se encontró el lote', `/api/lotes/${loteId}/lectura`);
    }
    consultas.set(loteId, vistas + 1);
    const lectura: Lectura =
      vistas === 0
        ? { loteId, estado: 'PROCESANDO', intentos: 1 }
        : { loteId, estado: 'LISTO', intentos: 1, propuesta: PROPUESTA_EJEMPLO };
    return HttpResponse.json(lectura);
  }),

  http.post(api('/lotes/:id/reintentar'), async ({ request, params }) => {
    await delay(LATENCIA_BACKEND_MS);
    const error = exigirCsrf(request) ?? exigirSesion(request);
    if (error) return error;
    consultas.set(String(params.id), 0);
    return new HttpResponse(null, { status: 202 });
  }),
];
