import { beforeEach, describe, expect, it } from 'vitest';
import { estado, reiniciarEstado, sistemasDe } from '@/mocks/db/estado';
import { planesDe } from '@/mocks/db/planes';
import { sembrarDemoCargada } from '@/mocks/db/demoCargada';
import { makeSystem } from '@/lib/sistema/core';

const EMAIL = 'demo@ensambla.local';

describe('sembrarDemoCargada', () => {
  beforeEach(() => reiniciarEstado());

  it('deja el almacén cargado y Premium activo', () => {
    sembrarDemoCargada(EMAIL);

    const sistemas = sistemasDe(EMAIL);
    expect(sistemas).toHaveLength(1);
    expect(sistemas[0]?.name).toBe('Almacén de barrio');
    expect(planesDe(EMAIL).suscripcion.codigoPlanActual).toBe('PREMIUM');
  });

  it('al sembrar de nuevo no duplica el almacén y conserva las secciones propias', () => {
    const propio = makeSystem('Mi sección', ['inventory'], false);
    estado.sistemas.set(EMAIL, [propio]);

    sembrarDemoCargada(EMAIL);
    sembrarDemoCargada(EMAIL);

    expect(sistemasDe(EMAIL).map((sistema) => sistema.name)).toEqual([
      'Almacén de barrio',
      'Mi sección',
    ]);
  });
});
