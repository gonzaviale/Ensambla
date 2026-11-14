import { delay, http, HttpResponse } from 'msw';
import { z } from 'zod';
import { hoy } from '@/lib/sistema/formato';
import {
  AGRUPACIONES_DE_METRICA,
  CALCULOS_DE_METRICA,
  FUENTES_DE_METRICA,
  PERIODOS_DE_METRICA,
} from '@/types/metricas';
import { estado, sistemasDe } from '../db/estado';
import {
  calcularMetricaDeEjemplo,
  MAXIMO_DE_METRICAS,
  sugeridasDe,
} from '../db/metricas';
import { errorResponse } from '../errores';
import { api, cuenta, LATENCIA_BACKEND_MS, pathDe } from './comun';

const definicionSchema = z.object({
  nombre: z.string().trim().min(1, 'Ponele un nombre a la métrica').max(80, 'Máximo 80 caracteres'),
  fuente: z.enum(FUENTES_DE_METRICA),
  calculo: z.enum(CALCULOS_DE_METRICA),
  agrupar: z.enum(AGRUPACIONES_DE_METRICA),
  periodo: z.enum(PERIODOS_DE_METRICA),
  soloFiado: z.boolean(),
});
const nuevaSchema = definicionSchema.extend({ autoria: z.enum(['SUGERIDA', 'PROPIA']) });

const mismaReceta = (a: z.infer<typeof definicionSchema>, b: z.infer<typeof definicionSchema>) =>
  JSON.stringify({ ...a, nombre: '' }) === JSON.stringify({ ...b, nombre: '' });

// Igual que el backend: cada cuaderno es de una cuenta, y uno ajeno responde igual que uno que no existe.
function cuaderno(request: Request, sistemaId: string, escritura = false) {
  const sesion = cuenta(request, escritura);
  if (sesion.error) return { error: sesion.error };
  const sistema = sistemasDe(sesion.usuario.email).find((otro) => otro.id === sistemaId);
  if (!sistema) return { error: errorResponse(404, 'Proyecto no encontrado', pathDe(request)) };
  return { sistema };
}

const guardadasDe = (sistemaId: string) => estado.metricas.get(sistemaId) ?? [];

export const handlersMetricas = [
  http.get(api('/proyectos/:sistemaId/metricas'), async ({ request, params }) => {
    const { sistema, error } = cuaderno(request, String(params.sistemaId));
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    return HttpResponse.json(
      guardadasDe(sistema.id).map((metrica, orden) => ({
        ...metrica,
        orden,
        resultado: calcularMetricaDeEjemplo(metrica.definicion, sistema, hoy()),
      })),
    );
  }),
  http.get(api('/proyectos/:sistemaId/metricas/sugeridas'), async ({ request, params }) => {
    const { sistema, error } = cuaderno(request, String(params.sistemaId));
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    return HttpResponse.json(
      sugeridasDe(
        sistema,
        guardadasDe(sistema.id).map((metrica) => metrica.definicion),
        hoy(),
      ),
    );
  }),
  http.post(api('/proyectos/:sistemaId/metricas/previsualizar'), async ({ request, params }) => {
    const { sistema, error } = cuaderno(request, String(params.sistemaId), true);
    if (error) return error;
    const receta = definicionSchema.safeParse(await request.json());
    if (!receta.success) return errorResponse(400, 'Revisá la métrica.', pathDe(request));
    await delay(LATENCIA_BACKEND_MS);
    return HttpResponse.json(calcularMetricaDeEjemplo(receta.data, sistema, hoy()));
  }),
  http.post(api('/proyectos/:sistemaId/metricas'), async ({ request, params }) => {
    const { sistema, error } = cuaderno(request, String(params.sistemaId), true);
    if (error) return error;
    const nueva = nuevaSchema.safeParse(await request.json());
    if (!nueva.success)
      return errorResponse(
        400,
        'Revisá la métrica.',
        pathDe(request),
        Object.fromEntries(nueva.error.issues.map((item) => [String(item.path[0]), item.message])),
      );
    const guardadas = guardadasDe(sistema.id);
    if (guardadas.length >= MAXIMO_DE_METRICAS)
      return errorResponse(
        409,
        `Ya tenés ${MAXIMO_DE_METRICAS} métricas. Quitá alguna para agregar otra.`,
        pathDe(request),
      );
    if (guardadas.some((otra) => mismaReceta(otra.definicion, nueva.data)))
      return errorResponse(409, 'Ya tenés una métrica igual a esa.', pathDe(request));
    await delay(LATENCIA_BACKEND_MS);
    const { autoria, ...definicion } = nueva.data;
    const creada = { id: crypto.randomUUID(), definicion, autoria };
    estado.metricas.set(sistema.id, [...guardadas, creada]);
    return HttpResponse.json(
      {
        ...creada,
        orden: guardadas.length,
        resultado: calcularMetricaDeEjemplo(definicion, sistema, hoy()),
      },
      { status: 201, headers: { Location: `/api/proyectos/${sistema.id}/metricas/${creada.id}` } },
    );
  }),
  http.delete(api('/proyectos/:sistemaId/metricas/:metricaId'), async ({ request, params }) => {
    const { sistema, error } = cuaderno(request, String(params.sistemaId), true);
    if (error) return error;
    const guardadas = guardadasDe(sistema.id);
    if (!guardadas.some((metrica) => metrica.id === params.metricaId))
      return errorResponse(404, 'Métrica no encontrada', pathDe(request));
    await delay(LATENCIA_BACKEND_MS);
    estado.metricas.set(
      sistema.id,
      guardadas.filter((metrica) => metrica.id !== params.metricaId),
    );
    return new HttpResponse(null, { status: 204 });
  }),
];
