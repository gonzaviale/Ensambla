import { describe, expect, it } from 'vitest';
import { handlersPara } from '@/mocks/handlers';

const rutas = (modo: Parameters<typeof handlersPara>[0]) =>
  handlersPara(modo).map((handler) => {
    const { method, path } = handler.info as { method: string; path: string };
    return `${method} ${path.replace(/^.*\/api/, '')}`;
  });

describe('API simulada según VITE_API_MOCKS', () => {
  it('en modo completo simula bloques y campos', () => {
    expect(rutas('completo')).toEqual(
      expect.arrayContaining([
        'GET /proyectos/:proyectoId/bloques',
        'POST /proyectos/:proyectoId/bloques',
        'PATCH /bloques/:bloqueId',
        'DELETE /bloques/:bloqueId',
        'GET /bloques/:bloqueId/campos',
        'POST /bloques/:bloqueId/campos',
        'PATCH /campos/:campoId',
        'DELETE /campos/:campoId',
      ]),
    );
  });

  it('en modo parcial deja bloques al backend (T-017) y simula sólo campos (T-018)', () => {
    expect(rutas('parcial')).toEqual([
      'GET /bloques/:bloqueId/campos',
      'POST /bloques/:bloqueId/campos',
      'PATCH /campos/:campoId',
      'DELETE /campos/:campoId',
    ]);
  });

  it('en modo ninguno no simula nada', () => {
    expect(rutas('ninguno')).toEqual([]);
  });
});
