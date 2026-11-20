import { describe, it } from 'vitest';
import {
  MEDIOS_DE_PAGO,
  MODOS_DE_VENDER,
  PESTANAS_DE_CAJA,
  cuandoSeLeenLosTonosDe,
  cuandoSeLeenLosTonosDeLasPosiciones,
  cuandoSeLeenLosTonosDeTodosLosConceptos,
  entoncesLosTonosSon,
  entoncesNingunTonoSeRepite,
  entoncesTodosLosTonosSonDeLaMarca,
} from '@/test/tonosDeColor';

describe('tonoDe', () => {
  it('pinta cada concepto con uno de los cuatro colores sólidos de la marca', () => {
    cuandoSeLeenLosTonosDeTodosLosConceptos();

    entoncesTodosLosTonosSonDeLaMarca();
  });

  it('distingue los tres medios de pago con colores distintos', () => {
    cuandoSeLeenLosTonosDe(MEDIOS_DE_PAGO);

    entoncesNingunTonoSeRepite();
  });

  it('distingue las cuatro pestañas de Caja con colores distintos', () => {
    cuandoSeLeenLosTonosDe(PESTANAS_DE_CAJA);

    entoncesNingunTonoSeRepite();
  });

  it('distingue los tres modos de Vender con colores distintos', () => {
    cuandoSeLeenLosTonosDe(MODOS_DE_VENDER);

    entoncesNingunTonoSeRepite();
  });
});

describe('tonoPorPosicion', () => {
  it('rota celeste, naranja, coral y petróleo desde el primer cliente', () => {
    cuandoSeLeenLosTonosDeLasPosiciones(0, 3);

    entoncesLosTonosSon(['celeste', 'naranja', 'coral', 'petroleo']);
  });

  it('vuelve a empezar la rotación en el quinto cliente', () => {
    cuandoSeLeenLosTonosDeLasPosiciones(4, 7);

    entoncesLosTonosSon(['celeste', 'naranja', 'coral', 'petroleo']);
  });
});
