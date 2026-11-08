import { AVISO_ECONOMICO_DE_EJEMPLO } from '@/lib/sistema/avisoEconomicoDeEjemplo';
import type { AvisoEconomico } from '@/types/avisoEconomico';

/**
 * El aviso económico del cuaderno para el inicio. Por ahora es siempre el ejemplo (`esEjemplo: true`): cuando el
 * backend lo calcule, acá se cambia por la consulta al servidor y el resto del inicio queda igual.
 */
export function useAvisoEconomico(sistemaId: string): {
  aviso: AvisoEconomico | undefined;
  esEjemplo: boolean;
} {
  void sistemaId;
  return { aviso: AVISO_ECONOMICO_DE_EJEMPLO, esEjemplo: true };
}
