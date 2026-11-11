import {
  ordenDelServidorSchema,
  ordenesDelServidorSchema,
  type CambioDeEstado,
} from '@/types/pedidos';
import { http } from './client';

// Cada negocio ve solo sus órdenes: el servidor las busca por la sesión, nunca por un dato que mande el navegador.
// Trae las 100 más recientes, la más nueva primero.
export const listarOrdenes = (signal?: AbortSignal) =>
  http.get('/ordenes', ordenesDelServidorSchema, signal);

// Una orden ajena responde 404, igual que una que no existe; un paso que no corresponde responde 400.
export const cambiarEstadoDeOrden = (id: string, cambio: CambioDeEstado) =>
  http.patch(`/ordenes/${encodeURIComponent(id)}/estado`, cambio, ordenDelServidorSchema);
