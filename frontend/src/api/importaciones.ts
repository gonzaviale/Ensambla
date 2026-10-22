import type { Lectura, PedidoDeLectura } from '@/types/importacion';
import { lecturaSchema } from '@/types/importacion';
import { http } from './client';

// La foto viaja junto al bloque de destino en un multipart: la parte `imagen` y la parte `pedido` (JSON). El
// backend toma del pedido el nombre del bloque, `destinoExistente` y, de cada dato, clave, nombre, tipo y opciones.
export function pedirLectura(pedido: PedidoDeLectura, imagen: Blob): Promise<Lectura> {
  const cuerpo = new FormData();
  // Una foto elegida trae su nombre; la que sale de achicarla es un Blob sin nombre.
  cuerpo.append('imagen', imagen, (imagen as Partial<File>).name ?? 'foto.jpg');
  cuerpo.append(
    'pedido',
    new Blob([JSON.stringify({ ...pedido, destinoExistente: pedido.destinoExistente ?? false })], {
      type: 'application/json',
    }),
  );
  return http.postForm('/importaciones/lectura', cuerpo, lecturaSchema);
}
