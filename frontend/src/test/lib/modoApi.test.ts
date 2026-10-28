import { afterEach, describe, expect, it } from 'vitest';
import {
  CLAVE_ENTRAR_COMO_DEMO,
  CLAVE_MODO_API,
  guardarModoApi,
  modoApiElegido,
  modoMocksEfectivo,
  tomarEntrarComoDemo,
} from '@/lib/modoApi';

afterEach(() => localStorage.clear());

describe('modo de datos elegido con el interruptor', () => {
  it('sin elegir nada, manda lo compilado', () => {
    expect(modoApiElegido()).toBeNull();
    expect(modoMocksEfectivo('ninguno', null)).toBe('ninguno');
    expect(modoMocksEfectivo('completo', null)).toBe('completo');
  });

  it('«mock» simula todo, se haya compilado como se haya compilado', () => {
    expect(modoMocksEfectivo('ninguno', 'mock')).toBe('completo');
    expect(modoMocksEfectivo('parcial', 'mock')).toBe('completo');
  });

  it('«real» usa el backend; si se compiló parcial, lo respeta', () => {
    expect(modoMocksEfectivo('completo', 'real')).toBe('ninguno');
    expect(modoMocksEfectivo('ninguno', 'real')).toBe('ninguno');
    expect(modoMocksEfectivo('parcial', 'real')).toBe('parcial');
  });

  it('guarda la elección y la lee al arrancar', () => {
    guardarModoApi('mock');
    expect(modoApiElegido()).toBe('mock');
    guardarModoApi('real');
    expect(modoApiElegido()).toBe('real');
  });

  it('ignora un valor guardado que no es un modo', () => {
    localStorage.setItem(CLAVE_MODO_API, 'cualquiera');
    expect(modoApiElegido()).toBeNull();
  });

  it('al pasar a «mock» pide entrar con la demo una sola vez', () => {
    guardarModoApi('mock');

    expect(tomarEntrarComoDemo()).toBe(true);
    expect(tomarEntrarComoDemo()).toBe(false);
  });

  it('al pasar a «real» no queda pendiente entrar con la demo', () => {
    guardarModoApi('mock');
    guardarModoApi('real');

    expect(localStorage.getItem(CLAVE_ENTRAR_COMO_DEMO)).toBeNull();
    expect(tomarEntrarComoDemo()).toBe(false);
  });
});
