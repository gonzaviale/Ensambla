import { http, HttpResponse } from 'msw';
import { cambiosNegocioSchema, type ConfiguracionNegocio } from '@/types/negocio';
import { estado } from '../db/estado';
import { RUBROS_DE_PRUEBA, ZONA_DE_PRUEBA } from '../db/ubicacion';
import { errorResponse } from '../errores';
import { api, cuenta, pathDe } from './comun';

const METROS_POR_GRADO = 111_195;

// Aproximación plana: alcanza para decidir si un punto cae en el radio de la zona de prueba.
const zonaQueCubre = (ubicacion: ConfiguracionNegocio['ubicacion']): string | null => {
  if (!ubicacion) return null;
  const norte = (ubicacion.latitud - ZONA_DE_PRUEBA.latitud) * METROS_POR_GRADO;
  const este =
    (ubicacion.longitud - ZONA_DE_PRUEBA.longitud) *
    METROS_POR_GRADO *
    Math.cos((ZONA_DE_PRUEBA.latitud * Math.PI) / 180);
  return Math.hypot(norte, este) <= ZONA_DE_PRUEBA.radioMetros ? ZONA_DE_PRUEBA.nombre : null;
};

const configuracionDe = (email: string, nombre: string): ConfiguracionNegocio =>
  estado.negocios.get(email) ?? {
    nombre,
    rubro: 'OTRO',
    ubicacion: null,
    comparteDatosZona: true,
    zona: null,
  };

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
    guardado.zona = zonaQueCubre(guardado.ubicacion);
    estado.negocios.set(usuario.email, guardado);
    estado.alCambiar?.();
    return HttpResponse.json(guardado);
  }),
];
