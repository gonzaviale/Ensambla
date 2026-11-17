import { useReducer } from 'react';
import { esHoraDePreguntar, estaPospuesta, posponerHastaManana } from '@/lib/consultaDeSami';
import { hoy } from '@/lib/sistema/formato';
import type { RespuestaPendiente } from '@/types/objetivos';
import { useRespuestasPendientes } from './queries';

/** Lo que Sami pregunta hoy: sin apuro, ni lo de esta semana antes del miércoles, ni lo que la persona dejó para mañana. */
export function usePreguntasDeSami() {
  const { pendientes } = useRespuestasPendientes();
  const [, repintar] = useReducer((vez: number) => vez + 1, 0);
  const dia = hoy();
  const paraHoy = pendientes.filter((pendiente) => sePreguntaHoy(pendiente, dia));

  const posponer = (miniId: string) => {
    posponerHastaManana(miniId, dia);
    repintar();
  };

  return { primera: paraHoy[0], otras: paraHoy.slice(1), posponer };
}

function sePreguntaHoy({ miniObjetivo }: RespuestaPendiente, dia: string): boolean {
  return esHoraDePreguntar(miniObjetivo.semanaInicio, dia) && !estaPospuesta(miniObjetivo.id, dia);
}
