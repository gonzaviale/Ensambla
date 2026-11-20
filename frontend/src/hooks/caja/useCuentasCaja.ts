import { useQuery } from '@tanstack/react-query';
import { listarCuentasCaja } from '@/api/caja';
import { useSistemaStore } from '@/stores/sistemaStore';
import { idCaja } from '@/lib/caja';

export function useCuentasCaja(sistemaId: string, inventarioId: string) {
  const usuarioId = useSistemaStore((s) => s.usuarioId);
  return useQuery({
    queryKey: ['caja-cuentas', usuarioId, sistemaId, inventarioId],
    queryFn: () => listarCuentasCaja(idCaja(sistemaId), idCaja(inventarioId)),
    staleTime: 0,
    refetchInterval: 30_000,
  });
}
