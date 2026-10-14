import { z } from 'zod';
import {
  movimientoRegistradoSchema,
  movimientoSchema,
  stockProductoSchema,
  type FiltroStock,
  type NuevoMovimiento,
} from '@/types/stock';
import { http } from './client';
import { pageSchema } from './types';

function aQuery(filtro: object) {
  const params = new URLSearchParams();
  for (const [clave, valor] of Object.entries(filtro)) {
    if (valor !== undefined && valor !== '') params.set(clave, String(valor));
  }
  const query = params.toString();
  return query ? `?${query}` : '';
}

export const registrarMovimiento = (bloqueId: string, movimiento: NuevoMovimiento) =>
  http.post(
    `/bloques/${encodeURIComponent(bloqueId)}/movimientos`,
    movimiento,
    movimientoRegistradoSchema,
  );

// Los últimos movimientos (hasta 100) de las filas de la tabla, o sólo los de una.
export const listarMovimientos = (bloqueId: string, registroId?: string, signal?: AbortSignal) =>
  http.get(
    `/bloques/${encodeURIComponent(bloqueId)}/movimientos${aQuery({ registroId })}`,
    z.array(movimientoSchema),
    signal,
  );

export const consultarStock = (bloqueId: string, filtro: FiltroStock, signal?: AbortSignal) =>
  http.get(
    `/bloques/${encodeURIComponent(bloqueId)}/stock${aQuery(filtro)}`,
    pageSchema(stockProductoSchema),
    signal,
  );
