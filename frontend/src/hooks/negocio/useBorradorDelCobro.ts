import { useState } from 'react';
import type { CambiosNegocio, ConfiguracionNegocio } from '@/types/negocio';

/** Lo que el comerciante escribe en el cobro con QR, antes de guardarlo. */
export function useBorradorDelCobro(negocio: ConfiguracionNegocio) {
  const aliasGuardado = negocio.cobroAlias ?? '';
  const titularGuardado = negocio.cobroTitular ?? '';
  const [alias, setAlias] = useState(aliasGuardado);
  const [titular, setTitular] = useState(titularGuardado);
  const aliasLimpio = alias.trim();
  const cambios: CambiosNegocio = {
    cobroAlias: aliasLimpio,
    cobroTitular: aliasLimpio ? titular.trim() : '',
  };
  const hayCambios =
    cambios.cobroAlias !== aliasGuardado || cambios.cobroTitular !== titularGuardado;

  return { alias, setAlias, titular, setTitular, cambios, hayCambios };
}
