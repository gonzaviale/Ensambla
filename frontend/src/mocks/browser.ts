import { setupWorker } from 'msw/browser';
import { env } from '@/config/env';
import { handlersPara } from './handlers';
import { sembrarUsuario } from './db/estado';

// Cuenta sólo del modo simulado; no se crea ni autentica ningún usuario en el backend.
if (env.mocks === 'completo') {
  sembrarUsuario({
    nombre: 'Demo Ensambla',
    email: 'demo@ensambla.local',
    contrasenia: 'EnsamblaDemo2026!',
  });
}

export const worker = setupWorker(...handlersPara(env.mocks));
