import '@testing-library/jest-dom/vitest';
import { transferableAbortController } from 'node:util';
import { cleanup } from '@testing-library/react';
import { afterAll, afterEach, beforeAll, vi } from 'vitest';
import { invalidarCsrf } from '@/api/client';
import { reiniciarEstado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { useSistemaStore } from '@/stores/sistemaStore';

// El fetch nativo de Node requiere sus propias señales, no las de jsdom.
const controller = transferableAbortController();
vi.stubGlobal('AbortController', controller.constructor);
vi.stubGlobal('AbortSignal', controller.signal.constructor);

beforeAll(() => {
  window.scrollTo = () => undefined;
  server.listen({ onUnhandledRequest: 'error' });
});

afterEach(() => {
  cleanup();
  server.resetHandlers();
  reiniciarEstado();
  useSistemaStore.getState().reiniciar();
  invalidarCsrf();
  localStorage.clear();
});

afterAll(() => server.close());
