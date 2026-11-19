import { describe, expect, it } from 'vitest';
import { localidadEscrita } from '@/lib/ubicacion';
import {
  cuandoSeArmaElGuardadoDe,
  dadoUnBorradorCompletoConCambios,
  entoncesLaUbicacionSeGuardaEn,
  entoncesNoSeGuardaYSeExplicaQue,
  entoncesSeGuardanSoloEstosCambios,
} from '@/test/borradorDeUbicacion';

describe('Lo que se guarda en cada solapa de la ubicación', () => {
  it('el comercio guarda solo su nombre y si comparte los datos de la zona', () => {
    const borrador = dadoUnBorradorCompletoConCambios({ nombre: 'Kiosco Norte' });

    const resultado = cuandoSeArmaElGuardadoDe('comercio', borrador);

    entoncesSeGuardanSoloEstosCambios(resultado, {
      nombre: 'Kiosco Norte',
      comparteDatosZona: true,
    });
  });

  it('el comercio sin nombre explica que hay que escribirlo', () => {
    const borrador = dadoUnBorradorCompletoConCambios({ nombre: '   ' });

    const resultado = cuandoSeArmaElGuardadoDe('comercio', borrador);

    entoncesNoSeGuardaYSeExplicaQue(
      resultado,
      'Escribí el nombre del comercio (hasta 120 caracteres).',
    );
  });

  it('el rubro guarda solo el rubro elegido', () => {
    const borrador = dadoUnBorradorCompletoConCambios({ rubro: 'ALMACEN' });

    const resultado = cuandoSeArmaElGuardadoDe('rubro', borrador);

    entoncesSeGuardanSoloEstosCambios(resultado, { rubro: 'ALMACEN' });
  });

  it('un rubro que no existe explica que hay que elegir uno', () => {
    const borrador = dadoUnBorradorCompletoConCambios({ rubro: 'NO_EXISTE' });

    const resultado = cuandoSeArmaElGuardadoDe('rubro', borrador);

    entoncesNoSeGuardaYSeExplicaQue(resultado, 'Elegí el rubro de tu comercio.');
  });

  it('la ubicación guarda la dirección elegida con su localidad', () => {
    const borrador = dadoUnBorradorCompletoConCambios({ localidad: 'San Justo' });

    const resultado = cuandoSeArmaElGuardadoDe('ubicacion', borrador);

    entoncesLaUbicacionSeGuardaEn(resultado, 'San Justo');
  });

  it('la ubicación sin dirección elegida explica que hay que buscarla', () => {
    const borrador = dadoUnBorradorCompletoConCambios({ direccion: null, punto: null });

    const resultado = cuandoSeArmaElGuardadoDe('ubicacion', borrador);

    entoncesNoSeGuardaYSeExplicaQue(resultado, 'Buscá y elegí la dirección de tu comercio.');
  });

  it('la ubicación con la localidad sin verificar pide esperar al mapa', () => {
    const borrador = dadoUnBorradorCompletoConCambios({ localidadBloquea: true });

    const resultado = cuandoSeArmaElGuardadoDe('ubicacion', borrador);

    entoncesNoSeGuardaYSeExplicaQue(
      resultado,
      'Esperá a que verifiquemos la localidad con el mapa para guardar.',
    );
  });

  it('la ubicación con la localidad distinta a la del mapa explica la diferencia', () => {
    const borrador = dadoUnBorradorCompletoConCambios({
      localidadBloquea: true,
      errorDeLocalidad: 'La localidad no coincide con la del mapa (Ramos Mejía).',
    });

    const resultado = cuandoSeArmaElGuardadoDe('ubicacion', borrador);

    entoncesNoSeGuardaYSeExplicaQue(
      resultado,
      'La localidad no coincide con la del mapa (Ramos Mejía).',
    );
  });

  it('la ubicación sin localidad explica que hay que buscarla con la localidad al final', () => {
    const borrador = dadoUnBorradorCompletoConCambios({ localidad: '' });

    const resultado = cuandoSeArmaElGuardadoDe('ubicacion', borrador);

    entoncesNoSeGuardaYSeExplicaQue(
      resultado,
      'No pudimos saber la localidad. Buscá la dirección con la localidad al final.',
    );
  });
});

describe('La localidad escrita al final de la dirección', () => {
  it.each([
    ['Varela 1903, San Justo', 'San Justo'],
    ['Varela 1903, Ramos Mejía,  Buenos Aires ', 'Buenos Aires'],
    ['Varela 1903', ''],
  ])('de «%s» sale «%s»', (consulta, localidad) => {
    expect(localidadEscrita(consulta)).toBe(localidad);
  });
});
