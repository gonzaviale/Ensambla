import { http, HttpResponse } from 'msw';
import { coincideLocalidad } from '@/lib/ubicacion';
import { cambiosNegocioSchema, type ConfiguracionNegocio } from '@/types/negocio';
import { claveDelNegocio, estado, type UsuarioMock } from '../db/estado';
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

// La del negocio activo. Uno que todavía no se configuró arranca con el nombre y el rubro con que se creó.
const configuracionDe = (usuario: UsuarioMock): ConfiguracionNegocio => {
  const guardada = estado.negocios.get(claveDelNegocio(usuario.email));
  if (guardada) return guardada;
  const cuenta = estado.negociosDeLaCuenta.get(usuario.email);
  const activo = cuenta?.negocios.find((negocio) => negocio.id === cuenta.activoId);
  return {
    nombre: activo?.nombre ?? `Negocio de ${usuario.nombre}`,
    rubro: activo?.rubro ?? 'OTRO',
    ubicacion: null,
    comparteDatosZona: true,
    zona: null,
  };
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
    return HttpResponse.json(configuracionDe(usuario));
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
    const actual = configuracionDe(usuario);
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
    estado.negocios.set(claveDelNegocio(usuario.email), guardado);
    estado.alCambiar?.();
    return HttpResponse.json(guardado);
  }),
];
