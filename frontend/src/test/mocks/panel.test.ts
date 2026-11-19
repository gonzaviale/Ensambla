import { describe, expect, it } from 'vitest';
import { makeTemplate } from '@/lib/sistema/core';
import { crearAlmacenCargado } from '@/lib/sistema/demo';
import { panelDe } from '@/mocks/db/panel';

const HOY = '2026-10-19';
const SEMANAS_DE_TREINTA_DIAS = [
  '2026-09-20',
  '2026-09-27',
  '2026-10-04',
  '2026-10-11',
  '2026-10-18',
];

type Panel = ReturnType<typeof panelDe>;

let panel: Panel;

describe('panel de la home simulado', () => {
  it('mira treinta días hacia atrás desde hoy', () => {
    cuandoSeArmaElPanelDelAlmacenCargadoDe(30);

    entoncesElPeriodoVaDesdeHastaHoy('2026-09-20', 30);
  });

  it('arma las semanas desde el primer día del período', () => {
    cuandoSeArmaElPanelDelAlmacenCargadoDe(30);

    entoncesLasSemanasEmpiezanEnLosDias(SEMANAS_DE_TREINTA_DIAS);
  });

  it('la última semana puede ser más corta y termina hoy', () => {
    cuandoSeArmaElPanelDelAlmacenCargadoDe(30);

    entoncesLaUltimaSemanaTerminaHoy();
  });

  it('lo fiado no es una entrada: se informa aparte y no suma al saldo', () => {
    cuandoSeArmaElPanelDelAlmacenCargadoDe(30);

    entoncesLoFiadoSeInformaAparteYNoSumaAlSaldo();
  });

  it('lo que queda es el saldo con el que cierra el flujo de caja', () => {
    cuandoSeArmaElPanelDelAlmacenCargadoDe(30);

    entoncesLoQueQuedaEsElSaldoFinalDelFlujo();
  });

  it('con menos días mira un período más corto', () => {
    cuandoSeArmaElPanelDelAlmacenCargadoDe(7);

    entoncesElPeriodoVaDesdeHastaHoy('2026-10-13', 7);
    entoncesHayUnaSolaSemana();
  });

  it('sin ventas en el período no hay métricas ni semanas, y las filas de ejemplo no cuentan', () => {
    cuandoSeArmaElPanelDeUnaPlantillaDeEjemplo();

    entoncesNoHayDatosMetricasNiSemanas();
  });
});

function cuandoSeArmaElPanelDelAlmacenCargadoDe(dias: number) {
  panel = panelDe(crearAlmacenCargado(HOY), HOY, dias);
}

function cuandoSeArmaElPanelDeUnaPlantillaDeEjemplo() {
  panel = panelDe(makeTemplate('shop', {}, true), HOY, 30);
}

function entoncesElPeriodoVaDesdeHastaHoy(desde: string, dias: number) {
  expect(panel.dias).toBe(dias);
  expect(panel.desde).toBe(desde);
  expect(panel.hasta).toBe(HOY);
}

function entoncesLasSemanasEmpiezanEnLosDias(dias: string[]) {
  expect(panel.flujoDeCaja.map((semana) => semana.desde)).toEqual(dias);
}

function entoncesLaUltimaSemanaTerminaHoy() {
  expect(panel.flujoDeCaja.at(-1)?.hasta).toBe(HOY);
}

function entoncesLoFiadoSeInformaAparteYNoSumaAlSaldo() {
  const metricas = panel.metricas;
  if (!metricas) throw new Error('Debería haber métricas');
  const entradas = panel.flujoDeCaja.reduce((total, semana) => total + semana.entradas, 0);
  expect(metricas.teDeben).toBeGreaterThan(0);
  expect(entradas).toBeCloseTo(metricas.vendiste - metricas.teDeben, 2);
}

function entoncesLoQueQuedaEsElSaldoFinalDelFlujo() {
  expect(panel.metricas?.queda).toBeCloseTo(panel.flujoDeCaja.at(-1)?.saldoAcumulado ?? NaN, 2);
}

function entoncesHayUnaSolaSemana() {
  expect(panel.flujoDeCaja).toHaveLength(1);
}

function entoncesNoHayDatosMetricasNiSemanas() {
  expect(panel.hayDatos).toBe(false);
  expect(panel.metricas).toBeNull();
  expect(panel.flujoDeCaja).toEqual([]);
}
