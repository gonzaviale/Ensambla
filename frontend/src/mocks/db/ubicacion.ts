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

const METROS_POR_GRADO = 111_195;

// Aproximación plana: alcanza para decidir si un punto cae en el radio de la zona de prueba.
export function dentroDeLaZonaDePrueba(punto: { latitud: number; longitud: number }) {
  const norte = (punto.latitud - ZONA_DE_PRUEBA.latitud) * METROS_POR_GRADO;
  const este =
    (punto.longitud - ZONA_DE_PRUEBA.longitud) *
    METROS_POR_GRADO *
    Math.cos((ZONA_DE_PRUEBA.latitud * Math.PI) / 180);
  return Math.hypot(norte, este) <= ZONA_DE_PRUEBA.radioMetros;
}

// Lo que el mapa de prueba reconoce en un punto: San Justo dentro de la zona, otra localidad fuera.
export const localidadDelPuntoDePrueba = (punto: { latitud: number; longitud: number }) =>
  dentroDeLaZonaDePrueba(punto) ? LOCALIDAD_DE_PRUEBA : LOCALIDAD_FUERA_DE_LA_ZONA;
