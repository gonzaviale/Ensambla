import { describe, expect, it } from 'vitest';
import { handlersPara } from '@/mocks/handlers';

const rutas = (modo: Parameters<typeof handlersPara>[0]) =>
  handlersPara(modo).map((handler) => {
    const { method, path } = handler.info as { method: string; path: string };
    return `${method} ${path.replace(/^.*\/api/, '')}`;
  });

describe('API simulada según VITE_API_MOCKS', () => {
  it('en modo completo simula la sesión y los sistemas', () => {
    expect(rutas('completo')).toEqual(
      expect.arrayContaining([
        'POST /auth/login',
        'POST /auth/register',
        'GET /auth/me',
        'GET /sistemas',
        'PUT /sistemas/:id',
        'GET /preferencias',
      ]),
    );
  });

  it('en modo parcial simula lo que el backend no tiene y deja la sesión al real', () => {
    const parcial = rutas('parcial');

    expect(parcial).toEqual(expect.arrayContaining(['GET /sistemas', 'PUT /preferencias']));
    expect(parcial.filter((ruta) => ruta.includes('/auth/'))).toEqual([]);
  });

  it('la lectura de fotos sólo se simula en el modo completo: en parcial la hace la IA del backend', () => {
    expect(rutas('completo')).toContain('POST /importaciones/lectura');
    expect(rutas('parcial')).not.toContain('POST /importaciones/lectura');
  });

  it('en modo ninguno no simula nada', () => {
    expect(rutas('ninguno')).toEqual([]);
  });
});
