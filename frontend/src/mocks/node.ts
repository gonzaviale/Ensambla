import { setupServer } from 'msw/node';
import { handlersPara } from './handlers';

export const server = setupServer(...handlersPara('completo'));
