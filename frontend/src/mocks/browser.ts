import { setupWorker } from 'msw/browser';
import { env } from '@/config/env';
import { handlersPara } from './handlers';

export const worker = setupWorker(...handlersPara(env.mocks));
