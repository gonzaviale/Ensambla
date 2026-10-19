import { setupWorker } from 'msw/browser';
import { env } from '@/config/env';
import type { Sistema } from '@/lib/sistema/tipos';
import type { Preferencias } from '@/types/preferencias';
import { handlersPara } from './handlers';
import { estado, sembrarUsuario } from './db/estado';
import { interceptarFetch } from './interceptarFetch';

const CLAVE = 'ensambla.mock.datos.v1';

type Guardado = {
  sistemas: Record<string, Sistema[]>;
  preferencias: Record<string, Preferencias>;
};

// La API simulada vive en memoria: en el navegador se copia a localStorage para que los sistemas y las
// preferencias sobrevivan a recargar la página, como hacía el mockup.
function restaurar() {
  try {
    const crudo = localStorage.getItem(CLAVE);
    if (!crudo) return;
    const guardado = JSON.parse(crudo) as Partial<Guardado>;
    estado.sistemas = new Map(Object.entries(guardado.sistemas ?? {}));
    estado.preferencias = new Map(Object.entries(guardado.preferencias ?? {}));
  } catch {
    // Datos ilegibles: se arranca de cero.
  }
}

function conservar() {
  try {
    const guardado: Guardado = {
      sistemas: Object.fromEntries(estado.sistemas),
      preferencias: Object.fromEntries(estado.preferencias),
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
