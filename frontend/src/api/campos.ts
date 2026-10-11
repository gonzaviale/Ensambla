import { z } from 'zod';
import { campoSchema, type GuardarCampo } from '@/types/campo';
import { http } from './client';

// T-018: campos de un bloque. Mientras el backend no lo tenga, MSW lo simula también en modo parcial.
const id = encodeURIComponent;

export const listarCampos = (bloqueId: string, signal?: AbortSignal) =>
  http.get(`/bloques/${id(bloqueId)}/campos`, campoSchema.array(), signal);

export const crearCampo = ({ bloqueId, datos }: { bloqueId: string; datos: GuardarCampo }) =>
  http.post(`/bloques/${id(bloqueId)}/campos`, datos, campoSchema);

/** PATCH con el cuerpo completo: lo que no se manda vuelve al valor por defecto. */
export const editarCampo = ({ campoId, datos }: { campoId: string; datos: GuardarCampo }) =>
  http.patch(`/campos/${id(campoId)}`, datos, campoSchema);

export const eliminarCampo = (campoId: string) =>
  http.delete(`/campos/${id(campoId)}`, z.undefined());
