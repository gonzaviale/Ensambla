import { z } from 'zod';
import { usuarioSchema, type Login, type Registro, type Usuario } from '@/types/auth';
import { ApiError } from './apiError';
import { http, invalidarCsrf } from './client';

// Sesión real por cookie HttpOnly: la identidad vive en el servidor, nunca en localStorage.

// 401 significa "no hay sesión": para el front es un estado, no un error.
export async function obtenerUsuarioActual(signal?: AbortSignal): Promise<Usuario | null> {
  try {
    return await http.get('/auth/me', usuarioSchema, signal);
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) return null;
    throw error;
  }
}

export async function iniciarSesion(datos: Login): Promise<Usuario> {
  const usuario = await http.post('/auth/login', datos, usuarioSchema);
  invalidarCsrf();
  return usuario;
}

// El registro crea usuario y negocio en una transacción pero no abre sesión: después hay que
// llamar a iniciarSesion.
export function registrarUsuario(datos: Registro): Promise<Usuario> {
  return http.post('/auth/register', datos, usuarioSchema);
}

export async function cerrarSesion(): Promise<void> {
  try {
    await http.post('/auth/logout', undefined, z.void());
  } finally {
    invalidarCsrf();
  }
}
