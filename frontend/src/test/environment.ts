import { builtinEnvironments, type Environment } from 'vitest/environments';

// Node fetch exige sus AbortSignal nativos; jsdom los reemplaza por objetos de otro entorno.
const environment: Environment = {
  name: 'ensambla-jsdom',
  transformMode: 'web',
  async setup(global, options) {
    const nodeAbortController = global.AbortController;
    const nodeAbortSignal = global.AbortSignal;
    const result = await builtinEnvironments.jsdom.setup(global, options);
    global.AbortController = nodeAbortController;
    global.AbortSignal = nodeAbortSignal;
    return result;
  },
};

export default environment;

