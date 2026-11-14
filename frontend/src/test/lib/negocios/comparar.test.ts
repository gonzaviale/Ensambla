import { describe, expect, it } from 'vitest';
import {
  ORDEN_INICIAL,
  ordenar,
  siguienteOrden,
  textoDelPeriodo,
  valorDeMetrica,
} from '@/lib/negocios/comparar';
import type { ResumenDeNegocio } from '@/types/negocios';

const negocio = (id: string, datos: Partial<ResumenDeNegocio> = {}): ResumenDeNegocio => ({
  id,
  nombre: id,
  rubro: 'OTRO',
  activo: false,
  hayDatos: true,
  vendiste: 0,
  gastos: 0,
  queda: 0,
  teDeben: 0,
  avisosPendientes: 0,
  ...datos,
});

const ALMACEN = negocio('almacen', { vendiste: 1250000, queda: 840000, avisosPendientes: 3 });
const KIOSCO = negocio('kiosco', { vendiste: 280000, queda: -72000, avisosPendientes: 2 });
const FERIA = negocio('feria', {
  hayDatos: false,
  vendiste: null,
  gastos: null,
  queda: null,
  teDeben: null,
  avisosPendientes: 5,
});

const ids = (negocios: ResumenDeNegocio[]) => negocios.map((item) => item.id);

describe('ordenar', () => {
  it('arranca por lo vendido, de mayor a menor', () => {
    expect(ids(ordenar([KIOSCO, FERIA, ALMACEN], ORDEN_INICIAL))).toEqual([
      'almacen',
      'kiosco',
      'feria',
    ]);
  });

  it('ordena por la métrica elegida en los dos sentidos', () => {
    expect(
      ids(ordenar([ALMACEN, KIOSCO, FERIA], { metrica: 'avisosPendientes', sentido: 'desc' })),
    ).toEqual(['feria', 'almacen', 'kiosco']);
    expect(ids(ordenar([ALMACEN, KIOSCO, FERIA], { metrica: 'queda', sentido: 'asc' }))).toEqual([
      'kiosco',
      'almacen',
      'feria',
    ]);
  });

  it('deja al final lo que no tiene valor, en cualquier sentido', () => {
    expect(ids(ordenar([FERIA, KIOSCO], { metrica: 'vendiste', sentido: 'desc' }))).toEqual([
      'kiosco',
      'feria',
    ]);
    expect(ids(ordenar([FERIA, KIOSCO], { metrica: 'vendiste', sentido: 'asc' }))).toEqual([
      'kiosco',
      'feria',
    ]);
  });

  it('en un empate conserva el orden del servidor y no toca la lista original', () => {
    const lista = [negocio('primero'), negocio('segundo')];

    expect(ids(ordenar(lista, ORDEN_INICIAL))).toEqual(['primero', 'segundo']);
    expect(
      ids(ordenar([FERIA, negocio('otra-sin-datos', { vendiste: null })], ORDEN_INICIAL)),
    ).toEqual(['feria', 'otra-sin-datos']);
    expect(lista[0]?.id).toBe('primero');
  });
});

describe('siguienteOrden', () => {
  it('la misma columna da vuelta el sentido y otra arranca de mayor a menor', () => {
    expect(siguienteOrden(ORDEN_INICIAL, 'vendiste')).toEqual({
      metrica: 'vendiste',
      sentido: 'asc',
    });
    expect(siguienteOrden({ metrica: 'vendiste', sentido: 'asc' }, 'vendiste')).toEqual({
      metrica: 'vendiste',
      sentido: 'desc',
    });
    expect(siguienteOrden({ metrica: 'vendiste', sentido: 'asc' }, 'gastos')).toEqual({
      metrica: 'gastos',
      sentido: 'desc',
    });
  });
});

describe('valorDeMetrica', () => {
  it('muestra los importes en pesos, los avisos como número y lo que falta como raya', () => {
    expect(valorDeMetrica(KIOSCO, 'vendiste')).toBe('$280.000');
    expect(valorDeMetrica(KIOSCO, 'queda')).toBe('-$72.000');
    expect(valorDeMetrica(KIOSCO, 'avisosPendientes')).toBe('2');
    expect(valorDeMetrica(FERIA, 'gastos')).toBe('—');
  });
});

describe('textoDelPeriodo', () => {
  it('dice desde qué día hasta qué día, sin correrse por la zona horaria', () => {
    expect(textoDelPeriodo('2026-10-15', '2026-11-13')).toBe(
      'Del 15 de octubre al 13 de noviembre',
    );
  });
});
