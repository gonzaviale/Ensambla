import { afterEach, describe, expect, it } from 'vitest';
import {
  borrarBorradorSesion,
  guardarBorradorSesion,
  recuperarBorradorSesion,
} from '@/lib/borradorSesion';
import { makeSystem } from '@/lib/sistema/core';
import { useSistemaStore } from '@/stores/sistemaStore';

afterEach(() => {
  sessionStorage.clear();
});

describe('borradores asociados a la sesión', () => {
  it('recupera cambios pendientes sólo para la misma identidad', () => {
    const sistema = makeSystem('Sin guardar');
    guardarBorradorSesion({
      usuarioId: 'ana',
      sistemas: [sistema],
      pendientes: [sistema.id],
      eliminados: [],
    });

    expect(recuperarBorradorSesion('otra-persona')).toBeNull();
    expect(recuperarBorradorSesion('ana')?.sistemas[0]?.name).toBe('Sin guardar');
  });

  it('al reingresar combina el servidor con los cambios pendientes y vuelve a intentar guardarlos', () => {
    const original = makeSystem('Anterior');
    const editado = { ...original, name: 'Corregido sin guardar' };
    guardarBorradorSesion({
      usuarioId: 'ana',
      sistemas: [editado],
      pendientes: [editado.id],
      eliminados: [],
    });

    useSistemaStore.getState().hidratar('ana', [original]);

    expect(useSistemaStore.getState().sistemas[0]?.name).toBe('Corregido sin guardar');
    expect(useSistemaStore.getState().pendientes).toEqual([original.id]);
  });

  it('una salida explícita elimina el borrador de la pestaña', () => {
    const sistema = makeSystem('Borrador');
    guardarBorradorSesion({
      usuarioId: 'ana',
      sistemas: [sistema],
      pendientes: [sistema.id],
      eliminados: [],
    });

    borrarBorradorSesion();

    expect(recuperarBorradorSesion('ana')).toBeNull();
  });

  it('un borrador inválido no se incorpora a la cuenta', () => {
    sessionStorage.setItem('ensambla:borrador-de-sesion', '{roto');

    expect(recuperarBorradorSesion('ana')).toBeNull();
  });
});
