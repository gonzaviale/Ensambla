import { useMemo } from 'react';
import {
  buscarNegocio,
  cuentasCorrientes,
  datosDelNegocio,
  resultadoDelMes,
} from '@/lib/gestion/resultado';
import { hoy as fechaDeHoy } from '@/lib/sistema/formato';
import { useSistemaStore } from '@/stores/sistemaStore';

/**
 * Los números de «Mi negocio» sacados del cuaderno con inventario y ventas: el resultado del último mes y lo
 * que debe cada cliente. `null` si la persona todavía no tiene un cuaderno con esos datos.
 */
export function useNegocio() {
  const sistemas = useSistemaStore((estado) => estado.sistemas);
  return useMemo(() => {
    const sistema = buscarNegocio(sistemas);
    if (!sistema) return null;
    const hoy = fechaDeHoy();
    const datos = datosDelNegocio(sistema);
    return {
      sistema,
      hoy,
      datos,
      resultado: resultadoDelMes(datos, hoy),
      fiado: cuentasCorrientes(datos, hoy),
    };
  }, [sistemas]);
}

export type Negocio = NonNullable<ReturnType<typeof useNegocio>>;
