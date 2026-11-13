import { useEffect, useRef } from 'react';
import type { ResultadoCatalogos } from '@/types/escaneo';

export function useDetalleCatalogos(resultado: ResultadoCatalogos | null) {
  const dialogo = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    dialogo.current?.close();
  }, [resultado]);
  return {
    dialogo,
    abrir: () => dialogo.current?.showModal(),
    cerrar: () => dialogo.current?.close(),
  };
}
