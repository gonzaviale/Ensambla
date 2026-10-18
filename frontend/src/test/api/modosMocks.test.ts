import { describe, expect, it } from 'vitest';
import { handlersPara } from '@/mocks/handlers';

const rutas = (modo: Parameters<typeof handlersPara>[0]) =>
  handlersPara(modo).map((handler) => {
    const { method, path } = handler.info as { method: string; path: string };
    return `${method} ${path.replace(/^.*\/api/, '')}`;
  });

describe('API simulada según VITE_API_MOCKS', () => {
  it('en modo completo simula la sesión', () => {
    expect(rutas('completo')).toEqual(
      expect.arrayContaining(['POST /auth/login', 'POST /auth/register', 'GET /auth/me']),
    );
  });

  it('en modo parcial deja todo al backend real', () => {
    expect(rutas('parcial')).toEqual([]);
  });

  it('en modo ninguno no simula nada', () => {
    expect(rutas('ninguno')).toEqual([]);
  });
});
