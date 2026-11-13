import { setupWorker } from 'msw/browser';
import { env } from '@/config/env';
import type { Sistema } from '@/lib/sistema/tipos';
import type { Preferencias } from '@/types/preferencias';
import type { ConfiguracionNegocio } from '@/types/negocio';
import type { ObjetivoDelServidor } from '@/types/objetivos';
import { pruebaPremiumSchema, type PruebaPremium } from '@/types/planes';
import { handlersPara } from './handlers';
import { sembrarDemoCargada } from './db/demoCargada';
import type { NegociosDeLaCuenta } from './db/negocios';
import { estado, sembrarUsuario } from './db/estado';
import { interceptarFetch } from './interceptarFetch';

const CLAVE = 'ensambla.mock.datos.v1';

type Guardado = {
  sistemas: Record<string, Sistema[]>;
  preferencias: Record<string, Preferencias>;
  negocios: Record<string, ConfiguracionNegocio>;
  negociosDeLaCuenta?: Record<string, NegociosDeLaCuenta>;
  pruebasPremium: Record<string, PruebaPremium>;
  contenidosDesbloqueados?: Record<string, string[]>;
  objetivos?: Record<string, ObjetivoDelServidor[]>;
};

// La API simulada vive en memoria: en el navegador se copia a localStorage para que los sistemas y las
// preferencias sobrevivan a recargar la página, como hacía el mockup.
function restaurar() {
  try {
    const crudo = localStorage.getItem(CLAVE);
    if (!crudo) return;
    const guardado = JSON.parse(crudo) as Partial<Guardado>;
    estado.sistemas = new Map(Object.entries(guardado.sistemas ?? {}));
    estado.contenidosDesbloqueados = new Map(
      Object.entries(guardado.contenidosDesbloqueados ?? {}),
    );
    estado.preferencias = new Map(Object.entries(guardado.preferencias ?? {}));
    estado.objetivos = new Map(Object.entries(guardado.objetivos ?? {}));
    estado.negocios = new Map(Object.entries(guardado.negocios ?? {}));
    estado.negociosDeLaCuenta = new Map(Object.entries(guardado.negociosDeLaCuenta ?? {}));
    estado.pruebasPremium = new Map(
      Object.entries(guardado.pruebasPremium ?? {}).flatMap(([email, valor]) => {
        const prueba = pruebaPremiumSchema.safeParse(valor);
        return prueba.success ? [[email, prueba.data] as const] : [];
      }),
    );
  } catch {
    // Datos ilegibles: se arranca de cero.
  }
}

function conservar() {
  try {
    const guardado: Guardado = {
      sistemas: Object.fromEntries(estado.sistemas),
      preferencias: Object.fromEntries(estado.preferencias),
      objetivos: Object.fromEntries(estado.objetivos),
      negocios: Object.fromEntries(estado.negocios),
      negociosDeLaCuenta: Object.fromEntries(estado.negociosDeLaCuenta),
      pruebasPremium: Object.fromEntries(estado.pruebasPremium),
      contenidosDesbloqueados: Object.fromEntries(estado.contenidosDesbloqueados),
    };
    localStorage.setItem(CLAVE, JSON.stringify(guardado));
  } catch {
    // Sin almacenamiento (modo privado, lleno): la demo sigue andando, solo no se conserva.
  }
}

restaurar();
estado.alCambiar = conservar;

// Cuenta sólo del modo simulado; no se crea ni autentica ningún usuario en el backend.
if (env.mocks === 'completo') {
  sembrarUsuario({
    nombre: 'Demo Ensambla',
    email: 'demo@ensambla.local',
    contrasenia: 'EnsamblaDemo2026!',
  });
  if (env.demoCargada) {
    sembrarDemoCargada('demo@ensambla.local');
  }
}

const handlers = handlersPara(env.mocks);
const worker = setupWorker(...handlers);

// Con service worker si el navegador lo permite; si no, interceptando fetch.
export async function iniciarMocks() {
  try {
    await worker.start({ onUnhandledRequest: 'bypass', quiet: true });
  } catch {
    interceptarFetch(handlers);
  }
}
