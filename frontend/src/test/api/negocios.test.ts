import { beforeEach, describe, expect, it } from 'vitest';
import {
  activarNegocio,
  crearNegocio,
  eliminarNegocio,
  listarNegocios,
  obtenerResumenConjunto,
} from '@/api/negocios';
import { ApiError } from '@/api/apiError';
import { estado, guardarSistemasDe } from '@/mocks/db/estado';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';
import { setupUsuarioConSesion } from '@/test/testHelpers';

const conPremium = (email: string) =>
  estado.pruebasPremium.set(email, {
    inicioEn: new Date(Date.now() - 1000).toISOString(),
    finEn: new Date(Date.now() + 86_400_000).toISOString(),
    paginasIaUtilizadas: 0,
    limitePaginasIa: 10,
  });

const primero = <T>(lista: T[]): T => {
  const [elemento] = lista;
  if (elemento === undefined) throw new Error('La lista está vacía');
  return elemento;
};

const rechazo = async (promesa: Promise<unknown>) => {
  const error = await promesa.then(
    () => null,
    (causa: unknown) => causa,
  );
  expect(error).toBeInstanceOf(ApiError);
  return error as ApiError;
};

describe('Negocios de la cuenta (contrato docs/negocios-de-la-cuenta.md)', () => {
  beforeEach(() => {
    setupUsuarioConSesion({ email: 'lucia@example.com', nombre: 'Lucía Gómez' });
  });

  it('una cuenta nueva tiene un solo negocio, activo, y el Gratuito no deja crear otro', async () => {
    const lista = await listarNegocios();
    expect(lista.limite).toBe(1);
    expect(lista.puedeCrear).toBe(false);
    expect(lista.negocios).toHaveLength(1);
    expect(primero(lista.negocios)).toMatchObject({
      nombre: 'Negocio de Lucía Gómez',
      activo: true,
    });
    expect(lista.activoId).toBe(primero(lista.negocios).id);
  });

  it('con el cupo agotado crear responde 403 con el código que lleva a Planes', async () => {
    const error = await rechazo(crearNegocio({ nombre: 'Otro negocio' }));
    expect(error).toMatchObject({ status: 403, codigo: 'LIMITE_NEGOCIOS' });
    expect(error.message).toMatch(/Premium/);
    expect((await listarNegocios()).negocios).toHaveLength(1);
  });

  it('con Premium se pueden tener hasta seis y el séptimo se rechaza', async () => {
    conPremium('lucia@example.com');
    expect((await listarNegocios()).limite).toBe(6);
    for (let n = 2; n <= 6; n++) {
      const creado = await crearNegocio({ nombre: `Negocio ${n}`, rubro: 'KIOSCO' });
      expect(creado).toMatchObject({ nombre: `Negocio ${n}`, rubro: 'KIOSCO', activo: false });
    }
    const lista = await listarNegocios();
    expect(lista.negocios).toHaveLength(6);
    expect(lista.puedeCrear).toBe(false);
    expect(await rechazo(crearNegocio({ nombre: 'Séptimo' }))).toMatchObject({
      status: 403,
      codigo: 'LIMITE_NEGOCIOS',
    });
  });

  it('crear no cambia el negocio activo y validar rechaza lo inválido', async () => {
    conPremium('lucia@example.com');
    const antes = await listarNegocios();
    await crearNegocio({ nombre: 'Segundo' });
    expect((await listarNegocios()).activoId).toBe(antes.activoId);
    expect(await rechazo(crearNegocio({ nombre: '   ' }))).toMatchObject({ status: 400 });
    expect(
      await rechazo(crearNegocio({ nombre: 'Con rubro raro', rubro: 'NO_EXISTE' })),
    ).toMatchObject({
      status: 400,
    });
  });

  it('activar cambia el negocio activo, es idempotente y uno ajeno responde 404', async () => {
    conPremium('lucia@example.com');
    const segundo = await crearNegocio({ nombre: 'Segundo' });
    const lista = await activarNegocio(segundo.id);
    expect(lista.activoId).toBe(segundo.id);
    expect(lista.negocios.filter((negocio) => negocio.activo).map((negocio) => negocio.id)).toEqual(
      [segundo.id],
    );
    expect((await activarNegocio(segundo.id)).activoId).toBe(segundo.id);
    expect(await rechazo(activarNegocio(crypto.randomUUID()))).toMatchObject({ status: 404 });
  });

  it('no mezcla los negocios entre cuentas: el de otra cuenta responde 404', async () => {
    conPremium('lucia@example.com');
    const propio = await crearNegocio({ nombre: 'De Lucía' });
    setupUsuarioConSesion({ email: 'otra@example.com', nombre: 'Otra' });
    expect((await listarNegocios()).negocios.map((negocio) => negocio.id)).not.toContain(propio.id);
    expect(await rechazo(activarNegocio(propio.id))).toMatchObject({ status: 404 });
    expect(await rechazo(eliminarNegocio(propio.id))).toMatchObject({ status: 404 });
  });

  it('eliminar quita el negocio; si era el activo pasa a serlo el más antiguo que queda', async () => {
    conPremium('lucia@example.com');
    const inicial = primero((await listarNegocios()).negocios);
    const segundo = await crearNegocio({ nombre: 'Segundo' });
    await crearNegocio({ nombre: 'Tercero' });
    await activarNegocio(segundo.id);
    await eliminarNegocio(segundo.id);
    const lista = await listarNegocios();
    expect(lista.negocios.map((negocio) => negocio.nombre)).toEqual([inicial.nombre, 'Tercero']);
    expect(lista.activoId).toBe(inicial.id);
    await eliminarNegocio(lista.negocios.at(1)?.id ?? '');
    expect((await listarNegocios()).activoId).toBe(inicial.id);
  });

  it('el último negocio de la cuenta no se puede eliminar (409)', async () => {
    const unico = primero((await listarNegocios()).negocios);
    expect(await rechazo(eliminarNegocio(unico.id))).toMatchObject({ status: 409 });
    expect((await listarNegocios()).negocios).toHaveLength(1);
  });

  it('el resumen conjunto lista todos los negocios; los nuevos arrancan sin datos', async () => {
    conPremium('lucia@example.com');
    await crearNegocio({ nombre: 'Nuevo' });
    const resumen = await obtenerResumenConjunto();
    expect(resumen.dias).toBe(30);
    expect(resumen.total.negocios).toBe(2);
    expect(resumen.negocios.map((negocio) => negocio.hayDatos)).toEqual([false, false]);
    expect(resumen.negocios.at(1)).toMatchObject({
      nombre: 'Nuevo',
      vendiste: null,
      queda: null,
      avisosPendientes: 0,
    });
    expect(resumen.total).toMatchObject({ vendiste: 0, gastos: 0, queda: 0, teDeben: 0 });
  });

  it('el resumen conjunto suma lo vendido por el primer negocio y sus avisos pendientes', async () => {
    guardarSistemasDe('lucia@example.com', [crearAlmacenCargado(hoy())]);
    const resumen = await obtenerResumenConjunto(366);
    const propio = primero(resumen.negocios);
    expect(propio.hayDatos).toBe(true);
    expect(propio.vendiste).toBeGreaterThan(0);
    expect(resumen.total.vendiste).toBe(propio.vendiste);
    expect(resumen.dias).toBe(366);
  });

  it('rechaza una cantidad de días inválida', async () => {
    expect(await rechazo(obtenerResumenConjunto(0))).toMatchObject({ status: 400 });
    expect(await rechazo(obtenerResumenConjunto(367))).toMatchObject({ status: 400 });
  });
});
