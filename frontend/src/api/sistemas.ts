import { z } from 'zod';
import type { Sistema } from '@/lib/sistema/tipos';
import { preferenciasSchema, type Preferencias } from '@/types/preferencias';
import { sistemaSchema, sistemasSchema } from '@/types/sistema';
import { http } from './client';

const ENCABEZADO_LEIDO_EN = 'X-Leido-En';

// A qué hora leyó esta pestaña los sistemas, según el servidor. Al guardar se la devolvemos: así el servidor sabe qué
// filas no conocíamos (las que Sami cargó por WhatsApp mientras la pestaña estaba abierta) y no las elimina.
let leidoEn: string | null = null;

export async function listarSistemas(signal?: AbortSignal) {
  const { datos, encabezado } = await http.getConEncabezado(
    '/sistemas',
    sistemasSchema,
    ENCABEZADO_LEIDO_EN,
    signal,
  );
  leidoEn = encabezado;
  return datos;
}

// El sistema se guarda entero y con el id que le puso la pantalla: si no existe se crea y si existe se
// reemplaza, así que se puede reintentar sin duplicar nada.
export const guardarSistema = (sistema: Sistema) =>
  http.put(
    `/sistemas/${encodeURIComponent(sistema.id)}`,
    sistema,
    sistemaSchema,
    leidoEn ? { [ENCABEZADO_LEIDO_EN]: leidoEn } : undefined,
  );

export const eliminarSistema = (id: string) =>
  http.delete(`/sistemas/${encodeURIComponent(id)}`, z.undefined());

export const obtenerPreferencias = (signal?: AbortSignal) =>
  http.get('/preferencias', preferenciasSchema, signal);

export const guardarPreferencias = (preferencias: Preferencias) =>
  http.put('/preferencias', preferencias, preferenciasSchema);
