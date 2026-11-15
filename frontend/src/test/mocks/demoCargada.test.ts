import { beforeEach, describe, expect, it } from 'vitest';
import { estado, reiniciarEstado, sembrarUsuario, sistemasDe } from '@/mocks/db/estado';
import { resumenConjuntoDe } from '@/mocks/db/negocios';
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

  it('arranca con tres negocios, cada uno con sus secciones y un caso distinto', () => {
    const usuario = sembrarUsuario({ email: EMAIL });
    sembrarDemoCargada(EMAIL);

    const resumen = resumenConjuntoDe(usuario, 30);
    expect(resumen.negocios.map((negocio) => negocio.nombre)).toEqual([
      'Almacén de barrio',
      'Kiosco de la estación',
      'Puesto de la feria',
    ]);
    const [almacen, kiosco, feria] = resumen.negocios;
    expect(almacen).toMatchObject({ activo: true, hayDatos: true });
    expect(almacen?.queda).toBeGreaterThan(0);
    expect(kiosco?.hayDatos).toBe(true);
    expect(kiosco?.queda).toBeLessThan(0);
    expect(feria).toMatchObject({ hayDatos: false, vendiste: null });
    expect(sistemasDe(EMAIL, 'demo-kiosco').map((sistema) => sistema.name)).toEqual(['Kiosco']);
  });

  it('al sembrar de nuevo conserva el negocio activo y las secciones propias de cada negocio', () => {
    sembrarDemoCargada(EMAIL);
    const cuenta = estado.negociosDeLaCuenta.get(EMAIL);
    if (!cuenta) throw new Error('Falta la cuenta');
    estado.negociosDeLaCuenta.set(EMAIL, { ...cuenta, activoId: 'demo-kiosco' });
    const propia = makeSystem('Fiados del kiosco', ['sales'], false);
    estado.sistemas.set(`${EMAIL}#demo-kiosco`, [...sistemasDe(EMAIL, 'demo-kiosco'), propia]);

    sembrarDemoCargada(EMAIL);

    expect(estado.negociosDeLaCuenta.get(EMAIL)?.activoId).toBe('demo-kiosco');
    expect(sistemasDe(EMAIL).map((sistema) => sistema.name)).toEqual([
      'Kiosco',
      'Fiados del kiosco',
    ]);
  });
});
