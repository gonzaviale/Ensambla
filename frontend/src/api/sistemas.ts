import { z } from 'zod';
import type { Sistema } from '@/lib/sistema/tipos';
import { preferenciasSchema, type Preferencias } from '@/types/preferencias';
import { sistemaSchema, sistemasSchema } from '@/types/sistema';
import { http } from './client';

export const listarSistemas = (signal?: AbortSignal) =>
  http.get('/sistemas', sistemasSchema, signal);

// El sistema se guarda entero y con el id que le puso la pantalla: si no existe se crea y si existe se
// reemplaza, así que se puede reintentar sin duplicar nada.
export const guardarSistema = (sistema: Sistema) =>
  http.put(`/sistemas/${encodeURIComponent(sistema.id)}`, sistema, sistemaSchema);

export const eliminarSistema = (id: string) =>
  http.delete(`/sistemas/${encodeURIComponent(id)}`, z.undefined());

export const obtenerPreferencias = (signal?: AbortSignal) =>
  http.get('/preferencias', preferenciasSchema, signal);

export const guardarPreferencias = (preferencias: Preferencias) =>
  http.put('/preferencias', preferencias, preferenciasSchema);
