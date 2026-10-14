import { useMutation, useQueryClient } from '@tanstack/react-query';
import { confirmarLectura, reintentarLectura, subirLote } from '@/api/lotes';
import { proyectosKeys } from '@/hooks/proyectos/queries';
import type { ConfirmarLectura } from '@/types/lectura';
import { lecturaKey, loteKey } from './queries';

// El lote recién creado ya trae sus páginas: se deja en la caché para no volver a pedirlo.
export function useSubirLote() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ fotos, bloqueDestinoId }: { fotos: Blob[]; bloqueDestinoId?: string }) =>
      subirLote(fotos, bloqueDestinoId),
    onSuccess: (lote) => queryClient.setQueryData(loteKey(lote.id), lote),
  });
}

export function useReintentarLectura(loteId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => reintentarLectura(loteId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: lecturaKey(loteId) }),
  });
}

// Guarda de una vez lo revisado. Al terminar, los proyectos cambian (uno nuevo o con una tabla más).
export function useConfirmarLectura(loteId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (datos: ConfirmarLectura) => confirmarLectura({ loteId, datos }),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: proyectosKeys.all }),
        queryClient.invalidateQueries({ queryKey: lecturaKey(loteId) }),
      ]);
    },
  });
}
