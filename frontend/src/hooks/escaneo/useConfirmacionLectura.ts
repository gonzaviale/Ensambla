import { useEffect, useState } from 'react';
import type { ResultadoSonoroEscaneo } from '@/lib/escaneo/sonidos';

export type ConfirmacionLectura = { id: number; tipo: ResultadoSonoroEscaneo };

export function useConfirmacionLectura(lectura: ConfirmacionLectura | null) {
  const [visible, setVisible] = useState<ConfirmacionLectura | null>(null);
  useEffect(() => {
    if (!lectura) return;
    setVisible(lectura);
    const temporizador = setTimeout(() => setVisible(null), 1400);
    return () => clearTimeout(temporizador);
  }, [lectura]);
  return visible;
}
