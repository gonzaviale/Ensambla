import { describe, expect, it } from 'vitest';
import { desbloquearAvisos } from '@/api/contenidoPlan';
import { obtenerZona, analizarZona } from '@/api/zona';
import { guardarSistema } from '@/api/sistemas';
import { http } from '@/api/client';
import { estado } from '@/mocks/db/estado';
import { setupUsuarioConSesion } from '@/test/testHelpers';
import { makeSystem } from '@/lib/sistema/core';
import { crearAlmacenDeEjemplo } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import { propioDe, productosPropios } from '@/lib/sistema/zona';
import { zonaSchema } from '@/types/zona';

describe('cupos de contenido del plan', () => {
  it('permite seis avisos y una consulta repetida no libera el séptimo', async () => {
    setupUsuarioConSesion();

    const primero = await desbloquearAvisos(['a', 'b', 'a', 'c', 'd', 'e', 'f', 'g']);
    const repetido = await desbloquearAvisos(['a', 'g']);

    expect(primero.autorizadas).toEqual(['a', 'b', 'c', 'd', 'e', 'f']);
    expect(primero.bloqueadas).toBe(1);
    expect(repetido.autorizadas).toEqual(['a']);
    expect(repetido.usadas).toBe(6);
  });

  it('la prueba Premium ofrece todos los avisos sin gastar el cupo gratuito', async () => {
    const usuario = setupUsuarioConSesion();
    estado.pruebasPremium.set(usuario.email, {
      inicioEn: new Date(Date.now() - 1000).toISOString(),
      finEn: new Date(Date.now() + 86400000).toISOString(),
      paginasIaUtilizadas: 0,
      limitePaginasIa: 10,
    });

    const acceso = await desbloquearAvisos(['a', 'b', 'c', 'd']);

    expect(acceso.autorizadas).toHaveLength(4);
    expect(acceso.limite).toBeNull();
    expect(estado.contenidosDesbloqueados.size).toBe(0);
  });

  it('Precios, Qué se vende cerca y Otra zona gastan cada uno sus seis productos mensuales', async () => {
    setupUsuarioConSesion();

    const propia = await obtenerZona();
    const cerca = await obtenerZona('cerca');
    const otra = await analizarZona({ lugar: 'Palermo' });

    expect(propia.productos).toHaveLength(6);
    expect(propia.acceso?.usadas).toBe(6);
    expect(cerca.productos).toHaveLength(6);
    expect(cerca.acceso?.usadas).toBe(6);
    expect(otra.productos).toHaveLength(6);
    expect(otra.acceso?.usadas).toBe(6);
    expect(otra.acceso?.bloqueadas).toBeGreaterThan(0);
  });

  it('cada pantalla desbloquea primero lo que le sirve: lo que tengo, o lo más vendido que me falta', async () => {
    const usuario = setupUsuarioConSesion();
    estado.sistemas.set(usuario.email, [crearAlmacenDeEjemplo(hoy())]);
    const propios = productosPropios(estado.sistemas.get(usuario.email) ?? []);

    const precios = await obtenerZona();
    const cerca = await obtenerZona('cerca');

    // true = ya lo tengo. Precios deja entrar primero lo que tengo; Cerca, lo que me falta (y lo mío al final).
    const lotengo = (zona: typeof precios) =>
      zona.productos.map((producto) => Boolean(propioDe(producto, propios)));
    expect(precios.productos).toHaveLength(6);
    expect(lotengo(precios)[0]).toBe(true);
    expect(lotengo(precios)).toEqual([...lotengo(precios)].sort((a, b) => Number(b) - Number(a)));
    expect(cerca.productos).toHaveLength(6);
    expect(lotengo(cerca)[0]).toBe(false);
    expect(lotengo(cerca)).toEqual([...lotengo(cerca)].sort((a, b) => Number(a) - Number(b)));
  });

  it('una vista desconocida se rechaza', async () => {
    setupUsuarioConSesion();

    await expect(http.get('/zona/agregados?vista=otra', zonaSchema)).rejects.toMatchObject({
      status: 400,
    });
  });

  it('el segundo sistema gratuito falla sin guardarse y el primero sigue editable', async () => {
    const usuario = setupUsuarioConSesion();
    const primero = makeSystem('Primero');
    await guardarSistema(primero);

    await expect(guardarSistema(makeSystem('Segundo'))).rejects.toMatchObject({
      status: 403,
      codigo: 'LIMITE_PLAN',
    });

    await guardarSistema({ ...primero, name: 'Corregido' });
    expect(estado.sistemas.get(usuario.email)).toHaveLength(1);
    expect(estado.sistemas.get(usuario.email)?.[0]?.name).toBe('Corregido');
  });
});
