import { z } from 'zod';
import { sistemaSchema } from '@/types/sistema';
import type { Sistema } from '@/lib/sistema/tipos';

const CLAVE = 'ensambla:borrador-de-sesion';
const DIA = 86_400_000;
const borradorSchema = z.object({
  usuarioId: z.string(),
  fecha: z.number(),
  sistemas: z.array(sistemaSchema.extend({ name: z.string() })),
  pendientes: z.array(z.string()),
  eliminados: z.array(z.string()),
});

export function borrarBorradorSesion() {
  try {
    sessionStorage.removeItem(CLAVE);
  } catch {
    /* El estado de memoria sigue disponible. */
  }
}

export function guardarBorradorSesion(datos: {
  usuarioId: string | null;
  sistemas: Sistema[];
  pendientes: string[];
  eliminados: string[];
}) {
  if (!datos.usuarioId) return;
  if (!datos.pendientes.length && !datos.eliminados.length) {
    borrarBorradorSesion();
    return;
  }
  try {
    sessionStorage.setItem(
      CLAVE,
      JSON.stringify({
        ...datos,
        fecha: Date.now(),
        sistemas: datos.sistemas.filter((sistema) => datos.pendientes.includes(sistema.id)),
      }),
    );
  } catch {
    /* No se elimina la copia en memoria si el almacenamiento está lleno. */
  }
}

export function recuperarBorradorSesion(usuarioId: string) {
  try {
    const valor = borradorSchema.safeParse(JSON.parse(sessionStorage.getItem(CLAVE) ?? 'null'));
    if (!valor.success) return null;
    if (Date.now() - valor.data.fecha > DIA) {
      borrarBorradorSesion();
      return null;
    }
    return valor.data.usuarioId === usuarioId ? valor.data : null;
  } catch {
    return null;
  }
}
