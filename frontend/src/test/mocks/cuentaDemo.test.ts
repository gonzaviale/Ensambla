import { describe, expect, it } from 'vitest';
import { prepararCuentaDemo } from '@/mocks/db/demo';
import { estado } from '@/mocks/db/estado';
import { ZONA_DE_PRUEBA } from '@/mocks/db/ubicacion';
import { crearAlmacenDeEjemplo } from '@/lib/sistema/demo';
import { hoy } from '@/lib/sistema/formato';

const DEMO = 'demo@ensambla.local';

describe('cuenta de demostración del modo simulado', () => {
  it('arranca con el almacén de ejemplo, el comercio en la zona de prueba y Premium a prueba', () => {
    const ahora = new Date('2026-10-28T12:00:00Z');

    prepararCuentaDemo(DEMO, ahora);

    expect(estado.sistemas.get(DEMO)?.map((sistema) => sistema.name)).toEqual(['Almacén de ejemplo']);
    expect(estado.negocios.get(DEMO)).toMatchObject({ rubro: 'ALMACEN', zona: ZONA_DE_PRUEBA.nombre });
    expect(estado.negocios.get(DEMO)?.ubicacion).not.toBeNull();
    expect(estado.pruebasPremium.get(DEMO)).toMatchObject({
      inicioEn: '2026-10-28T12:00:00.000Z',
      finEn: '2026-11-04T12:00:00.000Z',
    });
  });

  it('no pisa lo que ya se cambió en la demo', () => {
    const propio = { ...crearAlmacenDeEjemplo(hoy()), name: 'Mi kiosco' };
    estado.sistemas.set(DEMO, [propio]);
    estado.negocios.set(DEMO, {
      nombre: 'Kiosco',
      rubro: 'KIOSCO',
      ubicacion: null,
      comparteDatosZona: false,
      zona: null,
    });

    prepararCuentaDemo(DEMO);

    expect(estado.sistemas.get(DEMO)).toEqual([propio]);
    expect(estado.negocios.get(DEMO)?.nombre).toBe('Kiosco');
  });

  it('avisa que cambió algo para que el navegador lo conserve', () => {
    let guardados = 0;
    estado.alCambiar = () => {
      guardados++;
    };

    prepararCuentaDemo(DEMO);

    expect(guardados).toBe(1);
  });
});
