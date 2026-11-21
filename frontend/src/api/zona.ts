import type { Zona } from '@/lib/sistema/zona';
import type { PedidoDeAnalisis, PedidoDondeAbrir, VistaDeZona } from '@/types/zona';
import { dondeAbrirSchema, zonaSchema } from '@/types/zona';
import { http } from './client';

export const obtenerZona = (vista: VistaDeZona = 'precios', signal?: AbortSignal): Promise<Zona> =>
  http.get(`/zona/agregados?vista=${vista}`, zonaSchema, signal);

export const analizarZona = (pedido: PedidoDeAnalisis): Promise<Zona> =>
  http.post('/zona/analisis', pedido, zonaSchema);

export const explorarDondeAbrir = (pedido: PedidoDondeAbrir, signal?: AbortSignal) =>
  http.post('/zona/donde-abrir', pedido, dondeAbrirSchema, signal);
