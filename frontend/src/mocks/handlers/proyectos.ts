import { delay, http, HttpResponse } from 'msw';
import { crearProyectoSchema, renombrarProyectoSchema, type Proyecto } from '@/types/proyecto';
import { estado, plantillasMock } from '../db/estado';
import { errorResponse } from '../errores';
import { api, exigirCsrf, exigirSesion, LATENCIA_BACKEND_MS } from './comun';

function privado(request: Request, escritura = false) {
  return (escritura && exigirCsrf(request)) || exigirSesion(request);
}
function buscar(id: string) {
  const item = estado.proyectos.get(id);
  return item?.usuarioId === estado.sesionUsuarioId ? item.proyecto : undefined;
}
function guardar(
  nombre: string,
  descripcion: string | null,
  plantillaId: string | null = null,
  bloques: Proyecto['bloques'] = [],
) {
  const ahora = new Date().toISOString();
  const proyecto: Proyecto = {
    id: crypto.randomUUID(),
    nombre,
    descripcion,
    plantillaId,
    creadoEn: ahora,
    actualizadoEn: ahora,
    bloques,
  };
  estado.proyectos.set(proyecto.id, { usuarioId: String(estado.sesionUsuarioId), proyecto });
  return HttpResponse.json(proyecto, {
    status: 201,
    headers: { Location: `/api/proyectos/${proyecto.id}` },
  });
}
export const handlersProyectos = [
  http.get(api('/proyectos'), async ({ request }) => {
    await delay(LATENCIA_BACKEND_MS);
    const error = privado(request);
    if (error) return error;
    return HttpResponse.json(
      [...estado.proyectos.values()]
        .filter((item) => item.usuarioId === estado.sesionUsuarioId)
        .map((item) => ({ ...item.proyecto, bloques: [] }))
        .sort((a, b) => b.creadoEn.localeCompare(a.creadoEn) || a.id.localeCompare(b.id)),
    );
  }),
  http.post(api('/proyectos'), async ({ request }) => {
    const error = privado(request, true);
    if (error) return error;
    const datos = crearProyectoSchema.safeParse(await request.json());
    if (!datos.success)
      return errorResponse(400, 'Revisá el nombre del proyecto.', '/api/proyectos');
    return guardar(datos.data.nombre, datos.data.descripcion ?? null);
  }),
  http.post(api('/proyectos/desde-plantilla/:plantillaId'), ({ request, params }) => {
    const error = privado(request, true);
    if (error) return error;
    const plantilla = plantillasMock.find((item) => item.id === params.plantillaId);
    if (!plantilla)
      return errorResponse(404, 'Plantilla no encontrada', new URL(request.url).pathname);
    const estructura = plantilla.estructura as {
      bloques: {
        nombre: string;
        campos: {
          nombre: string;
          tipo: Proyecto['bloques'][number]['campos'][number]['tipo'];
          obligatorio?: boolean;
          valorInicial?: string;
          opciones?: string[];
        }[];
      }[];
    };
    const bloques = estructura.bloques.map((bloque, posicion) => ({
      id: crypto.randomUUID(),
      nombre: bloque.nombre,
      posicion,
      campos: bloque.campos.map((campo, posicion) => ({
        id: crypto.randomUUID(),
        nombre: campo.nombre,
        tipo: campo.tipo,
        obligatorio: campo.obligatorio ?? false,
        valorInicial: campo.valorInicial ?? null,
        opciones: [...(campo.opciones ?? [])],
        posicion,
      })),
    }));
    return guardar(plantilla.nombre, plantilla.descripcion, plantilla.id, bloques);
  }),
  http.get(api('/proyectos/:id'), ({ request, params }) => {
    const error = privado(request);
    if (error) return error;
    const proyecto = buscar(String(params.id));
    return proyecto
      ? HttpResponse.json(proyecto)
      : errorResponse(404, 'Proyecto no encontrado', new URL(request.url).pathname);
  }),
  http.patch(api('/proyectos/:id'), async ({ request, params }) => {
    const error = privado(request, true);
    if (error) return error;
    const proyecto = buscar(String(params.id));
    if (!proyecto)
      return errorResponse(404, 'Proyecto no encontrado', new URL(request.url).pathname);
    const datos = renombrarProyectoSchema.safeParse(await request.json());
    if (!datos.success)
      return errorResponse(400, 'Revisá el nombre del proyecto.', new URL(request.url).pathname);
    proyecto.nombre = datos.data.nombre;
    proyecto.actualizadoEn = new Date().toISOString();
    return HttpResponse.json(proyecto);
  }),
  http.delete(api('/proyectos/:id'), ({ request, params }) => {
    const error = privado(request, true);
    if (error) return error;
    if (!buscar(String(params.id)))
      return errorResponse(404, 'Proyecto no encontrado', new URL(request.url).pathname);
    estado.proyectos.delete(String(params.id));
    return new HttpResponse(null, { status: 204 });
  }),
];
