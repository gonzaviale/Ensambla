import { beforeEach, describe, expect, it } from 'vitest';
import { estado, reiniciarEstado, sistemasDe } from '@/mocks/db/estado';
import { panelDe } from '@/mocks/db/panel';
import { planesDe } from '@/mocks/db/planes';
import { sembrarDemoCargada } from '@/mocks/db/demoCargada';
import { makeSystem } from '@/lib/sistema/core';
import { hoy } from '@/lib/sistema/formato';

const EMAIL = 'demo@ensambla.local';

const nombresDeLosNegocios = () => sistemasDe(EMAIL).map((sistema) => sistema.name);

const panelDelNegocio = (id: string) => {
  const sistema = sistemasDe(EMAIL).find((item) => item.id === id);
  if (!sistema) throw new Error(`Falta el negocio ${id}`);
  return panelDe(sistema, hoy(), 30);
};

describe('sembrarDemoCargada', () => {
  beforeEach(() => reiniciarEstado());

  it('deja tres negocios, cada uno en su cuaderno, y Premium activo', () => {
    sembrarDemoCargada(EMAIL);

    expect(nombresDeLosNegocios()).toEqual([
      'Almacén de barrio',
      'Kiosco de la estación',
      'Puesto de la feria',
    ]);
    expect(sistemasDe(EMAIL).map((sistema) => sistema.id)).toEqual([
      'demo-almacen',
      'demo-kiosco',
      'demo-feria',
    ]);
    expect(planesDe(EMAIL).suscripcion.codigoPlanActual).toBe('PREMIUM');
  });

  it('cada negocio muestra un caso distinto', () => {
    sembrarDemoCargada(EMAIL);

    expect(panelDelNegocio('demo-almacen').metricas?.queda).toBeGreaterThan(0);
    expect(panelDelNegocio('demo-kiosco').metricas?.queda).toBeLessThan(0);
    expect(panelDelNegocio('demo-feria').hayDatos).toBe(false);
  });

  it('al sembrar de nuevo no duplica los de ejemplo y conserva los negocios propios', () => {
    const propio = makeSystem('Mi verdulería', ['inventory'], false);
    estado.sistemas.set(EMAIL, [propio]);

    sembrarDemoCargada(EMAIL);
    sembrarDemoCargada(EMAIL);

    expect(nombresDeLosNegocios()).toEqual([
      'Almacén de barrio',
      'Kiosco de la estación',
      'Puesto de la feria',
      'Mi verdulería',
    ]);
  });
});
