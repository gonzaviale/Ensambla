import { queryOptions, useQuery } from '@tanstack/react-query';
import { DIAS_DEL_PANEL, obtenerPanelDelNegocio } from '@/api/panel';

export const panelKeys = {
  all: ['panel'] as const,
  de: (sistemaId: string, dias: number) => ['panel', sistemaId, dias] as const,
};

export const panelDelNegocioQuery = (sistemaId: string, dias = DIAS_DEL_PANEL) =>
  queryOptions({
    queryKey: panelKeys.de(sistemaId, dias),
    queryFn: ({ signal }) => obtenerPanelDelNegocio(sistemaId, dias, signal),
    // Las ventas se anotan en otra pantalla: al volver a la home se vuelve a preguntar.
    staleTime: 0,
    retry: false,
  });

/** El panel de la home de un sistema; sin `sistemaId` (todavía no tiene secciones) no pregunta nada. */
export function usePanelDelNegocio(sistemaId: string | null) {
  return useQuery({
    ...panelDelNegocioQuery(sistemaId ?? '', DIAS_DEL_PANEL),
    enabled: sistemaId !== null,
  });
}
