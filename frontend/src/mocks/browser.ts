import { setupWorker } from 'msw/browser';
import { env } from '@/config/env';
import { handlersPara } from './handlers';
import { sembrarUsuario } from './db/estado';
import { interceptarFetch } from './interceptarFetch';

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
