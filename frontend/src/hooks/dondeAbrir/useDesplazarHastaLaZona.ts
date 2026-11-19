import { useEffect, useRef } from 'react';

function llevarAlAreaVisible(contenedor: HTMLElement, elemento: HTMLElement) {
  const cajaContenedor = contenedor.getBoundingClientRect();
  const cajaElemento = elemento.getBoundingClientRect();
  const arriba = cajaElemento.top - cajaContenedor.top + contenedor.scrollTop;
  const abajo = cajaElemento.bottom - cajaContenedor.top + contenedor.scrollTop;
  if (arriba < contenedor.scrollTop) {
    contenedor.scrollTop = arriba;
    return;
  }
  if (abajo > contenedor.scrollTop + contenedor.clientHeight) {
    contenedor.scrollTop = abajo - contenedor.clientHeight;
  }
}

/**
 * Si la zona se elige en el mapa, se desplaza únicamente la lista hasta mostrarla.
 * Nunca se mueve la página completa, y al abrir la pantalla no se mueve nada.
 */
export function useDesplazarHastaLaZona(seleccionadaId: string) {
  const lista = useRef<HTMLDivElement>(null);
  const primera = useRef(true);
  useEffect(() => {
    if (primera.current) {
      primera.current = false;
      return;
    }
    const elemento = document.getElementById(`zona-${seleccionadaId}`);
    if (lista.current && elemento) llevarAlAreaVisible(lista.current, elemento);
  }, [seleccionadaId]);
  return lista;
}
