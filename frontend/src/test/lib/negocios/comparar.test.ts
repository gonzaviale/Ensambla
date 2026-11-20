import { describe, it } from 'vitest';
import {
  ORDEN_INICIAL,
  ordenar,
  siguienteOrden,
  textoDelPeriodo,
  valorDeMetrica,
} from '@/lib/negocios/comparar';
import { cuandoSeCalcula, entoncesElResultadoEs } from '@/test/calculo';
import type { ResumenDeNegocio } from '@/types/negocios';

const negocio = (id: string, datos: Partial<ResumenDeNegocio> = {}): ResumenDeNegocio => ({
  id,
  nombre: id,
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
    cuandoSeCalcula(() => ids(ordenar([KIOSCO, FERIA, ALMACEN], ORDEN_INICIAL)));

    entoncesElResultadoEs(['almacen', 'kiosco', 'feria']);
  });

  it('ordena por la métrica elegida de menor a mayor', () => {
    cuandoSeCalcula(() =>
      ids(ordenar([ALMACEN, KIOSCO, FERIA], { metrica: 'queda', sentido: 'asc' })),
    );

    entoncesElResultadoEs(['kiosco', 'almacen', 'feria']);
  });

  it('deja al final lo que no tiene valor de mayor a menor', () => {
    cuandoSeCalcula(() => ids(ordenar([FERIA, KIOSCO], { metrica: 'vendiste', sentido: 'desc' })));

    entoncesElResultadoEs(['kiosco', 'feria']);
  });

  it('deja al final lo que no tiene valor de menor a mayor', () => {
    cuandoSeCalcula(() => ids(ordenar([FERIA, KIOSCO], { metrica: 'vendiste', sentido: 'asc' })));

    entoncesElResultadoEs(['kiosco', 'feria']);
  });

  it('en un empate conserva el orden del servidor', () => {
    cuandoSeCalcula(() => ids(ordenar([negocio('primero'), negocio('segundo')], ORDEN_INICIAL)));

    entoncesElResultadoEs(['primero', 'segundo']);
  });

  it('entre dos sin valor conserva el orden del servidor', () => {
    const otraSinDatos = negocio('otra-sin-datos', { vendiste: null });

    cuandoSeCalcula(() => ids(ordenar([FERIA, otraSinDatos], ORDEN_INICIAL)));

    entoncesElResultadoEs(['feria', 'otra-sin-datos']);
  });

  it('no toca la lista original', () => {
    const lista = [negocio('segundo', { vendiste: 1 }), negocio('primero', { vendiste: 9 })];

    cuandoSeCalcula(() => {
      ordenar(lista, ORDEN_INICIAL);
      return ids(lista);
    });

    entoncesElResultadoEs(['segundo', 'primero']);
  });
});

describe('siguienteOrden', () => {
  it('la misma columna da vuelta el sentido', () => {
    cuandoSeCalcula(() => siguienteOrden(ORDEN_INICIAL, 'vendiste'));

    entoncesElResultadoEs({ metrica: 'vendiste', sentido: 'asc' });
  });

  it('la misma columna en ascendente vuelve a descendente', () => {
    cuandoSeCalcula(() => siguienteOrden({ metrica: 'vendiste', sentido: 'asc' }, 'vendiste'));

    entoncesElResultadoEs({ metrica: 'vendiste', sentido: 'desc' });
  });

  it('otra columna arranca de mayor a menor', () => {
    cuandoSeCalcula(() => siguienteOrden({ metrica: 'vendiste', sentido: 'asc' }, 'gastos'));

    entoncesElResultadoEs({ metrica: 'gastos', sentido: 'desc' });
  });
});

describe('valorDeMetrica', () => {
  it('muestra los importes en pesos', () => {
    cuandoSeCalcula(() => valorDeMetrica(KIOSCO, 'vendiste'));

    entoncesElResultadoEs('$280.000');
  });

  it('muestra los importes negativos con el signo adelante', () => {
    cuandoSeCalcula(() => valorDeMetrica(KIOSCO, 'queda'));

    entoncesElResultadoEs('-$72.000');
  });

  it('muestra lo que falta como raya', () => {
    cuandoSeCalcula(() => valorDeMetrica(FERIA, 'gastos'));

    entoncesElResultadoEs('—');
  });
});

describe('textoDelPeriodo', () => {
  it('dice desde qué día hasta qué día, sin correrse por la zona horaria', () => {
    cuandoSeCalcula(() => textoDelPeriodo('2026-10-15', '2026-11-13'));

    entoncesElResultadoEs('Del 15 de octubre al 13 de noviembre');
  });
});
