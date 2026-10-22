import { z } from 'zod';
import {
  plantillaDelServidorSchema,
  proyectoCreadoSchema,
  respuestaDeConversacionSchema,
  type PedidoDeConversacion,
  type PedidoDeProyecto,
} from '@/types/samiProyecto';
import { http } from './client';

export const conversarConSami = (pedido: PedidoDeConversacion) =>
  http.post('/sami/conversacion', pedido, respuestaDeConversacionSchema);

export const crearProyectoConSami = (pedido: PedidoDeProyecto) =>
  http.post('/sami/proyectos', pedido, proyectoCreadoSchema);

export const listarPlantillasDelServidor = (signal?: AbortSignal) =>
  http.get('/plantillas', z.array(plantillaDelServidorSchema), signal);
