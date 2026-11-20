import { useSistemaStore } from '@/stores/sistemaStore';
import type { CambiosNegocio } from '@/types/negocio';
import { useActualizarNegocio } from './mutations';

export function useGuardarCobroConQr() {
  const actualizar = useActualizarNegocio();
  const avisar = useSistemaStore((estado) => estado.avisar);

  const guardar = (cambios: CambiosNegocio) =>
    actualizar.mutate(cambios, { onSuccess: () => avisar('Guardamos tu cobro con QR.') });

  return { guardar, guardando: actualizar.isPending, fallo: actualizar.isError };
}
