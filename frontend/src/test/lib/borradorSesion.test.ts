import { afterEach, describe, expect, it } from 'vitest';
import {
  borrarBorradorSesion,
  guardarBorradorSesion,
  recuperarBorradorSesion,
} from '@/lib/borradorSesion';
import { makeSystem } from '@/lib/sistema/core';
import { useSistemaStore } from '@/stores/sistemaStore';

type Sistema = ReturnType<typeof makeSystem>;
type Borrador = ReturnType<typeof recuperarBorradorSesion>;

let sistema: Sistema;
let borrador: Borrador;

afterEach(() => {
  sessionStorage.clear();
});

describe('borradores asociados a la sesión', () => {
  it('recupera los cambios pendientes de la misma identidad', () => {
    dadoUnBorradorSinGuardarDe('ana');

    cuandoSeRecuperaElBorradorDe('ana');

    entoncesElBorradorTraeElSistema('Sin guardar');
  });

  it('no recupera los cambios pendientes de otra identidad', () => {
    dadoUnBorradorSinGuardarDe('ana');

    cuandoSeRecuperaElBorradorDe('otra-persona');

    entoncesNoHayBorrador();
  });

  it('al reingresar combina el servidor con los cambios pendientes', () => {
    dadoUnBorradorEditadoDeUnSistemaQueElServidorTieneSinCorregir();

    cuandoAnaReingresa();

    entoncesLaSesionTieneElNombre('Corregido sin guardar');
  });

  it('al reingresar vuelve a intentar guardar los cambios pendientes', () => {
    dadoUnBorradorEditadoDeUnSistemaQueElServidorTieneSinCorregir();

    cuandoAnaReingresa();

    entoncesQuedaPendienteElSistema();
  });

  it('una salida explícita elimina el borrador de la pestaña', () => {
    dadoUnBorradorSinGuardarDe('ana');

    cuandoSeBorraElBorrador();

    entoncesYaNoSeRecuperaElBorradorDe('ana');
  });

  it('un borrador inválido no se incorpora a la cuenta', () => {
    dadoUnBorradorRoto();

    cuandoSeRecuperaElBorradorDe('ana');

    entoncesNoHayBorrador();
  });
});

function dadoUnBorradorSinGuardarDe(usuarioId: string) {
  sistema = makeSystem('Sin guardar');
  guardarBorradorSesion({
    usuarioId,
    sistemas: [sistema],
    pendientes: [sistema.id],
    eliminados: [],
  });
}

function dadoUnBorradorEditadoDeUnSistemaQueElServidorTieneSinCorregir() {
  sistema = makeSystem('Anterior');
  const editado = { ...sistema, name: 'Corregido sin guardar' };
  guardarBorradorSesion({
    usuarioId: 'ana',
    sistemas: [editado],
    pendientes: [editado.id],
    eliminados: [],
  });
}

function dadoUnBorradorRoto() {
  sessionStorage.setItem('ensambla:borrador-de-sesion', '{roto');
}

function cuandoSeRecuperaElBorradorDe(usuarioId: string) {
  borrador = recuperarBorradorSesion(usuarioId);
}

function cuandoAnaReingresa() {
  useSistemaStore.getState().hidratar('ana', [sistema]);
}

function cuandoSeBorraElBorrador() {
  borrarBorradorSesion();
}

function entoncesElBorradorTraeElSistema(nombre: string) {
  expect(borrador?.sistemas[0]?.name).toBe(nombre);
}

function entoncesNoHayBorrador() {
  expect(borrador).toBeNull();
}

function entoncesLaSesionTieneElNombre(nombre: string) {
  expect(useSistemaStore.getState().sistemas[0]?.name).toBe(nombre);
}

function entoncesQuedaPendienteElSistema() {
  expect(useSistemaStore.getState().pendientes).toEqual([sistema.id]);
}

function entoncesYaNoSeRecuperaElBorradorDe(usuarioId: string) {
  expect(recuperarBorradorSesion(usuarioId)).toBeNull();
}
