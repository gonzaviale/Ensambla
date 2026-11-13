import { describe, expect, it } from 'vitest';
import { rubroLegible, textoDeAvisos } from '@/lib/negocios/presentacion';

describe('textoDeAvisos', () => {
  it('dice cuántos avisos pendientes hay, en singular o plural, o que no hay', () => {
    expect(textoDeAvisos(0)).toBe('Sin avisos pendientes');
    expect(textoDeAvisos(1)).toBe('1 aviso pendiente');
    expect(textoDeAvisos(4)).toBe('4 avisos pendientes');
  });
});

describe('rubroLegible', () => {
  it('pasa el código del rubro a un texto con mayúscula inicial', () => {
    expect(rubroLegible('KIOSCO')).toBe('Kiosco');
    expect(rubroLegible('VENTA_ONLINE')).toBe('Venta online');
  });
});
