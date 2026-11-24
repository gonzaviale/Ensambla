export type EstadoDeLaPrueba = {
  activando: boolean;
  puedeIniciar: boolean;
  pruebaActiva: boolean;
  actual: boolean;
  pruebaUtilizada: boolean;
};

export function textoDelBotonDePrueba(estado: EstadoDeLaPrueba) {
  if (estado.activando) return 'Activando tu prueba…';
  if (estado.puedeIniciar) return 'Probar Premium por 7 días';
  if (estado.pruebaActiva) return 'Prueba activa';
  if (estado.actual) return 'Premium activo';
  if (estado.pruebaUtilizada) return 'Prueba ya utilizada';
  return 'Prueba no disponible';
}
