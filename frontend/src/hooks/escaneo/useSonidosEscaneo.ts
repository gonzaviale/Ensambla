import { useSyncExternalStore } from 'react';
import {
  cambiarSonidosEscaneo,
  leerSonidosEscaneo,
  prepararSonidosEscaneo,
  probarSonidosEscaneo,
  sonarEscaneo,
  suscribirSonidosEscaneo,
  requiereActivacionSonidosEscaneo,
} from '@/api/sonidosEscaneo';

export function useSonidosEscaneo() {
  const habilitado = useSyncExternalStore(suscribirSonidosEscaneo, leerSonidosEscaneo, () => true);
  const requiereActivacion = useSyncExternalStore(
    suscribirSonidosEscaneo,
    requiereActivacionSonidosEscaneo,
    () => false,
  );
  return {
    habilitado,
    requiereActivacion,
    preparar: prepararSonidosEscaneo,
    sonar: sonarEscaneo,
    probar: probarSonidosEscaneo,
    alternar: () => {
      cambiarSonidosEscaneo(!habilitado);
      if (!habilitado) prepararSonidosEscaneo();
    },
  };
}
