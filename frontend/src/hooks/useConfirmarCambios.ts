import { useCallback, useEffect, useRef, useState } from 'react';

export function useConfirmarCambios() {
  const [confirmacion, setConfirmacion] = useState<string | null>(null);
  const resolver = useRef<((acepta: boolean) => void) | null>(null);
  useEffect(
    () => () => {
      resolver.current?.(false);
      resolver.current = null;
    },
    [],
  );
  const confirmar = useCallback((mensaje: string) => {
    if (resolver.current) return Promise.resolve(false);
    return new Promise<boolean>((resolve) => {
      resolver.current = resolve;
      setConfirmacion(mensaje);
    });
  }, []);
  const responder = (acepta: boolean) => {
    const pendiente = resolver.current;
    resolver.current = null;
    setConfirmacion(null);
    pendiente?.(acepta);
  };
  return {
    confirmar,
    confirmacion,
    aceptar: () => responder(true),
    conservar: () => responder(false),
  };
}
