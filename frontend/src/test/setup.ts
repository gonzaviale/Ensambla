import '@testing-library/jest-dom/vitest';
import { transferableAbortController } from 'node:util';
import { cleanup } from '@testing-library/react';
import { afterAll, afterEach, beforeAll, vi } from 'vitest';
import { invalidarCsrf } from '@/api/client';
import { reiniciarEstado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { reiniciarRecorrido } from '@/stores/recorrido';
import { reiniciarSuscripcion } from '@/stores/suscripcion';

// El fetch nativo de Node requiere sus propias señales, no las de jsdom.
const controller = transferableAbortController();
vi.stubGlobal('AbortController', controller.constructor);
vi.stubGlobal('AbortSignal', controller.signal.constructor);

// En las pruebas el recorrido guiado ya está «visto»: la oferta del primer ingreso no estorba. Los
// tests del recorrido lo ponen en `visto: false` a propósito. Se limpia el almacenamiento porque
// guardar el estado escribe en localStorage y hay pruebas que verifican que quede vacío.
reiniciarRecorrido(true);
localStorage.clear();

beforeAll(() => {
  window.scrollTo = () => undefined;
  server.listen({ onUnhandledRequest: 'error' });
});

afterEach(() => {
  cleanup();
  server.resetHandlers();
  reiniciarEstado();
  invalidarCsrf();
  reiniciarSuscripcion();
  reiniciarRecorrido(true);
  localStorage.clear();
});

afterAll(() => server.close());
