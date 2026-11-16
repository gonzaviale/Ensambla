import { describe, expect, it } from 'vitest';
import { textoDeNegocios } from '@/lib/planes/negocios';

describe('textoDeNegocios', () => {
  it('con un solo negocio lo dice en singular', () => {
    expect(textoDeNegocios(1)).toBe('1 negocio');
  });

  it('con más de uno cuenta el propio y los que se pueden sumar', () => {
    expect(textoDeNegocios(6)).toBe('Tu negocio y hasta 5 más');
    expect(textoDeNegocios(2)).toBe('Tu negocio y hasta 1 más');
  });

  it('sin límite lo dice', () => {
    expect(textoDeNegocios(null)).toBe('Sin límite');
  });
});
