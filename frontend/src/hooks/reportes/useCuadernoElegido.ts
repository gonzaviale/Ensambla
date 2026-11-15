import { useEffect, useMemo, useState } from 'react';
import { cuadernosConDatos } from '@/lib/sistema/metricasInicio';
import { useSistemaStore } from '@/stores/sistemaStore';

const CLAVE_DEL_CUADERNO = 'ensambla.inicio.cuaderno';
/** Avisa a los demás que miran el mismo selector (el carrusel de arriba y el tablero) que cambió el negocio. */
const EVENTO_DE_CAMBIO = 'ensambla:cuaderno-elegido';

const leerCuadernoElegido = () => {
  try {
    return window.localStorage.getItem(CLAVE_DEL_CUADERNO);
  } catch {
    return null;
  }
};

/**
 * Los cuadernos que tienen números para mostrar y cuál se está mirando. Arranca con el último elegido en el inicio o,
 * si no hay, con el último que se tocó.
 */
export function useCuadernoElegido() {
  const sistemas = useSistemaStore((estado) => estado.sistemas);
  const cuadernos = useMemo(() => cuadernosConDatos(sistemas), [sistemas]);
  const [elegido, setElegido] = useState(leerCuadernoElegido);
  useEffect(() => {
    const alCambiar = () => setElegido(leerCuadernoElegido());
    window.addEventListener(EVENTO_DE_CAMBIO, alCambiar);
    return () => window.removeEventListener(EVENTO_DE_CAMBIO, alCambiar);
  }, []);
  const cuaderno = cuadernos.find((item) => item.id === elegido) ?? cuadernos[0];
  const elegir = (id: string) => {
    setElegido(id);
    try {
      window.localStorage.setItem(CLAVE_DEL_CUADERNO, id);
    } catch {
      // Sin almacenamiento funciona igual: sólo no se acuerda de la elección.
    }
    window.dispatchEvent(new Event(EVENTO_DE_CAMBIO));
  };
  return { cuadernos, cuaderno, elegir };
}
