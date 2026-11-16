import {
  opcionesDeCartelSchema,
  promocionesSugeridasSchema,
  textosDeCartelSchema,
  type ContenidoDeCartel,
  type PedidoDeExportacion,
  type PedidoDeTextos,
} from '@/types/carteles';
import { http } from './client';

export const obtenerPromocionesSugeridas = (signal?: AbortSignal) =>
  http.get('/carteles/promociones-sugeridas', promocionesSugeridasSchema, signal);

export const redactarTextosDeCartel = (pedido: PedidoDeTextos) =>
  http.post('/carteles/textos', pedido, textosDeCartelSchema);

export const armarOpcionesDeCartel = (contenido: ContenidoDeCartel) =>
  http.post('/carteles/opciones', contenido, opcionesDeCartelSchema);

export const exportarCartel = (pedido: PedidoDeExportacion) =>
  http.postArchivo('/carteles/exportar', pedido);
