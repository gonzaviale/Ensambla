import { delay, http, HttpResponse } from 'msw';
import type { Bloque } from '@/types/proyecto';
import { crearRegistroSchema, type Registro, type Valor } from '@/types/registro';
import { validarFormulario, valoresParaGuardar } from '@/lib/formularioRegistro';
import { estado } from '../db/estado';
import { errorResponse } from '../errores';
import { api, exigirCsrf, exigirSesion, LATENCIA_BACKEND_MS } from './comun';

// Mismo contrato que GET /api/bloques/{id}/registros y DELETE /api/registros/{id} del backend.
const TAMANIO_POR_DEFECTO = 20;
const TAMANIO_MAXIMO = 100;
const ORDEN_POR_CARGA = 'creadoEn';

const pathDe = (request: Request) => new URL(request.url).pathname;

function privado(request: Request, escritura = false) {
  return (escritura && exigirCsrf(request)) || exigirSesion(request);
}

function bloqueDeLaSesion(id: string): Bloque | undefined {
  for (const { usuarioId, proyecto } of estado.proyectos.values()) {
    if (usuarioId !== estado.sesionUsuarioId) continue;
    const bloque = proyecto.bloques.find((item) => item.id === id);
    if (bloque) return bloque;
  }
  return undefined;
}

const sinDato = (valor: Valor | undefined) => valor === undefined || valor === null;

function comparar(a: Valor | undefined, b: Valor | undefined) {
  if (typeof a === 'number' && typeof b === 'number') return a - b;
  if (typeof a === 'boolean' && typeof b === 'boolean') return Number(a) - Number(b);
  return String(a).toLowerCase().localeCompare(String(b).toLowerCase(), 'es');
}

// Los registros sin el dato quedan al final en ambos sentidos; el empate se resuelve por carga.
function ordenar(registros: Registro[], campoId: string | null, descendente: boolean) {
  const signo = descendente ? -1 : 1;
  return [...registros].sort((a, b) => {
    if (campoId) {
      const valorA = a.valores[campoId];
      const valorB = b.valores[campoId];
      if (sinDato(valorA) !== sinDato(valorB)) return sinDato(valorA) ? 1 : -1;
      const orden = sinDato(valorA) ? 0 : comparar(valorA, valorB);
      if (orden !== 0) return signo * orden;
    }
    return signo * (a.creadoEn.localeCompare(b.creadoEn) || a.id.localeCompare(b.id));
  });
}

const coincide = (registro: Registro, texto: string) =>
  Object.values(registro.valores).some(
    (valor) => valor !== null && String(valor).toLowerCase().includes(texto),
  );

export const handlersRegistros = [
  http.get(api('/bloques/:id/registros'), async ({ request, params }) => {
    await delay(LATENCIA_BACKEND_MS);
    const error = privado(request);
    if (error) return error;
    const bloque = bloqueDeLaSesion(String(params.id));
    if (!bloque) return errorResponse(404, 'El bloque no existe', pathDe(request));

    const consulta = new URL(request.url).searchParams;
    const texto = consulta.get('q')?.trim().toLowerCase() ?? '';
    const orden = consulta.get('orden')?.trim() || ORDEN_POR_CARGA;
    const direccion = consulta.get('direccion')?.trim().toLowerCase() || 'desc';
    if (direccion !== 'asc' && direccion !== 'desc') {
      return errorResponse(400, 'La dirección del orden tiene que ser asc o desc', pathDe(request));
    }
    if (orden !== ORDEN_POR_CARGA && !bloque.campos.some((campo) => campo.id === orden)) {
      return errorResponse(400, 'No se puede ordenar por ese campo', pathDe(request));
    }
    const pagina = Math.max(0, Number(consulta.get('pagina')) || 0);
    const tamanio = Math.min(
      TAMANIO_MAXIMO,
      Math.max(1, Number(consulta.get('tamanio')) || TAMANIO_POR_DEFECTO),
    );

    const delBloque = [...estado.registros.values()].filter(
      (registro) => registro.bloqueId === bloque.id && (!texto || coincide(registro, texto)),
    );
    const ordenados = ordenar(
      delBloque,
      orden === ORDEN_POR_CARGA ? null : orden,
      direccion === 'desc',
    );
    const totalPages = Math.max(1, Math.ceil(ordenados.length / tamanio));
    return HttpResponse.json({
      content: ordenados.slice(pagina * tamanio, (pagina + 1) * tamanio),
      pageNumber: pagina,
      pageSize: tamanio,
      totalElements: ordenados.length,
      totalPages,
      first: pagina === 0,
      last: pagina >= totalPages - 1,
    });
  }),

  http.delete(api('/registros/:id'), ({ request, params }) => {
    const error = privado(request, true);
    if (error) return error;
    const registro = estado.registros.get(String(params.id));
    if (!registro || !bloqueDeLaSesion(registro.bloqueId)) {
      return errorResponse(404, 'El registro no existe', pathDe(request));
    }
    estado.registros.delete(registro.id);
    return new HttpResponse(null, { status: 204 });
  }),

  http.post(api('/bloques/:bloqueId/registros'), async ({ request, params }) => {
    const error = privado(request, true);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    const path = pathDe(request);
    const bloque = bloqueDeLaSesion(String(params.bloqueId));
    if (!bloque) return errorResponse(404, 'Bloque no encontrado', path);
    const datos = crearRegistroSchema.safeParse(await request.json());
    if (!datos.success) return errorResponse(400, 'Revisá los datos del registro', path);
    const valores = Object.fromEntries(
      Object.entries(datos.data.valores).map(([id, valor]) => [id, String(valor)]),
    );
    const errores = validarFormulario(bloque.campos, valores);
    for (const [id, valor] of Object.entries(datos.data.valores)) {
      const campo = bloque.campos.find((campo) => campo.id === id);
      if (!campo) errores[id] = 'Este campo no pertenece al bloque';
      else if (campo.tipo === 'NUMERO' && typeof valor !== 'number')
        errores[id] = 'Tiene que ser un número';
      else if (campo.tipo === 'SI_NO' && typeof valor !== 'boolean')
        errores[id] = 'Tiene que ser sí o no';
      else if (campo.tipo !== 'NUMERO' && campo.tipo !== 'SI_NO' && typeof valor !== 'string')
        errores[id] = 'Tiene que ser un texto';
    }
    if (Object.keys(errores).length)
      return errorResponse(400, 'Revisá los campos marcados', path, errores);
    const ahora = new Date().toISOString();
    const registro: Registro = {
      id: crypto.randomUUID(),
      bloqueId: bloque.id,
      valores: valoresParaGuardar(bloque.campos, valores),
      creadoEn: ahora,
      actualizadoEn: ahora,
    };
    estado.registros.set(registro.id, registro);
    return HttpResponse.json(registro, { status: 201 });
  }),
];
