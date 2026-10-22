import { describe, expect, it } from 'vitest';
import { handlersPara } from '@/mocks/handlers';

const rutas = (modo: Parameters<typeof handlersPara>[0]) =>
  handlersPara(modo).map((handler) => {
    const { method, path } = handler.info as { method: string; path: string };
    return `${method} ${path.replace(/^.*\/api/, '')}`;
  });

describe('API simulada según VITE_API_MOCKS', () => {
  it('los planes y la prueba sólo se simulan en el modo completo', () => {
    expect(rutas('completo')).toEqual(
      expect.arrayContaining(['GET /planes', 'POST /planes/prueba']),
    );
    expect(rutas('parcial').filter((ruta) => ruta.includes('/planes'))).toEqual([]);
  });
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

  it('los avisos del motor de monitoreo sólo se simulan en el modo completo: en parcial los da el backend', () => {
    expect(rutas('completo')).toEqual(
      expect.arrayContaining([
        'GET /avisos',
        'POST /avisos/:id/aplicar',
        'POST /avisos/:id/descartar',
      ]),
    );
    expect(rutas('parcial').filter((ruta) => ruta.includes('/avisos'))).toEqual([]);
  });

  it('en modo ninguno no simula nada', () => {
    expect(rutas('ninguno')).toEqual([]);
  });

  it('la ubicación y los rubros sólo se simulan en modo completo', () => {
    expect(rutas('completo')).toEqual(
      expect.arrayContaining(['POST /ubicacion/direcciones', 'GET /ubicacion/rubros']),
    );
    expect(rutas('parcial').filter((ruta) => ruta.includes('/ubicacion/'))).toEqual([]);
  });
});
