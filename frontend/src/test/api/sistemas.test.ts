import { describe, expect, it } from 'vitest';
import { ApiError } from '@/api/apiError';
import {
  eliminarSistema,
  guardarPreferencias,
  guardarSistema,
  listarSistemas,
  obtenerPreferencias,
} from '@/api/sistemas';
import { makeTemplate } from '@/lib/sistema/core';
import { estado } from '@/mocks/db/estado';
import { setupUsuarioConSesion, setupUsuarioSinSesion } from '@/test/testHelpers';

describe('API de sistemas', () => {
  it('guarda un sistema nuevo, lo lista y lo reemplaza sin duplicarlo', async () => {
    setupUsuarioConSesion();
    const sistema = makeTemplate('shop', {}, true);

    await guardarSistema(sistema);
    await guardarSistema({ ...sistema, name: 'Mi kiosco' });

    const lista = await listarSistemas();
    expect(lista).toHaveLength(1);
    expect(lista[0]?.name).toBe('Mi kiosco');
    expect(lista[0]?.sections[0]?.blocks).toHaveLength(3);
  });

  it('cada cuenta ve solo sus sistemas', async () => {
    const lucia = setupUsuarioConSesion({ email: 'lucia@example.com' });
    await guardarSistema(makeTemplate('shop', {}, false));
    estado.sesionUsuarioId = null;
    setupUsuarioConSesion({ email: 'otra@example.com' });

    expect(await listarSistemas()).toEqual([]);
    expect(estado.sistemas.get(lucia.email)).toHaveLength(1);
  });

  it('devuelve los sistemas en el orden en que se crearon', async () => {
    setupUsuarioConSesion();
    const primero = { ...makeTemplate('shop', {}, false), createdAt: '2026-10-01T10:00:00.000Z' };
    const segundo = {
      ...makeTemplate('warehouse', {}, false),
      createdAt: '2026-10-02T10:00:00.000Z',
    };
    await guardarSistema(segundo);
    await guardarSistema(primero);

    expect((await listarSistemas()).map((sistema) => sistema.id)).toEqual([primero.id, segundo.id]);
  });

  it('rechaza un sistema inválido con el campo que falla', async () => {
    setupUsuarioConSesion();
    const sistema = makeTemplate('shop', {}, false);

    const error = await guardarSistema({ ...sistema, name: '   ' }).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).status).toBe(400);
    expect(await listarSistemas()).toEqual([]);
  });

  it('elimina un sistema y avisa si ya no existe', async () => {
    setupUsuarioConSesion();
    const sistema = makeTemplate('shop', {}, false);
    await guardarSistema(sistema);

    await eliminarSistema(sistema.id);

    expect(await listarSistemas()).toEqual([]);
    await expect(eliminarSistema(sistema.id)).rejects.toMatchObject({ status: 404 });
  });

  it('pide sesión', async () => {
    setupUsuarioSinSesion();

    await expect(listarSistemas()).rejects.toMatchObject({ status: 401 });
  });
});

describe('API de preferencias', () => {
  it('arranca con todo prendido y guarda lo que la persona decide', async () => {
    setupUsuarioConSesion({}, { bienvenidaVista: false });

    expect(await obtenerPreferencias()).toEqual({
      descartadas: [],
      grupos: {},
      bienvenidaVista: false,
    });
    await guardarPreferencias({
      descartadas: ['stock:1'],
      grupos: { zona: false },
      bienvenidaVista: true,
    });

    expect(await obtenerPreferencias()).toEqual({
      descartadas: ['stock:1'],
      grupos: { zona: false },
      bienvenidaVista: true,
    });
  });
});
