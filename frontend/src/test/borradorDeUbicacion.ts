import { expect } from 'vitest';
import { cambiosDeLaSolapa, type BorradorDeUbicacion, type CambiosDeSolapa } from '@/lib/ubicacion';
import { DIRECCIONES_DE_PRUEBA, RUBROS_DE_PRUEBA } from '@/mocks/db/ubicacion';
import type { SeccionUbicacion } from '@/types/ubicacion';

const DIRECCION = DIRECCIONES_DE_PRUEBA[0];

const borradorCompleto: BorradorDeUbicacion = {
  nombre: 'Almacén Doña Rosa',
  comparteDatosZona: true,
  rubro: RUBROS_DE_PRUEBA[0]?.codigo ?? '',
  rubrosDisponibles: RUBROS_DE_PRUEBA,
  direccion: DIRECCION ?? null,
  punto: DIRECCION ? { latitud: DIRECCION.latitud, longitud: DIRECCION.longitud } : null,
  localidad: 'San Justo',
  localidadBloquea: false,
  errorDeLocalidad: '',
};

export const dadoUnBorradorCompletoConCambios = (
  cambios: Partial<BorradorDeUbicacion> = {},
): BorradorDeUbicacion => ({ ...borradorCompleto, ...cambios });

export const cuandoSeArmaElGuardadoDe = (
  seccion: SeccionUbicacion,
  borrador: BorradorDeUbicacion,
): CambiosDeSolapa => cambiosDeLaSolapa(seccion, borrador);

export const entoncesSeGuardanSoloEstosCambios = (
  resultado: CambiosDeSolapa,
  cambios: CambiosDeSolapa['cambios'],
) => expect(resultado).toEqual({ cambios });

export const entoncesNoSeGuardaYSeExplicaQue = (resultado: CambiosDeSolapa, error: string) => {
  expect(resultado.cambios).toBeUndefined();
  expect(resultado.error).toBe(error);
};

export const entoncesLaUbicacionSeGuardaEn = (resultado: CambiosDeSolapa, localidad: string) =>
  expect(resultado.cambios?.ubicacion).toMatchObject({
    direccion: DIRECCION?.direccion,
    localidad,
  });
