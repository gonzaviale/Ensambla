import { delay, http, HttpResponse } from 'msw';
import type { Bloque as BloqueResponse } from '@/types/bloque';
import type { Bloque, Proyecto } from '@/types/proyecto';
import { mismoNombre } from '@/lib/campos';
import { estado } from '../db/estado';
import {
  bloqueDeLaSesion,
  fechasDe,
  proyectoDeLaSesion,
  registrarAlta,
  registrarCambio,
  siguientePosicion,
} from '../db/estructura';
import { errorResponse } from '../errores';
import { api, exigirCsrf, exigirSesion, LATENCIA_BACKEND_MS } from './comun';

// Mismo contrato que el backend de T-017. Como el backend ya lo implementa, sólo se usa con
// VITE_API_MOCKS=completo (y en los tests).
const MAX_NOMBRE = 120;

const pathDe = (request: Request) => new URL(request.url).pathname;

function privado(request: Request, escritura = false) {
  return (escritura && exigirCsrf(request)) || exigirSesion(request);
}

const aRespuesta = (proyecto: Proyecto, { id, nombre, posicion }: Bloque): BloqueResponse => ({
  id,
  proyectoId: proyecto.id,
  nombre,
  posicion,
  ...fechasDe(id, proyecto.creadoEn),
});

// Valida el cuerpo { nombre } como el backend: 400 con errors.nombre o 409 si ya existe.
async function leerNombre(request: Request, proyecto: Proyecto, propioId?: string) {
  const cuerpo: unknown = await request.json().catch(() => null);
  if (!cuerpo || typeof cuerpo !== 'object') {
    return { error: errorResponse(400, 'El cuerpo JSON no es válido', pathDe(request)) };
  }
  const { nombre } = cuerpo as { nombre?: unknown };
  const texto = typeof nombre === 'string' ? nombre.trim() : '';
  const problema = !texto
    ? 'El nombre es obligatorio'
    : texto.length > MAX_NOMBRE
      ? `El nombre puede tener hasta ${MAX_NOMBRE} caracteres`
      : undefined;
  if (problema) {
    return {
      error: errorResponse(400, 'Error de validación en los datos enviados', pathDe(request), {
        nombre: problema,
      }),
    };
  }
  if (proyecto.bloques.some((item) => item.id !== propioId && mismoNombre(item.nombre, texto))) {
    return {
      error: errorResponse(
        409,
        `Ya hay un bloque llamado "${texto}" en este proyecto`,
        pathDe(request),
      ),
    };
  }
  return { nombre: texto };
}

export const handlersBloques = [
  http.get(api('/proyectos/:proyectoId/bloques'), async ({ request, params }) => {
    await delay(LATENCIA_BACKEND_MS);
    const error = privado(request);
    if (error) return error;
    const proyecto = proyectoDeLaSesion(String(params.proyectoId));
    if (!proyecto) return errorResponse(404, 'Proyecto no encontrado', pathDe(request));
    return HttpResponse.json(
      [...proyecto.bloques]
        .sort((a, b) => a.posicion - b.posicion)
        .map((bloque) => aRespuesta(proyecto, bloque)),
    );
  }),

  http.post(api('/proyectos/:proyectoId/bloques'), async ({ request, params }) => {
    await delay(LATENCIA_BACKEND_MS);
    const error = privado(request, true);
    if (error) return error;
    const proyecto = proyectoDeLaSesion(String(params.proyectoId));
    if (!proyecto) return errorResponse(404, 'Proyecto no encontrado', pathDe(request));
    const leido = await leerNombre(request, proyecto);
    if ('error' in leido) return leido.error;

    const bloque: Bloque = {
      id: crypto.randomUUID(),
      nombre: leido.nombre,
      posicion: siguientePosicion(proyecto.bloques),
      campos: [],
    };
    proyecto.bloques.push(bloque);
    registrarAlta(bloque.id);
    return HttpResponse.json(aRespuesta(proyecto, bloque), {
      status: 201,
      headers: { Location: `/api/bloques/${bloque.id}` },
    });
  }),

  http.patch(api('/bloques/:bloqueId'), async ({ request, params }) => {
    await delay(LATENCIA_BACKEND_MS);
    const error = privado(request, true);
    if (error) return error;
    const ubicado = bloqueDeLaSesion(String(params.bloqueId));
    if (!ubicado) return errorResponse(404, 'El bloque no existe', pathDe(request));
    const { proyecto, bloque } = ubicado;
    const leido = await leerNombre(request, proyecto, bloque.id);
    if ('error' in leido) return leido.error;

    bloque.nombre = leido.nombre;
    registrarCambio(bloque.id, proyecto.creadoEn);
    return HttpResponse.json(aRespuesta(proyecto, bloque));
  }),

  // Borra en cascada los campos y los registros del bloque.
  http.delete(api('/bloques/:bloqueId'), async ({ request, params }) => {
    await delay(LATENCIA_BACKEND_MS);
    const error = privado(request, true);
    if (error) return error;
    const ubicado = bloqueDeLaSesion(String(params.bloqueId));
    if (!ubicado) return errorResponse(404, 'El bloque no existe', pathDe(request));
    const { proyecto, bloque } = ubicado;

    // Como el backend: no se borra una tabla con la que se conecta una columna de otra tabla.
    for (const otra of proyecto.bloques) {
      if (otra.id === bloque.id) continue;
      const columna = otra.campos.find(
        (campo) => campo.tipo === 'RELACION' && campo.bloqueRelacionadoId === bloque.id,
      );
      if (columna) {
        return errorResponse(
          400,
          `La tabla «${bloque.nombre}» se usa en la columna «${columna.nombre}» de «${otra.nombre}»`,
          pathDe(request),
        );
      }
    }

    proyecto.bloques = proyecto.bloques.filter((item) => item.id !== bloque.id);
    for (const registro of [...estado.registros.values()]) {
      if (registro.bloqueId === bloque.id) estado.registros.delete(registro.id);
    }
    for (const campo of bloque.campos) estado.fechas.delete(campo.id);
    estado.fechas.delete(bloque.id);
    return new HttpResponse(null, { status: 204 });
  }),
];
