import { describe, it } from 'vitest';
import {
  cuandoSeBuscanLosFactoresSinMirar,
  cuandoSeFormateaElDato,
  cuandoSeJuntanLasFuentes,
  dadoQueNoLeImportaNiLaCompetenciaNiLasEscuelas,
  dadoUnAporteConFuente,
  entoncesLasFuentesSon,
  entoncesLosFactoresSinMirarSon,
  entoncesSeMuestra,
} from '@/test/detalleDeZona';

describe('El dato observado de cada factor', () => {
  it('sin valor avisa que no hay dato suficiente', () => {
    const texto = cuandoSeFormateaElDato(undefined, 'comercios_500m');

    entoncesSeMuestra(texto, 'Sin dato suficiente');
  });

  it.each([
    [72, 'indice_0_100', '72 de 100'],
    [11, 'comercios_500m', '11 comercios a 500 m'],
    [3, 'lugares_500m', '3 lugares a 500 m'],
    [22400, 'habitantes_km2', '22.400 personas por km²'],
  ])('%s con unidad %s se muestra como «%s»', (valor, unidad, esperado) => {
    const texto = cuandoSeFormateaElDato(valor, unidad);

    entoncesSeMuestra(texto, esperado);
  });

  it('el alquiler mensual se muestra en pesos', () => {
    const texto = cuandoSeFormateaElDato(350000, 'ARS_mensuales');

    entoncesSeMuestra(texto, '$ 350.000');
  });

  it('una unidad que no conocemos muestra solo el número', () => {
    const texto = cuandoSeFormateaElDato(1500, 'otra_unidad');

    entoncesSeMuestra(texto, '1.500');
  });

  it('sin unidad muestra solo el número', () => {
    const texto = cuandoSeFormateaElDato(8, undefined);

    entoncesSeMuestra(texto, '8');
  });
});

describe('Las fuentes de los datos usados', () => {
  it('cada fuente aparece una sola vez aunque la usen varios factores', () => {
    const aportes = [
      dadoUnAporteConFuente('escuelas', 'osm'),
      dadoUnAporteConFuente('parques', 'osm'),
      dadoUnAporteConFuente('vecinos', 'censo'),
    ];

    const fuentes = cuandoSeJuntanLasFuentes(aportes);

    entoncesLasFuentesSon(fuentes, ['osm', 'censo']);
  });

  it('los factores sin fuente no aportan ninguna', () => {
    const aportes = [dadoUnAporteConFuente('poder', 'ninguna')];

    const fuentes = cuandoSeJuntanLasFuentes(aportes);

    entoncesLasFuentesSon(fuentes, []);
  });
});

describe('Lo que la persona dijo que no le importa', () => {
  it('se nombra en minúscula para escribirlo en una frase', () => {
    const filtros = dadoQueNoLeImportaNiLaCompetenciaNiLasEscuelas();

    const factores = cuandoSeBuscanLosFactoresSinMirar(filtros);

    entoncesLosFactoresSinMirarSon(factores, ['poca competencia', 'escuelas cerca']);
  });
});
