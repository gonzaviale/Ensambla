import { useMemo, useState } from 'react';
import { sistemasConDatos } from '@/lib/sistema/metricasInicio';
import { useSistemaStore } from '@/stores/sistemaStore';

const CLAVE_DEL_SISTEMA = 'ensambla.inicio.sistema';

const leerSistemaElegido = () => {
  try {
    return window.localStorage.getItem(CLAVE_DEL_SISTEMA);
  } catch {
    return null;
  }
};

/**
 * Las secciones que tienen números para mostrar y cuál se está mirando. Arranca con la última elegida en el inicio o,
 * si no hay, con la última que se tocó.
 */
export function useSistemaElegido() {
  const todos = useSistemaStore((estado) => estado.sistemas);
  const sistemas = useMemo(() => sistemasConDatos(todos), [todos]);
  const [elegido, setElegido] = useState(leerSistemaElegido);
  const sistema = sistemas.find((item) => item.id === elegido) ?? sistemas[0];
  const elegir = (id: string) => {
    setElegido(id);
    try {
      window.localStorage.setItem(CLAVE_DEL_SISTEMA, id);
    } catch {
      // Sin almacenamiento funciona igual: sólo no se acuerda de la elección.
    }
  };
  return { sistemas, sistema, elegir };
}
