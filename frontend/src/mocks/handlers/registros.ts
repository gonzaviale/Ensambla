import { delay, http, HttpResponse } from 'msw';
import { crearRegistroSchema, type Registro } from '@/types/registro';
import { validarFormulario, valoresParaGuardar } from '@/lib/formularioRegistro';
import { estado } from '../db/estado';
import { errorResponse } from '../errores';
import { api, exigirCsrf, exigirSesion, LATENCIA_BACKEND_MS } from './comun';

export const handlersRegistros = [
  http.post(api('/bloques/:bloqueId/registros'), async ({ request, params }) => {
    const error = exigirCsrf(request) || exigirSesion(request);
    if (error) return error;
    await delay(LATENCIA_BACKEND_MS);
    const path = new URL(request.url).pathname;
    const bloque = [...estado.proyectos.values()]
      .filter((item) => item.usuarioId === estado.sesionUsuarioId)
      .flatMap((item) => item.proyecto.bloques)
      .find((item) => item.id === params.bloqueId);
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
    estado.registros.set(registro.id, { usuarioId: String(estado.sesionUsuarioId), registro });
    return HttpResponse.json(registro, { status: 201 });
  }),
];
