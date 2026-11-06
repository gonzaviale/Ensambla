import { useMemo } from 'react';
import { PEDIDOS_DE_HOY } from '@/lib/gestion/datosDeEjemplo';
import { conLoDeHoy } from '@/lib/gestion/elDia';
import {
  buscarNegocio,
  cuentasCorrientes,
  datosDelNegocio,
  resultadoDelMes,
} from '@/lib/gestion/resultado';
import { hoy as fechaDeHoy } from '@/lib/sistema/formato';
import { useNegocioStore } from '@/stores/negocioStore';
import { useSistemaStore } from '@/stores/sistemaStore';

/**
 * Los números de «Mi negocio» sacados del cuaderno con inventario y ventas, con lo que pasó hoy encima (pedidos
 * confirmados, remitos recibidos, precios nuevos): el resultado del último mes, lo que debe cada cliente y los
 * pedidos de hoy. `null` si la persona todavía no tiene un cuaderno con esos datos.
 */
export function useNegocio() {
  const sistemas = useSistemaStore((estado) => estado.sistemas);
  const confirmados = useNegocioStore((estado) => estado.confirmados);
  const ordenes = useNegocioStore((estado) => estado.ordenesEnviadas);
  const recepcion = useNegocioStore((estado) => estado.recepcion);
  const precios = useNegocioStore((estado) => estado.precios);
  const ofertas = useNegocioStore((estado) => estado.ofertas);

  return useMemo(() => {
    const sistema = buscarNegocio(sistemas);
    if (!sistema) return null;
    const hoy = fechaDeHoy();
    const datos = conLoDeHoy(datosDelNegocio(sistema), hoy, {
      confirmados: Object.values(confirmados),
      recibidos: ordenes.filter((orden) => recepcion[orden.proveedor.id] === 'recibido'),
      precios,
    });
    return {
      sistema,
      hoy,
      datos,
      resultado: resultadoDelMes(datos, hoy),
      fiado: cuentasCorrientes(datos, hoy),
      /** Los mensajes de hoy: los de ejemplo y las respuestas a las ofertas enviadas. */
      pedidos: [...PEDIDOS_DE_HOY, ...Object.values(ofertas).flatMap((enviada) => enviada.pedidos)],
    };
  }, [sistemas, confirmados, ordenes, recepcion, precios, ofertas]);
}

export type Negocio = NonNullable<ReturnType<typeof useNegocio>>;
