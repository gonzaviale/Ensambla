import { useQueries } from '@tanstack/react-query';
import { panelDelNegocioQuery } from '@/hooks/panel/queries';
import { DIAS_DEL_RESUMEN_CONJUNTO, resumenConjuntoDe } from '@/lib/negocios/lista';
import type { PanelDelNegocio } from '@/types/panel';
import type { NegocioDeLaCuenta } from '@/types/negocios';

/**
 * Lo vendido, gastado y pendiente de los negocios de la cuenta en los últimos 30 días. El servidor ya calcula el panel
 * de cada cuaderno: acá se pide uno por negocio y se suman.
 */
export function useResumenConjunto(negocios: NegocioDeLaCuenta[]) {
  const consultas = useQueries({
    queries: negocios.map((negocio) => panelDelNegocioQuery(negocio.id, DIAS_DEL_RESUMEN_CONJUNTO)),
  });
  const refetch = () => Promise.all(consultas.map((consulta) => consulta.refetch()));
  if (consultas.some((consulta) => consulta.isError)) {
    return { isPending: false, isError: true, data: undefined, refetch } as const;
  }
  const paneles = consultas.flatMap((consulta) => (consulta.data ? [consulta.data] : []));
  if (paneles.length < negocios.length) {
    return { isPending: true, isError: false, data: undefined, refetch } as const;
  }
  const data = resumenConjuntoDe(negocios, paneles satisfies PanelDelNegocio[]);
  return { isPending: false, isError: false, data, refetch } as const;
}
