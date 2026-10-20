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
export const DIRECCIONES_DE_PRUEBA = [
  {
    direccion: 'Florencio Varela 1903, San Justo, Buenos Aires, Argentina',
    latitud: -34.6701,
    longitud: -58.5625,
  },
  {
    direccion: 'Florencio Varela, San Justo, Buenos Aires, Argentina',
    latitud: -34.671,
    longitud: -58.564,
  },
] as const;
