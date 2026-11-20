import { expect } from 'vitest';
import { FILTROS_INICIALES, type Aporte, type Filtros } from '@/lib/dondeAbrir';
import { factoresSinMirar, formatoDato, fuentesDeLosDatos } from '@/lib/dondeAbrirDetalle';

const OSM = { codigo: 'osm', nombre: 'OpenStreetMap', url: 'https://osm.org', licencia: 'ODbL' };
const CENSO = { codigo: 'censo', nombre: 'Censo', url: null, licencia: null };

const aporteDe = (clave: Aporte['clave'], fuente?: Aporte['fuente']): Aporte => ({
  clave,
  etiqueta: clave,
  peso: 0.1,
  puntaje: 5,
  aporte: 0.5,
  fuente,
});

export const dadoUnAporteConFuente = (
  clave: Aporte['clave'],
  fuente: 'osm' | 'censo' | 'ninguna',
) => aporteDe(clave, { osm: OSM, censo: CENSO, ninguna: undefined }[fuente]);

export const dadoQueNoLeImportaNiLaCompetenciaNiLasEscuelas = (): Filtros => ({
  ...FILTROS_INICIALES,
  importancia: { ...FILTROS_INICIALES.importancia, competencia: 0, escuelas: 0 },
});

export const cuandoSeFormateaElDato = formatoDato;
export const cuandoSeJuntanLasFuentes = fuentesDeLosDatos;
export const cuandoSeBuscanLosFactoresSinMirar = factoresSinMirar;

export const entoncesSeMuestra = (texto: string, esperado: string) =>
  expect(texto.replace(/\s+/g, ' ')).toBe(esperado);

export const entoncesLasFuentesSon = (fuentes: { codigo: string }[], codigos: string[]) =>
  expect(fuentes.map((fuente) => fuente.codigo)).toEqual(codigos);

export const entoncesLosFactoresSinMirarSon = (factores: string[], esperados: string[]) =>
  expect(factores).toEqual(esperados);
