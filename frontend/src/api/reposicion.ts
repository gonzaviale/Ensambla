import { reposicionSchema, type PedidoAlProveedor } from '@/types/reposicion';
import { http } from './client';

const rutaDeReposicion = (sistemaId: string) => `/proyectos/${encodeURIComponent(sistemaId)}/reposicion`;

// Los productos agotados o bajo su mínimo, con la cantidad que se sugiere pedir y agrupados por proveedor.
export const consultarReposicion = (sistemaId: string, signal?: AbortSignal) =>
  http.get(rutaDeReposicion(sistemaId), reposicionSchema, signal);

// El pedido a un proveedor como PDF, con las cantidades que la persona dejó.
export const descargarPedidoProveedor = (sistemaId: string, pedido: PedidoAlProveedor) =>
  http.postArchivo(`${rutaDeReposicion(sistemaId)}/pdf`, pedido);
