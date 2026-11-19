import { describe, expect, it } from 'vitest';
import {
  errorDeConsulta,
  leerPunto,
  normalizarConsulta,
  revisarConfiguracion,
} from '@/lib/ubicacion';
import { DIRECCIONES_DE_PRUEBA, RUBROS_DE_PRUEBA } from '@/mocks/db/ubicacion';
import { cuandoSeCalcula, entoncesElResultadoEs } from '@/test/calculo';

const direccion = DIRECCIONES_DE_PRUEBA[0];

type Revision = ReturnType<typeof revisarConfiguracion>;

let revision: Revision;
let revisionCorregida: Revision;

describe('Validación de la ubicación del comercio', () => {
  it('normaliza los espacios sin cambiar la dirección ni la localidad', () => {
    cuandoSeCalcula(() => normalizarConsulta('  Florencio  Varela 1903, San Justo  '));

    entoncesElResultadoEs('Florencio Varela 1903, San Justo');
  });

  it.each(['   ', '1234', 'a'.repeat(201)])(
    'rechaza la consulta demasiado corta o larga %s',
    (consulta) => {
      cuandoSeCalcula(() => errorDeConsulta(consulta) !== '');

      entoncesElResultadoEs(true);
    },
  );

  it.each(['12345', 'a'.repeat(200)])('admite los límites de la consulta %s', (consulta) => {
    cuandoSeCalcula(() => errorDeConsulta(consulta));

    entoncesElResultadoEs('');
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
    cuandoSeCalcula(() => leerPunto(latitud, longitud));

    entoncesElResultadoEs(null);
  });

  it.each([
    ['0', '0', { latitud: 0, longitud: 0 }],
    ['-90', '180', { latitud: -90, longitud: 180 }],
    ['90', '-180', { latitud: 90, longitud: -180 }],
    [' -34,67 ', '-58,56', { latitud: -34.67, longitud: -58.56 }],
  ])('admite el punto %s / %s (cero, límites o coma decimal)', (latitud, longitud, punto) => {
    cuandoSeCalcula(() => leerPunto(latitud, longitud));

    entoncesElResultadoEs(punto);
  });

  it('exige una dirección elegida antes de revisar', () => {
    cuandoSeRevisaSinDireccion();

    entoncesElErrorPideElegirLaDireccion();
  });

  it('exige coordenadas válidas antes de revisar', () => {
    cuandoSeRevisaConLatitudFueraDeRango();

    entoncesNoHayConfiguracion();
  });

  it('exige un código incluido en el catálogo actual', () => {
    cuandoSeRevisaConUnRubroInventado();

    entoncesNoHayConfiguracion();
  });

  it('exige que el catálogo de rubros no esté vacío', () => {
    cuandoSeRevisaConUnCatalogoVacio();

    entoncesNoHayConfiguracion();
  });

  it('conserva la dirección de referencia cuando el punto no se tocó', () => {
    cuandoSeRevisanElPuntoOriginalYUnoCorregido();

    entoncesElPuntoOriginalNoQuedaAjustado();
  });

  it('distingue un punto corregido manualmente', () => {
    cuandoSeRevisanElPuntoOriginalYUnoCorregido();

    entoncesElPuntoCorregidoQuedaAjustadoConSuRubro();
  });
});

function cuandoSeRevisaSinDireccion() {
  revision = revisarConfiguracion(null, '0', '0', 'ALMACEN', RUBROS_DE_PRUEBA);
}

function cuandoSeRevisaConLatitudFueraDeRango() {
  revision = revisarConfiguracion(direccion, '91', '0', 'ALMACEN', RUBROS_DE_PRUEBA);
}

function cuandoSeRevisaConUnRubroInventado() {
  revision = revisarConfiguracion(direccion, '0', '0', 'INVENTADO', RUBROS_DE_PRUEBA);
}

function cuandoSeRevisaConUnCatalogoVacio() {
  revision = revisarConfiguracion(direccion, '0', '0', 'ALMACEN', []);
}

function cuandoSeRevisanElPuntoOriginalYUnoCorregido() {
  revision = revisarConfiguracion(
    direccion,
    String(direccion.latitud),
    String(direccion.longitud),
    'KIOSCO',
    RUBROS_DE_PRUEBA,
  );
  revisionCorregida = revisarConfiguracion(
    direccion,
    '-34.68',
    '-58.57',
    'ALMACEN',
    RUBROS_DE_PRUEBA,
  );
}

function entoncesElErrorPideElegirLaDireccion() {
  expect(revision.error).toContain('elegí la dirección');
}

function entoncesNoHayConfiguracion() {
  expect(revision.configuracion).toBeNull();
}

function entoncesElPuntoOriginalNoQuedaAjustado() {
  expect(revision.error).toBe('');
  expect(revision.configuracion?.ajustado).toBe(false);
}

function entoncesElPuntoCorregidoQuedaAjustadoConSuRubro() {
  expect(revisionCorregida.configuracion).toEqual({
    direccion: direccion.direccion,
    punto: { latitud: -34.68, longitud: -58.57 },
    rubro: RUBROS_DE_PRUEBA[0],
    ajustado: true,
  });
}
