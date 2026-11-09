import { useQuery } from '@tanstack/react-query';
import { avisoEconomicoQuery } from '@/hooks/reportes/queries';
import type { AvisoEconomico } from '@/types/avisoEconomico';

/**
 * El aviso económico del cuaderno para el inicio: esta semana frente a la anterior. Mientras carga, si falla o si
 * todavía no hay una semana anterior para comparar, no hay aviso y el inicio no muestra la tarjeta.
 */
export function useAvisoEconomico(sistemaId: string): { aviso: AvisoEconomico | undefined } {
  const consulta = useQuery(avisoEconomicoQuery(sistemaId));
  return { aviso: consulta.data?.aviso ?? undefined };
}
