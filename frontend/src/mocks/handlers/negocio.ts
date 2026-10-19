import { http, HttpResponse } from 'msw';
import { cambiosNegocioSchema, type ConfiguracionNegocio } from '@/types/negocio';
import { estado } from '../db/estado';
import { RUBROS_DE_PRUEBA } from '../db/ubicacion';
import { errorResponse } from '../errores';
import { api, cuenta, pathDe } from './comun';

const configuracionDe = (email: string, nombre: string): ConfiguracionNegocio =>
  estado.negocios.get(email) ?? { nombre, rubro: 'OTRO', ubicacion: null, comparteDatosZona: true };

export const handlersNegocio = [
  http.get(api('/business'), ({ request }) => {
    const { usuario, error } = cuenta(request);
    if (error) return error;
    return HttpResponse.json(configuracionDe(usuario.email, `Negocio de ${usuario.nombre}`));
  }),
  http.patch(api('/business'), async ({ request }) => {
    const { usuario, error } = cuenta(request, true);
    if (error) return error;
    const cambios = cambiosNegocioSchema.safeParse(await request.json());
    if (!cambios.success)
      return errorResponse(400, 'Revisá los datos del comercio.', pathDe(request));
    if (
      cambios.data.rubro &&
      !RUBROS_DE_PRUEBA.some((rubro) => rubro.codigo === cambios.data.rubro)
    )
      return errorResponse(400, 'El rubro no existe.', pathDe(request));
    const guardado = {
      ...configuracionDe(usuario.email, `Negocio de ${usuario.nombre}`),
      ...cambios.data,
    };
    estado.negocios.set(usuario.email, guardado);
    estado.alCambiar?.();
    return HttpResponse.json(guardado);
  }),
];
