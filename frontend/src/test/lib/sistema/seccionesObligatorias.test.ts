import { describe, it } from 'vitest';
import {
  cuandoSeCompletanLasSeccionesObligatorias,
  dadoUnSistemaDeSamiConSoloUnaTablaDeStock,
  dadoUnSistemaDeSamiSinPestanas,
  dadoUnSistemaDeSamiSinPiezas,
  entoncesCadaSeccionTraeSusColumnasFijas,
  entoncesLaTablaDeStockSigueSiendoUnaSola,
  entoncesLasSeccionesSumadasNoTienenFilas,
  entoncesLasVentasSeConectanConElStockYConLosClientes,
  entoncesNoSumoNingunaSeccion,
  entoncesTieneUnaSeccionDeCadaTipoObligatorio,
} from '@/test/seccionesObligatorias';

describe('completar las secciones obligatorias de lo que arma Sami desde cero', () => {
  it('a un sistema sin piezas le suma una sección de cada tipo que Ensambla necesita', () => {
    dadoUnSistemaDeSamiSinPiezas();

    cuandoSeCompletanLasSeccionesObligatorias();

    entoncesTieneUnaSeccionDeCadaTipoObligatorio();
  });

  it('no repite una sección que ya está', () => {
    dadoUnSistemaDeSamiConSoloUnaTablaDeStock();

    cuandoSeCompletanLasSeccionesObligatorias();

    entoncesLaTablaDeStockSigueSiendoUnaSola();
  });

  it('cada sección sumada trae sus columnas fijas', () => {
    dadoUnSistemaDeSamiSinPiezas();

    cuandoSeCompletanLasSeccionesObligatorias();

    entoncesCadaSeccionTraeSusColumnasFijas();
  });

  it('las secciones sumadas empiezan sin filas', () => {
    dadoUnSistemaDeSamiSinPiezas();

    cuandoSeCompletanLasSeccionesObligatorias();

    entoncesLasSeccionesSumadasNoTienenFilas();
  });

  it('las ventas sumadas quedan conectadas con el stock y con los clientes', () => {
    dadoUnSistemaDeSamiSinPiezas();

    cuandoSeCompletanLasSeccionesObligatorias();

    entoncesLasVentasSeConectanConElStockYConLosClientes();
  });

  it('sin pestañas no hay dónde sumarlas y no cambia nada', () => {
    dadoUnSistemaDeSamiSinPestanas();

    cuandoSeCompletanLasSeccionesObligatorias();

    entoncesNoSumoNingunaSeccion();
  });
});
