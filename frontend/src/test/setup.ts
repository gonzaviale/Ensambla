import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterAll, afterEach, beforeAll } from 'vitest';
import { invalidarCsrf } from '@/api/client';
import { reiniciarEstado } from '@/mocks/db/estado';
import { server } from '@/mocks/node';

beforeAll(() => {
  window.scrollTo = () => undefined;
  server.listen({ onUnhandledRequest: 'error' });
});

afterEach(() => {
  cleanup();
  server.resetHandlers();
  reiniciarEstado();
  invalidarCsrf();
  localStorage.clear();
});

afterAll(() => server.close());
