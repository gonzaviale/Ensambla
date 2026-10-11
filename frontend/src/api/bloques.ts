import { z } from 'zod';
import { bloqueSchema } from '@/types/bloque';
import { http } from './client';

// T-017 (backend implementado): bloques de un proyecto, ordenados por posición.
const id = encodeURIComponent;

export const listarBloques = (proyectoId: string, signal?: AbortSignal) =>
  http.get(`/proyectos/${id(proyectoId)}/bloques`, bloqueSchema.array(), signal);

export const crearBloque = ({ proyectoId, nombre }: { proyectoId: string; nombre: string }) =>
  http.post(`/proyectos/${id(proyectoId)}/bloques`, { nombre }, bloqueSchema);

export const renombrarBloque = ({ bloqueId, nombre }: { bloqueId: string; nombre: string }) =>
  http.patch(`/bloques/${id(bloqueId)}`, { nombre }, bloqueSchema);

/** Borra en cascada los campos y registros del bloque. */
export const eliminarBloque = (bloqueId: string) =>
  http.delete(`/bloques/${id(bloqueId)}`, z.undefined());
