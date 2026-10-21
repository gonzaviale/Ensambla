import {
  puntoSchema,
  type ConfiguracionUbicacion,
  type Direccion,
  type Rubro,
} from '@/types/ubicacion';

export function normalizarConsulta(consulta: string) {
  return consulta.trim().replace(/\s+/g, ' ');
}

export function errorDeConsulta(consulta: string) {
  const texto = normalizarConsulta(consulta);
  return texto.length < 5 || texto.length > 200
    ? 'Escribí calle, altura y localidad (entre 5 y 200 caracteres).'
    : '';
}

const sinTildes = (texto: string) =>
  texto
    .normalize('NFD')
    .replace(/\p{M}+/gu, '')
    .toLowerCase()
    .trim()
    .replace(/\s+/g, ' ');

// Misma regla que el backend: sin mayúsculas, tildes ni espacios de más. Si el mapa no
// reconoce ninguna localidad en el punto, no hay nada que contradecir.
export function coincideLocalidad(escrita: string, delMapa: string[]) {
  return delMapa.length === 0 || delMapa.some((nombre) => sinTildes(nombre) === sinTildes(escrita));
}

export function leerPunto(latitud: string, longitud: string) {
  if (!latitud.trim() || !longitud.trim()) return null;
  const parsed = puntoSchema.safeParse({
    latitud: Number(latitud.trim().replace(',', '.')),
    longitud: Number(longitud.trim().replace(',', '.')),
  });
  return parsed.success ? parsed.data : null;
}

export function revisarConfiguracion(
  direccion: Direccion | null,
  latitud: string,
  longitud: string,
  codigoRubro: string,
  rubros: Rubro[],
): { error: string; configuracion: ConfiguracionUbicacion | null } {
  if (!direccion)
    return { error: 'Buscá y elegí la dirección de tu comercio.', configuracion: null };
  const punto = leerPunto(latitud, longitud);
  if (!punto) {
    return {
      error: 'Revisá las coordenadas: latitud entre -90 y 90, longitud entre -180 y 180.',
      configuracion: null,
    };
  }
  const rubro = rubros.find((item) => item.codigo === codigoRubro);
  if (!rubro) return { error: 'Elegí el rubro de tu comercio.', configuracion: null };
  return {
    error: '',
    configuracion: {
      direccion: direccion.direccion,
      punto,
      rubro,
      ajustado: punto.latitud !== direccion.latitud || punto.longitud !== direccion.longitud,
    },
  };
}
