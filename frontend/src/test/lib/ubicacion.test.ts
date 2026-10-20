import { describe, expect, it } from 'vitest';
import {
  errorDeConsulta,
  leerPunto,
  normalizarConsulta,
  revisarConfiguracion,
} from '@/lib/ubicacion';
import { DIRECCIONES_DE_PRUEBA, RUBROS_DE_PRUEBA } from '@/mocks/db/ubicacion';

const direccion = DIRECCIONES_DE_PRUEBA[0];

describe('Validación de la ubicación del comercio', () => {
  it('normaliza los espacios sin cambiar la dirección ni la localidad', () => {
    expect(normalizarConsulta('  Florencio  Varela 1903, San Justo  ')).toBe(
      'Florencio Varela 1903, San Justo',
    );
  });

  it('rechaza consultas demasiado cortas o largas y admite sus límites', () => {
    expect(errorDeConsulta('   ')).not.toBe('');
    expect(errorDeConsulta('1234')).not.toBe('');
    expect(errorDeConsulta('a'.repeat(201))).not.toBe('');
    expect(errorDeConsulta('12345')).toBe('');
    expect(errorDeConsulta('a'.repeat(200))).toBe('');
  });

  it.each([
    ['', '0'],
    ['0', ''],
    [' ', '0'],
    ['NaN', '0'],
    ['Infinity', '0'],
    ['91', '0'],
    ['-91', '0'],
    ['0', '181'],
    ['0', '-181'],
    ['1,2,3', '0'],
  ])('rechaza el punto inválido %s / %s', (latitud, longitud) => {
    expect(leerPunto(latitud, longitud)).toBeNull();
  });

  it('admite el cero, los límites geográficos y la coma decimal', () => {
    expect(leerPunto('0', '0')).toEqual({ latitud: 0, longitud: 0 });
    expect(leerPunto('-90', '180')).toEqual({ latitud: -90, longitud: 180 });
    expect(leerPunto('90', '-180')).toEqual({ latitud: 90, longitud: -180 });
    expect(leerPunto(' -34,67 ', '-58,56')).toEqual({ latitud: -34.67, longitud: -58.56 });
  });

  it('exige una dirección elegida antes de revisar', () => {
    expect(revisarConfiguracion(null, '0', '0', 'ALMACEN', RUBROS_DE_PRUEBA).error).toContain(
      'elegí la dirección',
    );
  });

  it('exige coordenadas válidas antes de revisar', () => {
    expect(
      revisarConfiguracion(direccion, '91', '0', 'ALMACEN', RUBROS_DE_PRUEBA).configuracion,
    ).toBeNull();
  });

  it('exige un código incluido en el catálogo actual', () => {
    expect(
      revisarConfiguracion(direccion, '0', '0', 'INVENTADO', RUBROS_DE_PRUEBA).configuracion,
    ).toBeNull();
    expect(revisarConfiguracion(direccion, '0', '0', 'ALMACEN', []).configuracion).toBeNull();
  });

  it('conserva la dirección de referencia y distingue un punto corregido manualmente', () => {
    const original = revisarConfiguracion(
      direccion,
      String(direccion.latitud),
      String(direccion.longitud),
      'KIOSCO',
      RUBROS_DE_PRUEBA,
    );
    const corregida = revisarConfiguracion(
      direccion,
      '-34.68',
      '-58.57',
      'ALMACEN',
      RUBROS_DE_PRUEBA,
    );
    expect(original.error).toBe('');
    expect(original.configuracion?.ajustado).toBe(false);
    expect(corregida.configuracion).toEqual({
      direccion: direccion.direccion,
      punto: { latitud: -34.68, longitud: -58.57 },
      rubro: RUBROS_DE_PRUEBA[0],
      ajustado: true,
    });
  });
});
