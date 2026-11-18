import { beforeEach, describe, expect, it } from 'vitest';
import {
  guardarNegocioActivo,
  leerNegocioGuardado,
  listaDeNegocios,
  negocioActivoDe,
  resumenConjuntoDe,
} from '@/lib/negocios/lista';
import { negocioDePrueba } from '@/test/testHelpers';
import type { PanelDelNegocio } from '@/types/panel';

const ALMACEN = negocioDePrueba('almacen', 'Almacén San Justo');
const KIOSCO = negocioDePrueba('kiosco', 'Kiosco de la estación');

const panelCon = (metricas: Partial<NonNullable<PanelDelNegocio['metricas']>>) =>
  ({ hayDatos: true, metricas }) as PanelDelNegocio;
const PANEL_SIN_VENTAS = { hayDatos: false, metricas: null } as PanelDelNegocio;

const givenNegocioGuardado = (id: string) => guardarNegocioActivo(id);

describe('el negocio activo', () => {
  beforeEach(() => localStorage.clear());

  it('se recuerda en el navegador', () => {
    givenNegocioGuardado('kiosco');

    expect(leerNegocioGuardado()).toBe('kiosco');
  });

  it('es el pedido si existe y, si no, el primero', () => {
    expect(negocioActivoDe([ALMACEN, KIOSCO], 'kiosco')).toBe(KIOSCO);
    expect(negocioActivoDe([ALMACEN, KIOSCO], 'uno-que-ya-no-esta')).toBe(ALMACEN);
    expect(negocioActivoDe([], 'kiosco')).toBeUndefined();
  });
});

describe('listaDeNegocios', () => {
  it('arma un negocio por cuaderno y marca el activo', () => {
    const lista = listaDeNegocios([ALMACEN, KIOSCO], 'kiosco', null);

    expect(lista.activoId).toBe('kiosco');
    expect(lista.negocios).toEqual([
      { id: 'almacen', nombre: 'Almacén San Justo', activo: false, creadoEn: ALMACEN.createdAt },
      { id: 'kiosco', nombre: 'Kiosco de la estación', activo: true, creadoEn: KIOSCO.createdAt },
    ]);
  });

  it('deja crear mientras no se llegue al límite del plan', () => {
    expect(listaDeNegocios([ALMACEN], null, 1).puedeCrear).toBe(false);
    expect(listaDeNegocios([ALMACEN], null, 2).puedeCrear).toBe(true);
    expect(listaDeNegocios([ALMACEN, KIOSCO], null, null).puedeCrear).toBe(true);
  });

  it('sin cuadernos no hay negocio activo', () => {
    expect(listaDeNegocios([], 'kiosco', 1)).toMatchObject({ activoId: null, negocios: [] });
  });
});

describe('resumenConjuntoDe', () => {
  it('suma el panel de cada negocio y deja afuera al que no tiene ventas', () => {
    const { negocios } = listaDeNegocios([ALMACEN, KIOSCO], 'almacen', null);

    const resumen = resumenConjuntoDe(negocios, [
      panelCon({ vendiste: 1000, gastos: 300, queda: 700, teDeben: 50 }),
      PANEL_SIN_VENTAS,
    ]);

    expect(resumen.total).toEqual({
      negocios: 2,
      vendiste: 1000,
      gastos: 300,
      queda: 700,
      teDeben: 50,
    });
    expect(resumen.negocios[0]).toMatchObject({ id: 'almacen', activo: true, hayDatos: true });
    expect(resumen.negocios[1]).toMatchObject({ id: 'kiosco', hayDatos: false, vendiste: null });
  });

  it('no dice cuántos avisos tiene cada negocio: el servidor no los separa', () => {
    const { negocios } = listaDeNegocios([ALMACEN], null, null);

    expect(
      resumenConjuntoDe(negocios, [PANEL_SIN_VENTAS]).negocios[0]?.avisosPendientes,
    ).toBeNull();
  });
});
