import { describe, expect, it } from 'vitest';
import { MENSAJE_SIN_SOPORTE, mensajeDeErrorDeDictado, unirDictado } from '@/lib/dictado';

describe('mensajeDeErrorDeDictado', () => {
  it('explica cómo habilitar el micrófono cuando no hay permiso', () => {
    expect(mensajeDeErrorDeDictado('not-allowed')).toMatch(/permití el micrófono/);
    expect(mensajeDeErrorDeDictado('service-not-allowed')).toBe(
      mensajeDeErrorDeDictado('not-allowed'),
    );
  });

  it('avisa cuando no se escuchó nada, no hay micrófono o no hay conexión', () => {
    expect(mensajeDeErrorDeDictado('no-speech')).toMatch(/No te escuché/);
    expect(mensajeDeErrorDeDictado('audio-capture')).toMatch(/No encontramos un micrófono/);
    expect(mensajeDeErrorDeDictado('network')).toMatch(/No hay conexión/);
  });

  it('ante un error desconocido invita a seguir escribiendo', () => {
    expect(mensajeDeErrorDeDictado('otra-cosa')).toMatch(/seguir escribiendo/);
  });

  it('no avisa cuando la persona misma cortó el dictado', () => {
    expect(mensajeDeErrorDeDictado('aborted')).toBeNull();
  });

  it('el aviso de navegador sin soporte sugiere escribir', () => {
    expect(MENSAJE_SIN_SOPORTE).toMatch(/escribí/);
  });
});

describe('unirDictado', () => {
  it('suma lo dictado a lo escrito con un solo espacio', () => {
    expect(unirDictado('Tengo un kiosco.  ', ' vendo golosinas ')).toBe(
      'Tengo un kiosco. vendo golosinas',
    );
  });

  it('si no había nada escrito, queda sólo lo dictado', () => {
    expect(unirDictado('', ' vendo velas ')).toBe('vendo velas');
    expect(unirDictado('   ', 'vendo velas')).toBe('vendo velas');
  });

  it('si no se dictó nada, deja lo escrito tal cual', () => {
    expect(unirDictado('Tengo un kiosco', '   ')).toBe('Tengo un kiosco');
  });
});
