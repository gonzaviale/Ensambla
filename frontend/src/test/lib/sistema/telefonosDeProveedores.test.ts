import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { recordarTelefono, telefonoDe } from '@/lib/sistema/telefonosDeProveedores';

describe('los teléfonos de los proveedores', () => {
  beforeEach(() => window.localStorage.clear());
  afterEach(() => vi.restoreAllMocks());

  it('un proveedor sin número anotado devuelve vacío', () => {
    expect(telefonoDe('Molinos')).toBe('');
  });

  it('recuerda el número de cada proveedor sin pisar los otros', () => {
    recordarTelefono('Molinos', '11 5555-0101');
    recordarTelefono('Arcor', '11 5555-0202');

    expect(telefonoDe('Molinos')).toBe('11 5555-0101');
    expect(telefonoDe('Arcor')).toBe('11 5555-0202');
  });

  it('si lo guardado está roto empieza de cero en vez de fallar', () => {
    window.localStorage.setItem('ensambla:telefonos-de-proveedores', '{no es json');

    expect(telefonoDe('Molinos')).toBe('');
  });

  it('sin almacenamiento disponible no falla ni recuerda', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('bloqueado');
    });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('bloqueado');
    });

    expect(() => recordarTelefono('Molinos', '11 5555-0101')).not.toThrow();
    expect(telefonoDe('Molinos')).toBe('');
  });
});
