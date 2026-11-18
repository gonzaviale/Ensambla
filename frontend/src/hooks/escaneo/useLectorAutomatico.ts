import { useEffect, useRef } from 'react';

type Camara = { abrir: () => void; cerrar: () => void; abierta: boolean; iniciando: boolean };

/** Abre una vez por entrada al lector; un permiso rechazado requiere un reintento explícito. */
export function useLectorAutomatico(habilitado: boolean, camara: Camara) {
  const actual = useRef(camara);
  useEffect(() => {
    actual.current = camara;
  });
  useEffect(() => {
    const actualizar = () => {
      if (!habilitado || document.visibilityState === 'hidden') actual.current.cerrar();
      else if (!actual.current.abierta) actual.current.abrir();
    };
    actualizar();
    document.addEventListener('visibilitychange', actualizar);
    return () => {
      document.removeEventListener('visibilitychange', actualizar);
      if (habilitado) actual.current.cerrar();
    };
  }, [habilitado]);
}
