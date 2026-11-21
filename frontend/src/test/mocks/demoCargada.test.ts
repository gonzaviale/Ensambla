import { beforeEach, describe, expect, it } from 'vitest';
import { estado, reiniciarEstado, sistemasDe } from '@/mocks/db/estado';
import { panelDe } from '@/mocks/db/panel';
import { planesDe } from '@/mocks/db/planes';
import { sembrarDemoCargada } from '@/mocks/db/demoCargada';
import { makeSystem } from '@/lib/sistema/core';
import { hoy } from '@/lib/sistema/formato';

const EMAIL = 'demo@ensambla.local';
const NEGOCIOS_DE_EJEMPLO = ['Almacén de barrio', 'Kiosco de la estación', 'Puesto de la feria'];

describe('sembrarDemoCargada', () => {
  beforeEach(() => reiniciarEstado());

  it('deja tres negocios, cada uno en su cuaderno', () => {
    cuandoSeSiembraLaDemo();

    entoncesLosNegociosSon(NEGOCIOS_DE_EJEMPLO);
    entoncesLosCuadernosSon('demo-almacen', 'demo-kiosco', 'demo-feria');
  });

  it('deja Premium activo', () => {
    cuandoSeSiembraLaDemo();

    entoncesElPlanActualEs('PREMIUM');
  });

  it('el almacén muestra un caso con saldo a favor', () => {
    cuandoSeSiembraLaDemo();

    entoncesElSaldoDelNegocioEsPositivo('demo-almacen');
  });

  it('el kiosco muestra un caso con saldo en contra', () => {
    cuandoSeSiembraLaDemo();

    entoncesElSaldoDelNegocioEsNegativo('demo-kiosco');
  });

  it('la feria muestra un caso sin datos', () => {
    cuandoSeSiembraLaDemo();

    entoncesElNegocioNoTieneDatos('demo-feria');
  });

  it('al sembrar de nuevo no duplica los de ejemplo y conserva los negocios propios', () => {
    dadoUnNegocioPropio('Mi verdulería');

    cuandoSeSiembraLaDemoDosVeces();

    entoncesLosNegociosSon([...NEGOCIOS_DE_EJEMPLO, 'Mi verdulería']);
  });
});

function dadoUnNegocioPropio(nombre: string) {
  estado.sistemas.set(EMAIL, [makeSystem(nombre, ['inventory'], false)]);
}

function cuandoSeSiembraLaDemo() {
  sembrarDemoCargada(EMAIL);
}

function cuandoSeSiembraLaDemoDosVeces() {
  sembrarDemoCargada(EMAIL);
  sembrarDemoCargada(EMAIL);
}

const panelDelNegocio = (id: string) => {
  const sistema = sistemasDe(EMAIL).find((item) => item.id === id);
  if (!sistema) throw new Error(`Falta el negocio ${id}`);
  return panelDe(sistema, hoy(), 30);
};

function entoncesLosNegociosSon(nombres: string[]) {
  expect(sistemasDe(EMAIL).map((sistema) => sistema.name)).toEqual(nombres);
}

function entoncesLosCuadernosSon(...ids: string[]) {
  expect(sistemasDe(EMAIL).map((sistema) => sistema.id)).toEqual(ids);
}

function entoncesElPlanActualEs(codigo: string) {
  expect(planesDe(EMAIL).suscripcion.codigoPlanActual).toBe(codigo);
}

function entoncesElSaldoDelNegocioEsPositivo(id: string) {
  expect(panelDelNegocio(id).metricas?.queda).toBeGreaterThan(0);
}

function entoncesElSaldoDelNegocioEsNegativo(id: string) {
  expect(panelDelNegocio(id).metricas?.queda).toBeLessThan(0);
}

function entoncesElNegocioNoTieneDatos(id: string) {
  expect(panelDelNegocio(id).hayDatos).toBe(false);
}
