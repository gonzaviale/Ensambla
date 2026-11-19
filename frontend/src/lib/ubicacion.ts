import { cambiosNegocioSchema, type CambiosNegocio } from '@/types/negocio';
import {
  puntoSchema,
  type ConfiguracionUbicacion,
  type Direccion,
  type PorSeccion,
  type Punto,
  type Rubro,
  type SeccionUbicacion,
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

type Revision = { error: string; configuracion: ConfiguracionUbicacion | null };

const rechazada = (error: string): Revision => ({ error, configuracion: null });

const aceptada = (configuracion: ConfiguracionUbicacion): Revision => ({
  error: '',
  configuracion,
});

const seMovioElPunto = (punto: Punto, direccion: Direccion) =>
  punto.latitud !== direccion.latitud || punto.longitud !== direccion.longitud;

export function revisarConfiguracion(
  direccion: Direccion | null,
  latitud: string,
  longitud: string,
  codigoRubro: string,
  rubros: Rubro[],
): Revision {
  if (!direccion) return rechazada('Buscá y elegí la dirección de tu comercio.');
  const punto = leerPunto(latitud, longitud);
  if (!punto) {
    return rechazada('Revisá las coordenadas: latitud entre -90 y 90, longitud entre -180 y 180.');
  }
  const rubro = rubros.find((item) => item.codigo === codigoRubro);
  if (!rubro) return rechazada('Elegí el rubro de tu comercio.');
  const ajustado = seMovioElPunto(punto, direccion);
  return aceptada({ direccion: direccion.direccion, punto, rubro, ajustado });
}

/** Lo que la persona tiene cargado en pantalla, antes de guardar. */
export type BorradorDeUbicacion = {
  nombre: string;
  comparteDatosZona: boolean;
  rubro: string;
  rubrosDisponibles: Rubro[];
  direccion: Direccion | null;
  punto: Punto | null;
  localidad: string;
  localidadBloquea: boolean;
  errorDeLocalidad: string;
};

/** El PATCH de una solapa con solo sus campos, o la explicación de qué le falta. */
export type CambiosDeSolapa = { cambios?: CambiosNegocio; error?: string };

function cambiosDelComercio({ nombre, comparteDatosZona }: BorradorDeUbicacion): CambiosDeSolapa {
  const datos = cambiosNegocioSchema.safeParse({ nombre, comparteDatosZona });
  return datos.success
    ? { cambios: datos.data }
    : { error: 'Escribí el nombre del comercio (hasta 120 caracteres).' };
}

function cambiosDelRubro({ rubro, rubrosDisponibles }: BorradorDeUbicacion): CambiosDeSolapa {
  return rubrosDisponibles.some((item) => item.codigo === rubro)
    ? { cambios: { rubro } }
    : { error: 'Elegí el rubro de tu comercio.' };
}

function cambiosDeLaUbicacion(borrador: BorradorDeUbicacion): CambiosDeSolapa {
  const { direccion, punto, localidad } = borrador;
  if (!direccion || !punto) return { error: 'Buscá y elegí la dirección de tu comercio.' };
  if (borrador.localidadBloquea) {
    const esperar = 'Esperá a que verifiquemos la localidad con el mapa para guardar.';
    return { error: borrador.errorDeLocalidad || esperar };
  }
  const datos = cambiosNegocioSchema.safeParse({
    ubicacion: { direccion: direccion.direccion, localidad, ...punto },
  });
  return datos.success
    ? { cambios: datos.data }
    : { error: 'No pudimos saber la localidad. Buscá la dirección con la localidad al final.' };
}

const CAMBIOS_POR_SOLAPA: PorSeccion<(borrador: BorradorDeUbicacion) => CambiosDeSolapa> = {
  comercio: cambiosDelComercio,
  rubro: cambiosDelRubro,
  ubicacion: cambiosDeLaUbicacion,
};

export const cambiosDeLaSolapa = (
  seccion: SeccionUbicacion,
  borrador: BorradorDeUbicacion,
): CambiosDeSolapa => CAMBIOS_POR_SOLAPA[seccion](borrador);

// Lo que sigue a la última coma de la dirección escrita: "Varela 1903, San Justo" → "San Justo".
export function localidadEscrita(consulta: string) {
  const partes = consulta.split(',');
  return partes.length > 1 ? (partes[partes.length - 1]?.trim() ?? '') : '';
}
