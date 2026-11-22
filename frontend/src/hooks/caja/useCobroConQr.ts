import { useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { consultarCobroQr, generarCobroQr } from '@/api/caja';

export type EstadoDelCobroQr = 'cargando' | 'fallo' | 'listo';

interface Situacion {
  falloElCobro: boolean;
  cargando: boolean;
}

function estadoDe({ falloElCobro, cargando }: Situacion): EstadoDelCobroQr {
  if (falloElCobro) return 'fallo';
  if (cargando) return 'cargando';
  return 'listo';
}

/** Genera el cobro con QR del importe apenas se abre la pantalla; el id es estable, así repetir no duplica. */
export function useCobroConQr(importeCentavos: number) {
  const [id] = useState(() => crypto.randomUUID());
  const generado = useRef(false);
  // El ref sólo decide entre crear y consultar el mismo cobro idempotente; no identifica otra consulta.
  // eslint-disable-next-line @tanstack/query/exhaustive-deps
  const cobro = useQuery({
    queryKey: ['caja', 'cobro-qr', id, importeCentavos],
    queryFn: async () => {
      if (generado.current) return consultarCobroQr(id);
      const creado = await generarCobroQr({ id, importe: importeCentavos / 100 });
      generado.current = true;
      return creado;
    },
    retry: false,
    refetchInterval: (consulta) =>
      consulta.state.data?.estado === 'PENDIENTE' ? 1_000 : false,
    gcTime: 0,
    refetchOnWindowFocus: false,
  });
  const estado = estadoDe({
    falloElCobro: cobro.isError,
    cargando: cobro.isPending,
  });

  return { id, estado, cobro: cobro.data };
}
