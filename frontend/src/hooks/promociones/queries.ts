import { queryOptions, useQuery } from '@tanstack/react-query';
import { consultarPromociones } from '@/api/promociones';

export const promocionesKeys = {
  de: (sistemaId: string) => ['promociones', sistemaId] as const,
};

export const promocionesQuery = (sistemaId: string) =>
  queryOptions({
    queryKey: promocionesKeys.de(sistemaId),
    queryFn: ({ signal }) => consultarPromociones(sistemaId, signal),
    // Lo que se vende cambia todos los días: al volver a la pantalla se vuelve a mirar.
    staleTime: 0,
    retry: false,
  });

/** Las ideas para promocionar del sistema y los canales para compartirlas. */
export function usePromociones(sistemaId: string) {
  return useQuery(promocionesQuery(sistemaId));
}
