import { describe, expect, it } from 'vitest';
import { handlersPara } from '@/mocks/handlers';

type Modo = Parameters<typeof handlersPara>[0];

let rutasSimuladas: string[] = [];

describe('API simulada según VITE_API_MOCKS', () => {
  it('los planes y la prueba se simulan en el modo completo', () => {
    cuandoSeSimulaElModo('completo');

    entoncesSeSimulan('GET /planes', 'POST /planes/prueba');
  });

  it('los planes y la prueba no se simulan en el modo parcial', () => {
    cuandoSeSimulaElModo('parcial');

    entoncesNoSeSimulaNingunaRutaQueContenga('/planes');
  });

  it('en modo completo simula la sesión y los sistemas', () => {
    cuandoSeSimulaElModo('completo');

    entoncesSeSimulan(
      'POST /auth/login',
      'POST /auth/register',
      'GET /auth/me',
      'GET /sistemas',
      'PUT /sistemas/:id',
      'GET /preferencias',
    );
  });

  it('en modo parcial deja todas las rutas implementadas al backend real', () => {
    cuandoSeSimulaElModo('parcial');

    entoncesNoSeSimulaNingunaRuta();
  });

  it('la lectura de fotos se simula en el modo completo', () => {
    cuandoSeSimulaElModo('completo');

    entoncesSeSimulan('POST /importaciones/lectura');
  });

  it('en parcial la lectura de fotos la hace la IA del backend', () => {
    cuandoSeSimulaElModo('parcial');

    entoncesNoSeSimulaNingunaRutaQueContenga('/importaciones/lectura');
  });

  it('los avisos del motor de monitoreo se simulan en el modo completo', () => {
    cuandoSeSimulaElModo('completo');

    entoncesSeSimulan('GET /avisos', 'POST /avisos/:id/aplicar', 'POST /avisos/:id/descartar');
  });

  it('en parcial los avisos del motor de monitoreo los da el backend', () => {
    cuandoSeSimulaElModo('parcial');

    entoncesNoSeSimulaNingunaRutaQueContenga('/avisos');
  });

  it('en modo ninguno no simula nada', () => {
    cuandoSeSimulaElModo('ninguno');

    entoncesNoSeSimulaNingunaRuta();
  });

  it('la ubicación y los rubros se simulan en modo completo', () => {
    cuandoSeSimulaElModo('completo');

    entoncesSeSimulan('POST /ubicacion/direcciones', 'GET /ubicacion/rubros');
  });

  it('en parcial la ubicación y los rubros los da el backend', () => {
    cuandoSeSimulaElModo('parcial');

    entoncesNoSeSimulaNingunaRutaQueContenga('/ubicacion/');
  });
});

function cuandoSeSimulaElModo(modo: Modo) {
  rutasSimuladas = handlersPara(modo).map((handler) => {
    const { method, path } = handler.info as { method: string; path: string };
    return `${method} ${path.replace(/^.*\/api/, '')}`;
  });
}

function entoncesSeSimulan(...rutas: string[]) {
  expect(rutasSimuladas).toEqual(expect.arrayContaining(rutas));
}

function entoncesNoSeSimulaNingunaRuta() {
  expect(rutasSimuladas).toEqual([]);
}

function entoncesNoSeSimulaNingunaRutaQueContenga(fragmento: string) {
  expect(rutasSimuladas.filter((ruta) => ruta.includes(fragmento))).toEqual([]);
}
