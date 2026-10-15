import type { RedState } from '@/types/red';
import { diasEntre } from './calculos';

// «El valor que aporta»: lo que Ensambla le ahorró al comercio, calculado con sus propios datos.
// Las estimaciones de tiempo son supuestos explícitos (a validar en el piloto con comercios reales).

/** Minutos que lleva revisar una lista a mano (comparar, calcular, anotar). */
export const MINUTOS_MANUALES_POR_LISTA = 45;
/** Minutos que lleva con Ensambla: confirmar dudas y aplicar. */
export const MINUTOS_CON_ENSAMBLA_POR_LISTA = 8;
const VENTANA_DIAS = 30;

export type ResumenValor = {
  /** Precios corregidos en los últimos 30 días. */
  preciosCorregidos: number;
  /** Productos que se vendían por debajo del costo nuevo y se detectaron. */
  bajoCosto: number;
  /** Plata por mes que se evita perder vendiendo con el precio viejo. */
  plataProtegidaMes: number;
  /** Listas aplicadas en los últimos 30 días. */
  listasAplicadas: number;
  minutosAhorrados: number;
  /** Cambios aplicados en el sistema que todavía no se trasladaron al mostrador. */
  pendientesDeMostrador: number;
};

export function resumenValor(
  estado: Pick<RedState, 'productos' | 'listas' | 'cambios'>,
  ahora: Date = new Date(),
): ResumenValor {
  const recientes = estado.cambios.filter((c) => diasEntre(c.fecha, ahora) <= VENTANA_DIAS);
  const ventas = (productoId: string) =>
    estado.productos.find((p) => p.id === productoId)?.ventasSemana ?? 0;
  const plataProtegidaMes = recientes.reduce(
    (suma, c) => suma + Math.max(0, c.precioNuevo - c.precioAnterior) * ventas(c.productoId) * 4,
    0,
  );
  const listasAplicadas = estado.listas.filter(
    (l) =>
      l.estado === 'aplicada' && l.aplicadaEl && diasEntre(l.aplicadaEl, ahora) <= VENTANA_DIAS,
  ).length;
  return {
    preciosCorregidos: recientes.filter((c) => c.precioNuevo !== c.precioAnterior).length,
    bajoCosto: recientes.filter((c) => c.precioAnterior < c.costoNuevo).length,
    plataProtegidaMes,
    listasAplicadas,
    minutosAhorrados:
      listasAplicadas * (MINUTOS_MANUALES_POR_LISTA - MINUTOS_CON_ENSAMBLA_POR_LISTA),
    pendientesDeMostrador: estado.cambios.filter(
      (c) => !c.trasladado && c.precioNuevo !== c.precioAnterior,
    ).length,
  };
}

/** Listas que el comercio recibió o subió en el mes calendario actual (para el tope del plan gratis). */
export function listasDelMes(estado: Pick<RedState, 'listas'>, ahora: Date = new Date()): number {
  return estado.listas.filter((lista) => {
    const fecha = new Date(lista.recibidaEl);
    return fecha.getFullYear() === ahora.getFullYear() && fecha.getMonth() === ahora.getMonth();
  }).length;
}
