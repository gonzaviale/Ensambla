import type { Aviso } from './alertas';

export type DestinoDeAviso = { sistemaId: string; bloqueId: string; filaId?: string };

function apuntaA(accion: Aviso['acciones'][number]): DestinoDeAviso | null {
  if (accion.tipo === 'zona' || !accion.bloqueId) return null;
  return {
    sistemaId: accion.sistemaId ?? '',
    bloqueId: accion.bloqueId,
    filaId: 'filaId' in accion ? accion.filaId : undefined,
  };
}

/** El objeto que representa un aviso: el registro si se sabe cuál y, si no, la tabla. Null si no apunta a ninguno. */
export function destinoDeAviso(aviso: Aviso): DestinoDeAviso | null {
  if (aviso.objeto) return aviso.objeto;
  const destinos = aviso.acciones.flatMap((accion) => apuntaA(accion) ?? []);
  const destino = destinos.find((item) => item.filaId) ?? destinos[0];
  return destino ? { ...destino, sistemaId: destino.sistemaId || aviso.sistemaId } : null;
}
