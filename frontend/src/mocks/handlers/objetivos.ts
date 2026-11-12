import { delay, http, HttpResponse } from 'msw';
import { z } from 'zod';
import { hoy } from '@/lib/sistema/formato';
import {
  objetivosDelServidorSchema,
  type MiniObjetivo,
  type ObjetivoDelServidor,
  type TipoDecision,
} from '@/types/objetivos';
import { estado, guardarObjetivosDe, objetivosDe, sistemasDe } from '../db/estado';
import { pasosObjetivoSimulados } from '../db/objetivos';
import {
  candidatosPara,
  datosDeEjemplo,
  lunesDe,
  type DatosAutogestion,
} from '../db/objetivosAutogestionados';
import { zonaDeLaPersona } from '../db/zona';
import { errorResponse } from '../errores';
import { api, cuenta, LATENCIA_BACKEND_MS, pathDe } from './comun';

const nuevoSchema = z.object({
  texto: z.string().trim().min(1, 'El texto es obligatorio').max(500, 'Máximo 500 caracteres'),
  meta: z.number().min(1, 'La meta tiene que ser mayor o igual a 1'),
});

// Igual que el backend: cada cuenta ve solo sus objetivos, y uno ajeno responde igual que uno que no existe.
export const handlersObjetivos = [
  http.get(api('/objetivos'), async ({ request }) => {
    const { usuario, error } = cuenta(request);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    return HttpResponse.json(objetivosDelServidorSchema.parse(objetivosDe(usuario.email)));
  }),
  http.post(api('/objetivos'), async ({ request }) => {
    const { usuario, error } = cuenta(request, true);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    const datos = nuevoSchema.safeParse(await request.json());
    if (!datos.success)
      return errorResponse(
        400,
        'Revisá los datos del objetivo.',
        pathDe(request),
        Object.fromEntries(
          datos.error.issues.map((problema) => [String(problema.path[0]), problema.message]),
        ),
      );
    const creado: ObjetivoDelServidor = {
      id: crypto.randomUUID(),
      texto: datos.data.texto,
      meta: datos.data.meta,
      creadoEn: new Date().toISOString(),
    };
    guardarObjetivosDe(usuario.email, [...objetivosDe(usuario.email), creado]);
    return HttpResponse.json(creado, {
      status: 201,
      headers: { Location: `/api/objetivos/${creado.id}` },
    });
  }),
  http.delete(api('/objetivos/:id'), async ({ request, params }) => {
    const { usuario, error } = cuenta(request, true);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    const propios = objetivosDe(usuario.email);
    if (!propios.some((objetivo) => objetivo.id === params.id))
      return errorResponse(404, 'Objetivo no encontrado', pathDe(request));
    guardarObjetivosDe(
      usuario.email,
      propios.filter((objetivo) => objetivo.id !== params.id),
    );
    return new HttpResponse(null, { status: 204 });
  }),
  http.get(api('/objetivos/pasos'), async ({ request }) => {
    const { usuario, error } = cuenta(request);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    return HttpResponse.json(
      pasosObjetivoSimulados(sistemasDe(usuario.email), hoy(), zonaDeLaPersona()),
    );
  }),
  ...handlersAutogestion(),
];

const decidirSchema = z.object({ tipo: z.enum(['ACEPTADO', 'IGNORADO']) });
const reemplazoSchema = z.object({
  tipo: z.string(),
  registroId: z.string().nullable().optional(),
  referencia: z.string().nullable().optional(),
});

/** Los datos del objetivo si es de la cuenta; uno ajeno responde igual que uno que no existe. */
function datosDe(request: Request, objetivoId: string, escritura = false) {
  const { usuario, error } = cuenta(request, escritura);
  if (error) return { error };
  const objetivo = objetivosDe(usuario.email).find((item) => item.id === objetivoId);
  if (!objetivo) return { error: errorResponse(404, 'Objetivo no encontrado', pathDe(request)) };
  let datos = estado.autogestion.get(objetivo.id);
  if (!datos) {
    datos = datosDeEjemplo(objetivo, hoy());
    estado.autogestion.set(objetivo.id, datos);
  }
  return { datos, objetivo };
}

function miniEnCurso(request: Request, datos: DatosAutogestion, miniId: string) {
  const mini = datos.minis.find((item) => item.id === miniId);
  return mini
    ? { mini }
    : { error: errorResponse(404, 'Mini-objetivo no encontrado', pathDe(request)) };
}

const pasarAlHistorial = (datos: DatosAutogestion, mini: MiniObjetivo) => {
  datos.minis = datos.minis.filter((item) => item.id !== mini.id);
  datos.historial = [mini, ...datos.historial];
};

const param = (valor: string | readonly string[] | undefined) => String(valor);

function handlersAutogestion() {
  const ruta = (resto: string) => api(`/objetivos/:id${resto}`);
  return [
    http.get(ruta('/mini-objetivos'), async ({ request, params }) => {
      const { datos, error } = datosDe(request, param(params.id));
      if (error) return error;
      await delay(LATENCIA_BACKEND_MS);
      return HttpResponse.json({
        minis: datos.minis,
        historial: datos.historial,
        faltanDatos: null,
      });
    }),
    http.get(ruta('/mini-objetivos/:miniId/candidatos'), async ({ request, params }) => {
      const { datos, error } = datosDe(request, param(params.id));
      if (error) return error;
      const { mini, error: sinMini } = miniEnCurso(request, datos, param(params.miniId));
      if (sinMini) return sinMini;
      await delay(LATENCIA_BACKEND_MS);
      return HttpResponse.json(candidatosPara(datos, mini));
    }),
    http.post(ruta('/mini-objetivos/:miniId/reemplazo'), async ({ request, params }) => {
      const { datos, error } = datosDe(request, param(params.id), true);
      if (error) return error;
      const { mini, error: sinMini } = miniEnCurso(request, datos, param(params.miniId));
      if (sinMini) return sinMini;
      const pedido = reemplazoSchema.safeParse(await request.json());
      const candidato = candidatosPara(datos, mini).find(
        (item) => pedido.success && item.tipo === pedido.data.tipo,
      );
      if (!candidato)
        return errorResponse(400, 'Ese candidato ya no está disponible', pathDe(request));
      await delay(LATENCIA_BACKEND_MS);
      const nuevo: MiniObjetivo = {
        ...candidato,
        id: crypto.randomUUID(),
        posicion: mini.posicion,
        estado: 'EN_CURSO',
        valorActual: candidato.valorInicial,
        avance: 0,
        semanaInicio: lunesDe(hoy()),
        textoConIa: false,
      };
      pasarAlHistorial(datos, { ...mini, estado: 'REEMPLAZADO' });
      datos.minis = [...datos.minis, nuevo].sort((a, b) => a.posicion - b.posicion);
      return HttpResponse.json(nuevo);
    }),
    http.delete(ruta('/mini-objetivos/:miniId'), async ({ request, params }) => {
      const { datos, error } = datosDe(request, param(params.id), true);
      if (error) return error;
      const { mini, error: sinMini } = miniEnCurso(request, datos, param(params.miniId));
      if (sinMini) return sinMini;
      await delay(LATENCIA_BACKEND_MS);
      pasarAlHistorial(datos, { ...mini, estado: 'ABANDONADO' });
      return new HttpResponse(null, { status: 204 });
    }),
    http.post(ruta('/mini-objetivos/:miniId/decisiones'), async ({ request, params }) => {
      const { datos, error } = datosDe(request, param(params.id), true);
      if (error) return error;
      const { mini, error: sinMini } = miniEnCurso(request, datos, param(params.miniId));
      if (sinMini) return sinMini;
      const pedido = decidirSchema.safeParse(await request.json());
      if (!pedido.success) return errorResponse(400, 'La decisión es obligatoria', pathDe(request));
      await delay(LATENCIA_BACKEND_MS);
      const tipo: TipoDecision =
        pedido.data.tipo === 'ACEPTADO' ? 'PASO_ACEPTADO' : 'PASO_IGNORADO';
      const decision = {
        id: crypto.randomUUID(),
        tipo,
        miniObjetivoId: mini.id,
        avisoId: null,
        registroId: mini.registroId,
        descripcion: mini.titulo,
        tomadaEn: new Date().toISOString(),
        semanaInicio: lunesDe(hoy()),
        efecto: 'SIN_MEDICION' as const,
        deltaIngresoSemanal: null,
      };
      datos.decisiones = [...datos.decisiones, decision];
      return HttpResponse.json(decision, { status: 201 });
    }),
    http.get(ruta('/progreso'), async ({ request, params }) => {
      const { datos, objetivo, error } = datosDe(request, param(params.id));
      if (error) return error;
      await delay(LATENCIA_BACKEND_MS);
      return HttpResponse.json({
        meta: objetivo.meta,
        hito: null,
        semanas: datos.progreso,
        minis: [...datos.minis, ...datos.historial.filter((item) => item.estado === 'LOGRADO')].map(
          (mini) => ({
            mini,
            hito: mini.estado === 'LOGRADO' ? 'MINI_LOGRADO' : null,
            semanas: [{ semana: lunesDe(hoy()), valor: mini.valorActual, estimado: false }],
          }),
        ),
      });
    }),
    http.get(ruta('/decisiones'), async ({ request, params }) => {
      const { datos, error } = datosDe(request, param(params.id));
      if (error) return error;
      await delay(LATENCIA_BACKEND_MS);
      const semanas = [...new Set(datos.decisiones.map((item) => item.semanaInicio))]
        .sort()
        .reverse();
      return HttpResponse.json({
        semanas: semanas.map((semanaInicio) => ({
          semanaInicio,
          decisiones: datos.decisiones.filter((item) => item.semanaInicio === semanaInicio),
        })),
      });
    }),
    http.get(ruta('/semanas'), async ({ request, params }) => {
      const { datos, error } = datosDe(request, param(params.id));
      if (error) return error;
      await delay(LATENCIA_BACKEND_MS);
      return HttpResponse.json({
        ultimoCierre: datos.semanas[0]?.semana ?? null,
        semanas: datos.semanas,
      });
    }),
  ];
}
