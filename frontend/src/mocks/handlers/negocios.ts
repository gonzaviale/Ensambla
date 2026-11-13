import { delay, http, HttpResponse } from 'msw';
import { MAXIMO_DIAS_DEL_RESUMEN } from '@/lib/sistema/periodoResumen';
import { DIAS_DEL_RESUMEN_CONJUNTO } from '@/api/negocios';
import { nuevoNegocioSchema } from '@/types/negocios';
import {
  aNegocioDeLaCuenta,
  guardarNegociosDe,
  limiteDeNegociosDe,
  listaDeNegociosDe,
  negociosDe,
  resumenConjuntoDe,
} from '../db/negocios';
import { RUBROS_DE_PRUEBA } from '../db/ubicacion';
import { errorResponse } from '../errores';
import { api, cuenta, LATENCIA_BACKEND_MS, pathDe } from './comun';

const SIN_CACHE = { headers: { 'Cache-Control': 'no-store' } };

// Los negocios de la cuenta (docs/negocios-de-la-cuenta.md). El negocio nunca viaja en el body ni en la ruta de
// las demás pantallas: sale de la sesión; acá sólo se elige, se crea o se elimina. Un id ajeno responde 404.
export const handlersNegocios = [
  http.get(api('/negocios/resumen'), async ({ request }) => {
    await delay(LATENCIA_BACKEND_MS);
    const sesion = cuenta(request);
    if (sesion.error) return sesion.error;
    const consulta = new URL(request.url).searchParams;
    const dias = consulta.has('dias') ? Number(consulta.get('dias')) : DIAS_DEL_RESUMEN_CONJUNTO;
    if (!Number.isInteger(dias) || dias < 1 || dias > MAXIMO_DIAS_DEL_RESUMEN) {
      return errorResponse(400, 'La cantidad de días no es válida', pathDe(request));
    }
    return HttpResponse.json(resumenConjuntoDe(sesion.usuario, dias), SIN_CACHE);
  }),
  http.get(api('/negocios'), async ({ request }) => {
    await delay(LATENCIA_BACKEND_MS);
    const sesion = cuenta(request);
    if (sesion.error) return sesion.error;
    return HttpResponse.json(listaDeNegociosDe(sesion.usuario), SIN_CACHE);
  }),
  http.post(api('/negocios'), async ({ request }) => {
    await delay(LATENCIA_BACKEND_MS);
    const sesion = cuenta(request, true);
    if (sesion.error) return sesion.error;
    const datos = nuevoNegocioSchema.safeParse(await request.json().catch(() => null));
    if (!datos.success) return errorResponse(400, 'Revisá los datos del negocio.', pathDe(request));
    const rubro = datos.data.rubro ?? 'OTRO';
    if (!RUBROS_DE_PRUEBA.some((existente) => existente.codigo === rubro))
      return errorResponse(400, 'El rubro no existe.', pathDe(request));
    const { activoId, negocios } = negociosDe(sesion.usuario);
    const limite = limiteDeNegociosDe(sesion.usuario.email);
    if (negocios.length >= limite) {
      return HttpResponse.json(
        {
          timestamp: new Date().toISOString().slice(0, 19),
          status: 403,
          error: 'Forbidden',
          message: `Tu plan permite hasta ${limite} ${limite === 1 ? 'negocio' : 'negocios'}. Pasate a Premium para sumar más.`,
          path: pathDe(request),
          traceId: crypto.randomUUID(),
          codigo: 'LIMITE_NEGOCIOS',
        },
        { status: 403 },
      );
    }
    const nuevo = {
      id: crypto.randomUUID(),
      nombre: datos.data.nombre,
      rubro,
      creadoEn: new Date().toISOString(),
    };
    guardarNegociosDe(sesion.usuario.email, { activoId, negocios: [...negocios, nuevo] });
    return HttpResponse.json(aNegocioDeLaCuenta(nuevo, activoId), { status: 201, ...SIN_CACHE });
  }),
  http.put(api('/negocios/:id/activo'), async ({ request, params }) => {
    await delay(LATENCIA_BACKEND_MS);
    const sesion = cuenta(request, true);
    if (sesion.error) return sesion.error;
    const { negocios } = negociosDe(sesion.usuario);
    if (!negocios.some((negocio) => negocio.id === params.id))
      return errorResponse(404, 'Negocio no encontrado', pathDe(request));
    guardarNegociosDe(sesion.usuario.email, { activoId: String(params.id), negocios });
    return HttpResponse.json(listaDeNegociosDe(sesion.usuario), SIN_CACHE);
  }),
  http.delete(api('/negocios/:id'), async ({ request, params }) => {
    await delay(LATENCIA_BACKEND_MS);
    const sesion = cuenta(request, true);
    if (sesion.error) return sesion.error;
    const { activoId, negocios } = negociosDe(sesion.usuario);
    if (!negocios.some((negocio) => negocio.id === params.id))
      return errorResponse(404, 'Negocio no encontrado', pathDe(request));
    if (negocios.length === 1)
      return errorResponse(
        409,
        'No podés eliminar el único negocio de tu cuenta.',
        pathDe(request),
      );
    const restantes = negocios.filter((negocio) => negocio.id !== params.id);
    // Si se elimina el activo, pasa a serlo el más antiguo de los que quedan.
    const nuevoActivo = activoId === params.id ? (restantes.at(0)?.id ?? activoId) : activoId;
    guardarNegociosDe(sesion.usuario.email, { activoId: nuevoActivo, negocios: restantes });
    return new HttpResponse(null, { status: 204 });
  }),
];
