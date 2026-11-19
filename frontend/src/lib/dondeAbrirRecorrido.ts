import {
  FILTROS_INICIALES,
  PRESETS,
  SIN_LIMITE_KM,
  conPreset,
  type Filtros,
  type ZonaPuntuada,
} from './dondeAbrir';
import type { Alcance, RespuestasDeSami } from './respuestasDeSami';

/** Cuántos puestos subió (+) o bajó (−) una zona con el último cambio de filtros; `nueva` si antes estaba afuera. */
export type CambioDePuesto = number | 'nueva';

export type DondeMostrar = 'elegida' | 'todas';

export const RESPUESTAS_INICIALES: RespuestasDeSami = {
  alcance: 'cerca',
  prioridad: 'equilibrado',
  publico: 'vecinos',
  tamano: 'mediano',
};

const DISTANCIA_SEGUN_ALCANCE: Record<Alcance, number> = {
  cerca: FILTROS_INICIALES.limites.distanciaMaxKm,
  toda: SIN_LIMITE_KM,
};

/** Traduce lo que la persona contestó a los filtros con los que se calcula. */
export function filtrosDesde(respuestas: RespuestasDeSami, actuales: Filtros): Filtros {
  const preset = PRESETS.find((item) => item.id === respuestas.prioridad) ?? PRESETS[0];
  const base = preset ? conPreset(actuales, preset) : actuales;
  return {
    ...base,
    publico: respuestas.publico,
    limites: {
      ...FILTROS_INICIALES.limites,
      distanciaMaxKm: DISTANCIA_SEGUN_ALCANCE[respuestas.alcance],
    },
  };
}

/** Las tres mejores para comparar de un vistazo; si se eligió otra, también aparece. */
export function mejoresParaComparar(
  ranking: ZonaPuntuada[],
  elegida: ZonaPuntuada | undefined,
): ZonaPuntuada[] {
  const podio = ranking.slice(0, 3);
  const estaEnElPodio = podio.some((item) => item.zona.id === elegida?.zona.id);
  return elegida && !estaEnElPodio ? [...podio, elegida] : podio;
}

const ZONAS_CON_PUNTOS: Record<
  DondeMostrar,
  (ranking: ZonaPuntuada[], elegida: ZonaPuntuada | undefined) => ZonaPuntuada[]
> = {
  todas: (ranking) => ranking,
  elegida: (_ranking, elegida) => (elegida ? [elegida] : []),
};

/** Los puntos del mapa (escuelas, parques, etc.) de las zonas donde la persona quiere verlos. */
export const puntosAMostrar = (
  dondeMostrar: DondeMostrar,
  ranking: ZonaPuntuada[],
  elegida: ZonaPuntuada | undefined,
) => ZONAS_CON_PUNTOS[dondeMostrar](ranking, elegida).flatMap((item) => item.puntos ?? []);

/** El orden de las zonas como una sola clave, para notar cuándo cambia. */
export const ordenDelRanking = (ranking: ZonaPuntuada[]) =>
  ranking.map((item) => item.zona.id).join('|');

export type HistorialDeOrden = { actual: string; anterior: string };

const idsEnOrden = (orden: string) => (orden ? orden.split('|') : []);

const cambioDeLugar = (antes: string[], id: string, puesto: number): CambioDePuesto => {
  const previo = antes.indexOf(id);
  return previo === -1 ? 'nueva' : previo - puesto;
};

/** Quién subió y quién bajó entre el orden anterior y el actual. */
export function cambiosDePuesto({ actual, anterior }: HistorialDeOrden) {
  const antes = idsEnOrden(anterior);
  return Object.fromEntries(
    idsEnOrden(actual).map((id, puesto) => [id, cambioDeLugar(antes, id, puesto)]),
  ) as Record<string, CambioDePuesto>;
}
