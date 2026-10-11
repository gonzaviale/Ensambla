import { builtinEnvironments, type Environment } from 'vitest/environments';

// Node fetch exige sus AbortSignal, FormData, Blob y File nativos; jsdom los reemplaza por objetos
// de otro entorno (la subida de fotos manda multipart).
const environment: Environment = {
  name: 'ensambla-jsdom',
  transformMode: 'web',
  async setup(global, options) {
    const nodeAbortController = global.AbortController;
    const nodeAbortSignal = global.AbortSignal;
    const { FormData: nodeFormData, Blob: nodeBlob, File: nodeFile } = global;
    const result = await builtinEnvironments.jsdom.setup(global, options);
    global.AbortController = nodeAbortController;
    global.AbortSignal = nodeAbortSignal;
    global.FormData = nodeFormData;
    global.Blob = nodeBlob;
    global.File = nodeFile;
    return result;
  },
};

export default environment;

