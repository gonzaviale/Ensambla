import { http, HttpResponse } from 'msw';
import { coincideLocalidad } from '@/lib/ubicacion';
import { cambiosNegocioSchema, type ConfiguracionNegocio } from '@/types/negocio';
import { estado } from '../db/estado';
import {
  dentroDeLaZonaDePrueba,
  localidadDelPuntoDePrueba,
  RUBROS_DE_PRUEBA,
  ZONA_DE_PRUEBA,
} from '../db/ubicacion';
import { errorResponse } from '../errores';
import { api, cuenta, pathDe } from './comun';

const zonaQueCubre = (ubicacion: ConfiguracionNegocio['ubicacion']): string | null =>
  ubicacion && dentroDeLaZonaDePrueba(ubicacion) ? ZONA_DE_PRUEBA.nombre : null;

const configuracionDe = (email: string, nombre: string): ConfiguracionNegocio =>
  estado.negocios.get(email) ?? {
    nombre,
    rubro: 'OTRO',
    ubicacion: null,
    comparteDatosZona: true,
    zona: null,
  };

// Igual que el backend: sólo se verifica la localidad si cambia el punto o la localidad guardados.
const localidadFueraDelMapa = (
  guardada: ConfiguracionNegocio['ubicacion'],
  nueva: { latitud: number; longitud: number; localidad: string },
) => {
  const cambia =
    guardada?.latitud !== nueva.latitud ||
    guardada?.longitud !== nueva.longitud ||
    guardada?.localidad !== nueva.localidad;
  const delMapa = localidadDelPuntoDePrueba(nueva);
  return cambia && !coincideLocalidad(nueva.localidad, [delMapa]) ? delMapa : null;
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
    const actual = configuracionDe(usuario.email, `Negocio de ${usuario.nombre}`);
    const nueva = cambios.data.ubicacion;
    const delMapa = nueva && localidadFueraDelMapa(actual.ubicacion, nueva);
    if (delMapa)
      return errorResponse(
        400,
        `La localidad no coincide con la del mapa (${delMapa})`,
        pathDe(request),
      );
    const guardado = { ...actual, ...cambios.data };
    guardado.zona = zonaQueCubre(guardado.ubicacion);
    estado.negocios.set(usuario.email, guardado);
    estado.alCambiar?.();
    return HttpResponse.json(guardado);
  }),
];
