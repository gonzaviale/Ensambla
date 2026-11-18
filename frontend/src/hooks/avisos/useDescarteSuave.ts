import { useState } from 'react';

/** Lo que dura el desvanecido de una fila descartada, en milisegundos (el mismo número va en el CSS). */
const DURACION_DEL_DESVANECIDO = 280;

const personaPrefiereSinAnimacion = () =>
  typeof window.matchMedia !== 'function' ||
  window.matchMedia('(prefers-reduced-motion: reduce)').matches;

/** Descartar con un desvanecido: la fila se apaga despacio y recién después se quita. Sin animación si la persona la desactivó. */
export function useDescarteSuave(alDescartar: () => void) {
  const [saliendo, setSaliendo] = useState(false);

  const descartar = () => {
    if (personaPrefiereSinAnimacion()) return alDescartar();
    setSaliendo(true);
    setTimeout(alDescartar, DURACION_DEL_DESVANECIDO);
  };

  return { saliendo, descartar };
}
