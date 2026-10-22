import type { Sistema } from '@/lib/sistema/tipos';
import type { AvisoDelServidor } from '@/types/avisos';
import type { ConfiguracionNegocio } from '@/types/negocio';
import type { PruebaPremium } from '@/types/planes';
import { PREFERENCIAS_INICIALES, type Preferencias } from '@/types/preferencias';

export type UsuarioMock = {
  id: string;
  nombre: string;
  email: string;
  contrasenia: string;
};

type Estado = {
  usuarios: Map<string, UsuarioMock>;
  sesionUsuarioId: string | null;
  /** Los sistemas de cada cuenta (por email). */
  sistemas: Map<string, Sistema[]>;
  preferencias: Map<string, Preferencias>;
  negocios: Map<string, ConfiguracionNegocio>;
  /** Los avisos que el motor del servidor dejó a cada cuenta (por email); los tests los siembran. */
  avisosDelServidor: Map<string, AvisoDelServidor[]>;
  pruebasPremium: Map<string, PruebaPremium>;
  /** Se llama después de cada cambio de datos: el navegador lo usa para no perderlos al recargar. */
  alCambiar: (() => void) | null;
};

export const estado: Estado = {
  usuarios: new Map(),
  sesionUsuarioId: null,
  sistemas: new Map(),
  preferencias: new Map(),
  negocios: new Map(),
  avisosDelServidor: new Map(),
  pruebasPremium: new Map(),
  alCambiar: null,
};

export function reiniciarEstado() {
  estado.usuarios = new Map();
  estado.sesionUsuarioId = null;
  estado.sistemas = new Map();
  estado.preferencias = new Map();
  estado.negocios = new Map();
  estado.avisosDelServidor = new Map();
  estado.pruebasPremium = new Map();
  estado.alCambiar = null;
}

export function usuarioActual() {
  return [...estado.usuarios.values()].find((usuario) => usuario.id === estado.sesionUsuarioId);
}

export const sistemasDe = (email: string) => estado.sistemas.get(email) ?? [];

export const preferenciasDe = (email: string) =>
  estado.preferencias.get(email) ?? PREFERENCIAS_INICIALES;

export function guardarSistemasDe(email: string, sistemas: Sistema[]) {
  estado.sistemas.set(email, sistemas);
  estado.alCambiar?.();
}

export function guardarPreferenciasDe(email: string, preferencias: Preferencias) {
  estado.preferencias.set(email, preferencias);
  estado.alCambiar?.();
}

export function sembrarUsuario(datos: Partial<UsuarioMock> & { conSesion?: boolean } = {}) {
  const { conSesion, ...resto } = datos;
  const usuario: UsuarioMock = {
    id: crypto.randomUUID(),
    nombre: 'Lucía Gómez',
    email: 'lucia@example.com',
    contrasenia: 'una-contrasenia-segura',
    ...resto,
  };
  estado.usuarios.set(usuario.email, usuario);
  if (conSesion) estado.sesionUsuarioId = usuario.id;
  return usuario;
}
