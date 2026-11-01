// Fixtures explícitas del modo completo y de las pruebas. El modo parcial usa el geocodificador real.
export const RUBROS_DE_PRUEBA = [
  { codigo: 'ALMACEN', nombre: 'Almacén' },
  { codigo: 'KIOSCO', nombre: 'Kiosco' },
  { codigo: 'MINIMERCADO', nombre: 'Minimercado' },
  { codigo: 'DIETETICA', nombre: 'Dietética' },
  { codigo: 'OTRO', nombre: 'Otro' },
];
// Misma zona que carga la migración V10 del backend.
export const ZONA_DE_PRUEBA = {
  nombre: 'San Justo - UNLaM',
  latitud: -34.6703,
  longitud: -58.5628,
  radioMetros: 1500,
};
export const LOCALIDAD_DE_PRUEBA = 'San Justo';
export const LOCALIDAD_FUERA_DE_LA_ZONA = 'Otra localidad';
export const DIRECCIONES_DE_PRUEBA = [
  {
    direccion: 'Florencio Varela 1903, San Justo, Buenos Aires, Argentina',
    latitud: -34.6701,
    longitud: -58.5625,
    localidad: LOCALIDAD_DE_PRUEBA,
  },
  {
    direccion: 'Florencio Varela, San Justo, Buenos Aires, Argentina',
    latitud: -34.671,
    longitud: -58.564,
    localidad: LOCALIDAD_DE_PRUEBA,
  },
] as const;

// Direcciones de otras zonas, para probar el análisis de zona: una con pocos comercios y una sin datos.
export const DIRECCION_EN_ZONA_CHICA_DE_PRUEBA = {
  direccion: 'Av. de Mayo 500, Ramos Mejía, Buenos Aires, Argentina',
  latitud: -34.6419,
  longitud: -58.5666,
  localidad: 'Ramos Mejía',
} as const;
export const DIRECCION_SIN_ZONA_DE_PRUEBA = {
  direccion: 'Honduras 5000, Palermo, Ciudad Autónoma de Buenos Aires, Argentina',
  latitud: -34.5889,
  longitud: -58.4306,
  localidad: 'Palermo',
} as const;

const METROS_POR_GRADO = 111_195;

type PuntoPlano = { latitud: number; longitud: number };

// Aproximación plana: alcanza para decidir si un punto cae en el radio de una zona de prueba.
export function distanciaEnMetros(desde: PuntoPlano, hasta: PuntoPlano) {
  const norte = (hasta.latitud - desde.latitud) * METROS_POR_GRADO;
  const este =
    (hasta.longitud - desde.longitud) *
    METROS_POR_GRADO *
    Math.cos((desde.latitud * Math.PI) / 180);
  return Math.hypot(norte, este);
}

export function dentroDeLaZonaDePrueba(punto: PuntoPlano) {
  return distanciaEnMetros(ZONA_DE_PRUEBA, punto) <= ZONA_DE_PRUEBA.radioMetros;
}

// Lo que el mapa de prueba reconoce en un punto: San Justo dentro de la zona, otra localidad fuera.
export const localidadDelPuntoDePrueba = (punto: { latitud: number; longitud: number }) =>
  dentroDeLaZonaDePrueba(punto) ? LOCALIDAD_DE_PRUEBA : LOCALIDAD_FUERA_DE_LA_ZONA;
