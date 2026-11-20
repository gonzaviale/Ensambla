import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { generarCobroQr } from '@/api/caja';
import { useConfiguracionNegocio } from '@/hooks/negocio/queries';

export type EstadoDelCobroQr = 'cargando' | 'sinAlias' | 'fallo' | 'listo';

interface Situacion {
  falloElNegocio: boolean;
  falloElCobro: boolean;
  cargando: boolean;
  hayAlias: boolean;
}

function estadoDe({
  falloElNegocio,
  falloElCobro,
  cargando,
  hayAlias,
}: Situacion): EstadoDelCobroQr {
  if (falloElNegocio || falloElCobro) return 'fallo';
  if (cargando) return 'cargando';
  if (!hayAlias) return 'sinAlias';
  return 'listo';
}

/** Genera el cobro con QR del importe apenas se abre la pantalla; el id es estable, así repetir no duplica. */
export function useCobroConQr(importeCentavos: number) {
  const negocio = useConfiguracionNegocio();
  const [id] = useState(() => crypto.randomUUID());
  const hayAlias = !!negocio.data?.cobroAlias;
  const cobro = useQuery({
    queryKey: ['caja', 'cobro-qr', id, importeCentavos],
    queryFn: () => generarCobroQr({ id, importe: importeCentavos / 100 }),
    enabled: hayAlias,
    retry: false,
    staleTime: Infinity,
    gcTime: 0,
    refetchOnWindowFocus: false,
  });
  const estado = estadoDe({
    falloElNegocio: negocio.isError,
    falloElCobro: cobro.isError,
    cargando: negocio.isPending || (hayAlias && cobro.isPending),
    hayAlias,
  });

  return { id, estado, cobro: cobro.data };
}
