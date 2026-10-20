import { useMutation } from '@tanstack/react-query';
import { pedirLectura } from '@/api/importaciones';
import { env } from '@/config/env';
import { reducirImagen } from '@/lib/sistema/imagen';
import { lecturaDeEjemplo } from '@/lib/sistema/lectura';
import type { PedidoDeLectura } from '@/types/importacion';

// Con el backend (modo parcial o ninguno) la foto la lee la IA; en el modo completo todo es simulado.
export const LECTURA_CON_IA = env.mocks !== 'completo';

type Entrada = { pedido: PedidoDeLectura; imagen: File | null };

// Con una imagen, la lectura es la del servicio (la IA propone registros y columnas para revisar). Sin imagen
// («Probar con un ejemplo») se arman datos de muestra, sin salir del navegador.
export const useLecturaDeImagen = () =>
  useMutation({
    mutationFn: async ({ pedido, imagen }: Entrada) =>
      imagen
        ? pedirLectura(pedido, await reducirImagen(imagen))
        : lecturaDeEjemplo({ ...pedido, destinoExistente: pedido.destinoExistente ?? false }),
  });
