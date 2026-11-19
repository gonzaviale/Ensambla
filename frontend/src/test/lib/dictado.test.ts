import { describe, expect, it } from 'vitest';
import { MENSAJE_SIN_SOPORTE, mensajeDeErrorDeDictado, unirDictado } from '@/lib/dictado';

let mensaje: string | null;
let texto: string;

describe('mensajeDeErrorDeDictado', () => {
  it('explica cómo habilitar el micrófono cuando no hay permiso', () => {
    cuandoFallaElDictadoPor('not-allowed');

    entoncesElMensajeDice(/permití el micrófono/);
  });

  it('trata igual la falta de permiso del servicio de voz', () => {
    cuandoFallaElDictadoPor('service-not-allowed');

    entoncesElMensajeDice(/permití el micrófono/);
  });

  it('avisa cuando no se escuchó nada', () => {
    cuandoFallaElDictadoPor('no-speech');

    entoncesElMensajeDice(/No te escuché/);
  });

  it('avisa cuando no hay micrófono', () => {
    cuandoFallaElDictadoPor('audio-capture');

    entoncesElMensajeDice(/No encontramos un micrófono/);
  });

  it('avisa cuando no hay conexión', () => {
    cuandoFallaElDictadoPor('network');

    entoncesElMensajeDice(/No hay conexión/);
  });

  it('ante un error desconocido invita a seguir escribiendo', () => {
    cuandoFallaElDictadoPor('otra-cosa');

    entoncesElMensajeDice(/seguir escribiendo/);
  });

  it('no avisa cuando la persona misma cortó el dictado', () => {
    cuandoFallaElDictadoPor('aborted');

    entoncesNoHayMensaje();
  });

  it('el aviso de navegador sin soporte sugiere escribir', () => {
    entoncesElAvisoSinSoporteSugiereEscribir();
  });
});

describe('unirDictado', () => {
  it('suma lo dictado a lo escrito con un solo espacio', () => {
    cuandoSeUne('Tengo un kiosco.  ', ' vendo golosinas ');

    entoncesElTextoEs('Tengo un kiosco. vendo golosinas');
  });

  it('si no había nada escrito, queda sólo lo dictado', () => {
    cuandoSeUne('', ' vendo velas ');

    entoncesElTextoEs('vendo velas');
  });

  it('si lo escrito eran sólo espacios, queda sólo lo dictado', () => {
    cuandoSeUne('   ', 'vendo velas');

    entoncesElTextoEs('vendo velas');
  });

  it('si no se dictó nada, deja lo escrito tal cual', () => {
    cuandoSeUne('Tengo un kiosco', '   ');

    entoncesElTextoEs('Tengo un kiosco');
  });
});

function cuandoFallaElDictadoPor(codigo: string) {
  mensaje = mensajeDeErrorDeDictado(codigo);
}

function cuandoSeUne(escrito: string, dictado: string) {
  texto = unirDictado(escrito, dictado);
}

function entoncesElMensajeDice(esperado: RegExp) {
  expect(mensaje).toMatch(esperado);
}

function entoncesNoHayMensaje() {
  expect(mensaje).toBeNull();
}

function entoncesElAvisoSinSoporteSugiereEscribir() {
  expect(MENSAJE_SIN_SOPORTE).toMatch(/escribí/);
}

function entoncesElTextoEs(esperado: string) {
  expect(texto).toBe(esperado);
}
