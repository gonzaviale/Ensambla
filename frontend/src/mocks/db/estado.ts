import type { Sistema } from '@/lib/sistema/tipos';
import type { AvisoDelServidor } from '@/types/avisos';
import type { ConfiguracionNegocio } from '@/types/negocio';
import type { ObjetivoDelServidor } from '@/types/objetivos';
import type { NegociosDeLaCuenta } from './negocios';
import type { DatosAutogestion } from './objetivosAutogestionados';
import type { PruebaPremium } from '@/types/planes';
import type { VerificacionWhatsapp, VinculoWhatsapp } from '@/types/whatsapp';
import { PREFERENCIAS_INICIALES, type Preferencias } from '@/types/preferencias';

export type UsuarioMock = {
  id: string;
  nombre: string;
  email: string;
  contrasenia: string;
};

export type CuentaWhatsapp = {
  vinculo: VinculoWhatsapp | null;
  verificacion: (VerificacionWhatsapp & { intentos: number }) | null;
};

type Estado = {
  usuarios: Map<string, UsuarioMock>;
  sesionUsuarioId: string | null;
  /**
   * Las secciones de cada negocio. Las del primer negocio de la cuenta van con el email (como cuando había uno solo);
   * las de los demás, con `email#idDelNegocio`. Ver `claveDeSistemas`.
   */
  sistemas: Map<string, Sistema[]>;
  preferencias: Map<string, Preferencias>;
  negocios: Map<string, ConfiguracionNegocio>;
  /** Los negocios de cada cuenta (por email) y cuál está activo; el primero se crea al pedirlos por primera vez. */
  negociosDeLaCuenta: Map<string, NegociosDeLaCuenta>;
  /** Los avisos que el motor del servidor dejó a cada cuenta (por email); los tests los siembran. */
  avisosDelServidor: Map<string, AvisoDelServidor[]>;
  /** Los objetivos de cada cuenta (por email), del más viejo al más nuevo. */
  objetivos: Map<string, ObjetivoDelServidor[]>;
  /** Mini-objetivos, progreso, decisiones y cierres de cada objetivo (por id); se arman la primera vez que se piden. */
  autogestion: Map<string, DatosAutogestion>;
  pruebasPremium: Map<string, PruebaPremium>;
  contenidosDesbloqueados: Map<string, string[]>;
  paginasIa: Map<string, number>;
  /** El WhatsApp de cada cuenta (por email): el número vinculado y la verificación en curso. */
  whatsapp: Map<string, CuentaWhatsapp>;
  /** Se llama después de cada cambio de datos: el navegador lo usa para no perderlos al recargar. */
  alCambiar: (() => void) | null;
};

export const estado: Estado = {
  usuarios: new Map(),
  sesionUsuarioId: null,
  sistemas: new Map(),
  preferencias: new Map(),
  negocios: new Map(),
  negociosDeLaCuenta: new Map(),
  avisosDelServidor: new Map(),
  objetivos: new Map(),
  autogestion: new Map(),
  pruebasPremium: new Map(),
  contenidosDesbloqueados: new Map(),
  paginasIa: new Map(),
  whatsapp: new Map(),
  alCambiar: null,
};

export function reiniciarEstado() {
  estado.usuarios = new Map();
  estado.sesionUsuarioId = null;
  estado.sistemas = new Map();
  estado.preferencias = new Map();
  estado.negocios = new Map();
  estado.negociosDeLaCuenta = new Map();
  estado.avisosDelServidor = new Map();
  estado.objetivos = new Map();
  estado.autogestion = new Map();
  estado.pruebasPremium = new Map();
  estado.contenidosDesbloqueados = new Map();
  estado.paginasIa = new Map();
  estado.whatsapp = new Map();
  estado.alCambiar = null;
}

export function usuarioActual() {
  return [...estado.usuarios.values()].find((usuario) => usuario.id === estado.sesionUsuarioId);
}

/**
 * Dónde se guardan las secciones de un negocio; sin decir cuál, las del negocio activo. Las del primero quedan con el
 * email, así una cuenta de un solo negocio (y los tests que la siembran) siguen igual que antes.
 */
export function claveDeSistemas(email: string, negocioId?: string) {
  const cuenta = estado.negociosDeLaCuenta.get(email);
  const id = negocioId ?? cuenta?.activoId;
  if (!cuenta || !id || id === cuenta.negocios[0]?.id) return email;
  return `${email}#${id}`;
}

export const sistemasDe = (email: string, negocioId?: string) =>
  estado.sistemas.get(claveDeSistemas(email, negocioId)) ?? [];

export const preferenciasDe = (email: string) =>
  estado.preferencias.get(email) ?? PREFERENCIAS_INICIALES;

export function guardarSistemasDe(email: string, sistemas: Sistema[], negocioId?: string) {
  estado.sistemas.set(claveDeSistemas(email, negocioId), sistemas);
  estado.alCambiar?.();
}

export const objetivosDe = (email: string) => estado.objetivos.get(email) ?? [];

export function guardarObjetivosDe(email: string, objetivos: ObjetivoDelServidor[]) {
  estado.objetivos.set(email, objetivos);
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
