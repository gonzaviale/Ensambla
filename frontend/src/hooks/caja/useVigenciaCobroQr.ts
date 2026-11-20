import { useEffect, useState } from 'react';

const ESPERA_MAXIMA = 2_147_483_647;

export function useVigenciaCobroQr(venceEn: string): boolean {
  const [ahora, setAhora] = useState(Date.now);
  const vencimiento = Date.parse(venceEn);

  useEffect(() => {
    const restante = vencimiento - Date.now();
    if (restante <= 0) return;
    const reloj = window.setTimeout(() => setAhora(Date.now()), Math.min(restante, ESPERA_MAXIMA));
    return () => window.clearTimeout(reloj);
  }, [vencimiento, ahora]);

  return ahora >= vencimiento;
}
