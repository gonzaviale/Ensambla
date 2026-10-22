import { describe, expect, it } from 'vitest';
import { desbloquearAvisos } from '@/api/contenidoPlan';
import { obtenerZona, analizarZona } from '@/api/zona';
import { guardarSistema } from '@/api/sistemas';
import { estado } from '@/mocks/db/estado';
import { setupUsuarioConSesion } from '@/test/testHelpers';
import { makeSystem } from '@/lib/sistema/core';

describe('cupos de contenido del plan', () => {
  it('permite tres avisos y una consulta repetida no libera el cuarto', async () => {
    setupUsuarioConSesion();

    const primero = await desbloquearAvisos(['a', 'b', 'a', 'c', 'd']);
    const repetido = await desbloquearAvisos(['a', 'd']);

    expect(primero.autorizadas).toEqual(['a', 'b', 'c']);
    expect(primero.bloqueadas).toBe(1);
    expect(repetido.autorizadas).toEqual(['a']);
    expect(repetido.usadas).toBe(3);
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

  it('Mi zona y Otra zona comparten los tres productos mensuales', async () => {
    setupUsuarioConSesion();

    const propia = await obtenerZona();
    const otra = await analizarZona({ lugar: 'Palermo' });

    expect(propia.productos).toHaveLength(3);
    expect(propia.acceso?.usadas).toBe(3);
    expect(otra.acceso?.usadas).toBe(3);
    expect(otra.productos).toHaveLength(0);
    expect(otra.acceso?.bloqueadas).toBeGreaterThan(0);
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
