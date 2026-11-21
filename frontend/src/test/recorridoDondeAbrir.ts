import { expect } from 'vitest';
import {
  FILTROS_INICIALES,
  SIN_LIMITE_KM,
  ZONAS,
  puntuar,
  type Filtros,
  type Punto,
  type ZonaPuntuada,
} from '@/lib/dondeAbrir';
import {
  cambiosDePuesto,
  filtrosDesde,
  mejoresParaComparar,
  puntosAMostrar,
  type DondeMostrar,
} from '@/lib/dondeAbrirRecorrido';
import type { RespuestasDeSami } from '@/lib/respuestasDeSami';

const PUNTO_DE_PRUEBA: Punto = {
  tipo: 'escuela',
  latitud: -34.6,
  longitud: -58.4,
  nombre: 'Escuela',
};

export const dadoUnRankingDeCincoZonas = (): ZonaPuntuada[] =>
  ZONAS.slice(0, 5).map((zona) => ({
    ...puntuar(zona, FILTROS_INICIALES),
    puntos: [PUNTO_DE_PRUEBA],
  }));

export const dadoQueContestoQue = (cambios: Partial<RespuestasDeSami>): RespuestasDeSami => ({
  alcance: 'cerca',
  prioridad: 'equilibrado',
  publico: 'vecinos',
  tamano: 'mediano',
  ...cambios,
});

export const cuandoSeTraducenLasRespuestas = (respuestas: RespuestasDeSami): Filtros =>
  filtrosDesde(respuestas, FILTROS_INICIALES);

export const entoncesLaDistanciaMaximaEs = (filtros: Filtros, km: number) =>
  expect(filtros.limites.distanciaMaxKm).toBe(km);

export const entoncesElPublicoEs = (filtros: Filtros, publico: Filtros['publico']) =>
  expect(filtros.publico).toBe(publico);

export const entoncesLaImportanciaDeLasEscuelasEs = (filtros: Filtros, importancia: number) =>
  expect(filtros.importancia.escuelas).toBe(importancia);

export const entoncesSinLimiteDeDistancia = (filtros: Filtros) =>
  entoncesLaDistanciaMaximaEs(filtros, SIN_LIMITE_KM);

export const cuandoSeArmaElPodioConLaElegida = (ranking: ZonaPuntuada[], posicion: number) =>
  mejoresParaComparar(ranking, ranking[posicion]);

export const cuandoSeArmaElPodioSinElegida = (ranking: ZonaPuntuada[]) =>
  mejoresParaComparar(ranking, undefined);

export const entoncesSeCompara = (
  zonas: ZonaPuntuada[],
  ranking: ZonaPuntuada[],
  puestos: number[],
) =>
  expect(zonas.map((item) => item.zona.id)).toEqual(
    puestos.map((puesto) => ranking[puesto]?.zona.id),
  );

export const cuandoSeMuestranLosPuntosDe = (
  dondeMostrar: DondeMostrar,
  ranking: ZonaPuntuada[],
  elegida: ZonaPuntuada | undefined,
) => puntosAMostrar(dondeMostrar, ranking, elegida);

export const entoncesHayPuntos = (puntos: Punto[], cantidad: number) =>
  expect(puntos).toHaveLength(cantidad);

export const cuandoCambiaElOrden = (anterior: string[], actual: string[]) =>
  cambiosDePuesto({ anterior: anterior.join('|'), actual: actual.join('|') });

export const entoncesLosCambiosSon = (
  cambios: ReturnType<typeof cambiosDePuesto>,
  esperados: ReturnType<typeof cambiosDePuesto>,
) => expect(cambios).toEqual(esperados);
