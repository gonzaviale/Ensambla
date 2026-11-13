import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { explorarDondeAbrir } from '@/api/zona';
import {
  SIN_LIMITE_COMPETENCIA,
  SIN_LIMITE_KM,
  type FactorClave,
  type Filtros,
} from '@/lib/dondeAbrir';
import { adaptarDondeAbrir } from '@/lib/dondeAbrirRemoto';
import type { PedidoDondeAbrir } from '@/types/zona';

const FACTOR_API: Record<FactorClave, keyof PedidoDondeAbrir['importancia']> = {
  competencia: 'COMPETENCIA',
  movimiento: 'ACTIVIDAD_ACCESO',
  vecinos: 'DENSIDAD',
  escuelas: 'ESCUELAS',
  oficinas: 'OFICINAS',
  transporte: 'TRANSPORTE',
  parques: 'PARQUES',
  salud: 'SALUD',
  complementarios: 'COMPLEMENTARIOS',
  demanda: 'DEMANDA_ENSAMBLA',
  poder: 'PERFIL_SOCIOECONOMICO',
  alquiler: 'ALQUILER',
};

const RUBRO_API = {
  almacen: 'ALMACEN',
  papeleria: 'PAPELERIA',
  kiosco: 'KIOSCO',
} as const;

const PUBLICO_API = {
  vecinos: 'VECINOS',
  estudiantes: 'ESTUDIANTES',
  oficinas: 'OFICINAS',
} as const;

export type PuntoElegido = { latitud: number; longitud: number };

export function pedidoDondeAbrir(filtros: Filtros, punto?: PuntoElegido | null): PedidoDondeAbrir {
  return {
    rubro: RUBRO_API[filtros.rubro],
    publico: PUBLICO_API[filtros.publico],
    importancia: Object.fromEntries(
      Object.entries(filtros.importancia).map(([factor, valor]) => [
        FACTOR_API[factor as FactorClave],
        valor,
      ]),
    ),
    limites: {
      ...(filtros.limites.distanciaMaxKm < SIN_LIMITE_KM && {
        distanciaMaxKm: filtros.limites.distanciaMaxKm,
      }),
      ...(filtros.limites.competenciaMax < SIN_LIMITE_COMPETENCIA && {
        competenciaMax: filtros.limites.competenciaMax,
      }),
      escuelasMin: filtros.limites.escuelasMin,
      parquesMin: filtros.limites.parquesMin,
      transporteMin: filtros.limites.transporteMin,
    },
    alquileres: [],
    ...(punto && { punto }),
  };
}

export function useDondeAbrir(filtros: Filtros, habilitada: boolean, punto?: PuntoElegido | null) {
  const pedido = pedidoDondeAbrir(filtros, punto);
  return useQuery({
    queryKey: ['zona', 'donde-abrir', pedido],
    queryFn: ({ signal }) => explorarDondeAbrir(pedido, signal),
    select: (respuesta) => adaptarDondeAbrir(respuesta, filtros),
    enabled: habilitada,
    staleTime: 5 * 60_000,
    placeholderData: keepPreviousData,
  });
}
