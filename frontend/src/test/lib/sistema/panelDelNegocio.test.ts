import { describe, it } from 'vitest';
import { makeTemplate } from '@/lib/sistema/core';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import {
  bloqueDeVentas,
  pesos,
  pesosConSigno,
  porcentaje,
  sistemaDelPanel,
  tieneVentas,
} from '@/lib/sistema/panelDelNegocio';
import {
  cuandoSeCalcula,
  entoncesElResultadoEs,
  entoncesElResultadoEsIdenticoA,
  entoncesElResultadoNoExiste,
} from '@/test/calculo';

const HOY = '2026-10-19';

describe('sistemaDelPanel', () => {
  it('sin secciones no hay panel', () => {
    cuandoSeCalcula(() => sistemaDelPanel([]));

    entoncesElResultadoEs(null);
  });

  it('elige la primera sección con ventas aunque no sea la primera de la lista', () => {
    const vacio = makeTemplate('shop', {}, true);
    const cargado = crearAlmacenCargado(HOY);

    cuandoSeCalcula(() => sistemaDelPanel([vacio, cargado]));

    entoncesElResultadoEsIdenticoA(cargado);
  });

  it('si ninguno tiene ventas se queda con el primero, para explicar cómo cargarlas', () => {
    const primero = makeTemplate('shop', {}, true);
    const segundo = makeTemplate('shop', {}, true);

    cuandoSeCalcula(() => sistemaDelPanel([primero, segundo]));

    entoncesElResultadoEsIdenticoA(primero);
  });

  it('las filas de ejemplo no son ventas', () => {
    cuandoSeCalcula(() => tieneVentas(makeTemplate('shop', {}, true)));

    entoncesElResultadoEs(false);
  });

  it('las filas reales sí son ventas', () => {
    cuandoSeCalcula(() => tieneVentas(crearAlmacenCargado(HOY)));

    entoncesElResultadoEs(true);
  });
});

describe('bloqueDeVentas', () => {
  it('es la tabla de ventas de la sección', () => {
    cuandoSeCalcula(() => bloqueDeVentas(crearAlmacenCargado(HOY))?.type);

    entoncesElResultadoEs('sales');
  });

  it('no hay tabla de ventas si la sección no tiene una', () => {
    cuandoSeCalcula(() => bloqueDeVentas(makeTemplate('business', {}, true)));

    entoncesElResultadoNoExiste();
  });
});

describe('formato del panel', () => {
  it('los pesos llevan el punto de los miles', () => {
    cuandoSeCalcula(() => pesos(839750));

    entoncesElResultadoEs('$839.750');
  });

  it('el cero se muestra sin decimales', () => {
    cuandoSeCalcula(() => pesos(0));

    entoncesElResultadoEs('$0');
  });

  it('los pesos negativos llevan el signo menos delante del peso', () => {
    cuandoSeCalcula(() => pesos(-12000.4));

    entoncesElResultadoEs('-$12.000');
  });

  it('lo que sobra lleva más', () => {
    cuandoSeCalcula(() => pesosConSigno(110000));

    entoncesElResultadoEs('+$110.000');
  });

  it('lo que falta lleva menos', () => {
    cuandoSeCalcula(() => pesosConSigno(-2000));

    entoncesElResultadoEs('-$2.000');
  });

  it('el cero no lleva signo', () => {
    cuandoSeCalcula(() => pesosConSigno(0));

    entoncesElResultadoEs('$0');
  });

  it('el porcentaje usa la coma decimal de es-AR, con un solo decimal', () => {
    cuandoSeCalcula(() => porcentaje(25.9));

    entoncesElResultadoEs('25,9 %');
  });

  it('el porcentaje redondo no lleva decimales', () => {
    cuandoSeCalcula(() => porcentaje(20));

    entoncesElResultadoEs('20 %');
  });

  it('el porcentaje redondea a un decimal', () => {
    cuandoSeCalcula(() => porcentaje(20.64));

    entoncesElResultadoEs('20,6 %');
  });
});
