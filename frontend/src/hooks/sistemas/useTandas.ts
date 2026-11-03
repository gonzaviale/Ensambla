import { useState, type UIEvent } from 'react';
import { MARGEN_DEL_SCROLL } from '@/lib/sistema/tandas';

/**
 * Una tabla que se dibuja por tandas: arranca con `tanda` registros y suma otra cada vez que se pide o que el scroll
 * de la tabla llega al final. `total` es la cantidad de registros que coinciden (ya filtrados). Cuando cambia `clave`
 * (lo que se busca o se filtra) se vuelve a la primera tanda.
 */
export function useTandas(total: number, tanda: number, clave = '') {
  const [estado, setEstado] = useState({ clave, visibles: tanda });
  const visibles = estado.clave === clave ? estado.visibles : tanda;
  const quedan = Math.max(0, total - visibles);

  const sumar = () => setEstado({ clave, visibles: visibles + tanda });
  /** Se asegura de que el registro en esa posición (desde 0) esté dibujado, por ejemplo para llevar la vista a él. */
  const mostrarHasta = (indice: number) =>
    setEstado({ clave, visibles: Math.max(visibles, indice + 1) });
  const alDesplazar = (evento: UIEvent<HTMLElement>) => {
    const { scrollTop, clientHeight, scrollHeight } = evento.currentTarget;
    if (quedan > 0 && scrollTop + clientHeight >= scrollHeight - MARGEN_DEL_SCROLL) sumar();
  };

  return { visibles, quedan, sumar, mostrarHasta, alDesplazar };
}
