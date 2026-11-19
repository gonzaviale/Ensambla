import '@testing-library/jest-dom/vitest';
import { transferableAbortController } from 'node:util';
import { cleanup } from '@testing-library/react';
import { afterAll, afterEach, beforeAll, vi } from 'vitest';
import { invalidarCsrf } from '@/api/client';
import { ritmoDeSami } from '@/lib/ritmoDeSami';
import { reiniciarEstado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';
import { useSistemaStore } from '@/stores/sistemaStore';

// Las burbujas de Sami aparecen de una en los tests; el escalonado tiene su propia prueba.
ritmoDeSami.maxima = 0;

// El fetch nativo de Node requiere sus propias señales, no las de jsdom.
const controller = transferableAbortController();
vi.stubGlobal('AbortController', controller.constructor);
vi.stubGlobal('AbortSignal', controller.signal.constructor);

// Pedidos al servidor simulado que todavía no terminaron. Al desmontar la pantalla, lo que quedó sin guardar
// se manda; hay que esperar a que llegue antes de limpiar la sesión, o le toca a la prueba que sigue.
let pedidosEnCurso = 0;
let controlesSinPedidos = 0;
const comprobarQueNoHayPedidos = () => {
  controlesSinPedidos = pedidosEnCurso === 0 ? controlesSinPedidos + 1 : 0;
  if (controlesSinPedidos < 4) throw new Error('Todavía hay pedidos en curso');
};
const esperarPedidos = () => {
  controlesSinPedidos = 0;
  return vi
    .waitFor(comprobarQueNoHayPedidos, { timeout: 2000, interval: 10 })
    .catch(() => undefined);
};

beforeAll(() => {
  server.events.on('request:start', () => pedidosEnCurso++);
  server.events.on('request:end', () => pedidosEnCurso--);
  window.scrollTo = () => undefined;
  // jsdom no implementa el desplazamiento de un elemento a la vista.
  Element.prototype.scrollIntoView = () => undefined;
  server.listen({ onUnhandledRequest: 'error' });
});

afterEach(async () => {
  const { pendientes, eliminados } = useSistemaStore.getState();
  const habiaPendientes = pendientes.length > 0 || eliminados.length > 0;
  cleanup();
  if (habiaPendientes || pedidosEnCurso > 0) await esperarPedidos();
  server.resetHandlers();
  reiniciarEstado();
  useSistemaStore.getState().reiniciar();
  invalidarCsrf();
  localStorage.clear();
  sessionStorage.clear();
});

afterAll(() => server.close());
