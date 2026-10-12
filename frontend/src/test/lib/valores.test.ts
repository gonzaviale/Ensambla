import { describe, expect, it } from 'vitest';
import { SIN_DATO, formatearValor } from '@/lib/valores';

const FILA = '00000000-0000-4000-8000-0000000000c1';

describe('formatearValor', () => {
  it('formatea números, fechas y sí/no', () => {
    expect(formatearValor('NUMERO', 1500.5)).toBe('1.500,5');
    expect(formatearValor('FECHA', '2026-10-20')).toBe('20/10/2026');
    expect(formatearValor('SI_NO', false)).toBe('No');
    expect(formatearValor('TEXTO', '')).toBe(SIN_DATO);
  });

  it('una conexión muestra la etiqueta de la fila, no su id', () => {
    expect(formatearValor('RELACION', FILA, { estado: 'lista', texto: 'Harina 000' })).toBe(
      'Harina 000',
    );
  });

  it.each([
    [{ estado: 'cargando' } as const, 'Cargando…'],
    [{ estado: 'eliminada' } as const, 'Fila eliminada'],
    [{ estado: 'error' } as const, 'No pudimos cargarla'],
    [undefined, 'Cargando…'],
  ])('una conexión %o se muestra como «%s»', (etiqueta, texto) => {
    expect(formatearValor('RELACION', FILA, etiqueta)).toBe(texto);
  });

  it('una conexión sin fila elegida no tiene dato', () => {
    expect(formatearValor('RELACION', undefined, { estado: 'cargando' })).toBe(SIN_DATO);
  });
});
