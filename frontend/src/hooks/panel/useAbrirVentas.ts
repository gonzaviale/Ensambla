import { useAccionesDeAvisos } from '@/hooks/avisos/useAccionesDeAvisos';
import { useNavegacion } from '@/hooks/sistemas/useNavegacion';
import { bloqueDeVentas } from '@/lib/sistema/panelDelNegocio';
import type { Sistema } from '@/lib/sistema/tipos';

/** Lleva a la tabla de ventas del negocio y la marca; si todavía no tiene una, abre el negocio. */
export function useAbrirVentas() {
  const navegacion = useNavegacion();
  const acciones = useAccionesDeAvisos();
  return (sistema: Sistema) => {
    const ventas = bloqueDeVentas(sistema);
    return ventas
      ? acciones.irAlObjeto({ sistemaId: sistema.id, bloqueId: ventas.id })
      : navegacion.abrirSistema(sistema.id, 'use');
  };
}
