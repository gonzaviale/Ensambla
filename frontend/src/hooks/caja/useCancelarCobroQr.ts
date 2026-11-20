import { useMutation } from '@tanstack/react-query';
import { cancelarCobroQr } from '@/api/caja';

export function useCancelarCobroQr() {
  const cancelacion = useMutation({ mutationFn: cancelarCobroQr });
  return { cancelar: (id: string) => cancelacion.mutate(id) };
}
