import { describe, it } from 'vitest';
import {
  cuandoSeAjustaParaGuardar,
  cuandoSeCreaDesdeElBorrador,
  dadoUnAlmacenDeEjemploComoBorrador,
  dadoUnAlmacenSinCambios,
  dadoUnBorradorConDosVentasRealesUnaApuntandoAUnEjemplo,
  dadoUnBorradorConGastosUnClienteDeEjemploNuevoYElTelefonoRenombrado,
  dadoUnBorradorConUnaVentaLeidaDeUnaFoto,
  entoncesConservaLaIdentidadDelOriginal,
  entoncesConservaLosRegistrosReales,
  entoncesElBorradorConservaSusGastos,
  entoncesElTelefonoSeLlamaCelular,
  entoncesElVinculoAUnEjemploQuedaVacio,
  entoncesLaPiezaSumadaQuedaSinFilas,
  entoncesLaPropuestaQuedaComoEstaba,
  entoncesLaVentaGuardadaApuntaASuVelaYSuClienta,
  entoncesLaVentaQuedaSinCliente,
  entoncesLosClientesSonLosDelOriginal,
  entoncesNadaEsEjemplo,
  entoncesNingunaPiezaTieneFilas,
  entoncesQuedaIgualAlOriginal,
  entoncesSeLlamaMiSeccionConElMismoId,
} from '@/test/guardarDelTaller';

describe('guardar un sistema ajustado con Sami', () => {
  it('conserva el id, la fecha de creación y el nombre del original si el nuevo queda vacío', () => {
    dadoUnBorradorConGastosUnClienteDeEjemploNuevoYElTelefonoRenombrado();

    cuandoSeAjustaParaGuardar();

    entoncesConservaLaIdentidadDelOriginal();
  });

  it('no guarda los ejemplos que se sumaron en el taller', () => {
    dadoUnBorradorConGastosUnClienteDeEjemploNuevoYElTelefonoRenombrado();

    cuandoSeAjustaParaGuardar();

    entoncesLaPiezaSumadaQuedaSinFilas('expenses');
  });

  it('conserva las filas que el sistema ya tenía', () => {
    dadoUnBorradorConGastosUnClienteDeEjemploNuevoYElTelefonoRenombrado();

    cuandoSeAjustaParaGuardar();

    entoncesLosClientesSonLosDelOriginal();
  });

  it('guarda los cambios en los datos', () => {
    dadoUnBorradorConGastosUnClienteDeEjemploNuevoYElTelefonoRenombrado();

    cuandoSeAjustaParaGuardar();

    entoncesElTelefonoSeLlamaCelular();
  });

  it('un vínculo a un ejemplo que no se guarda queda vacío', () => {
    dadoUnBorradorConGastosUnClienteDeEjemploNuevoYElTelefonoRenombrado();

    cuandoSeAjustaParaGuardar();

    entoncesLaVentaQuedaSinCliente();
  });

  it('el borrador no se toca', () => {
    dadoUnBorradorConGastosUnClienteDeEjemploNuevoYElTelefonoRenombrado();

    cuandoSeAjustaParaGuardar();

    entoncesElBorradorConservaSusGastos();
  });

  it('sin cambios queda igual al original', () => {
    dadoUnAlmacenSinCambios();

    cuandoSeAjustaParaGuardar();

    entoncesQuedaIgualAlOriginal();
  });

  it('guarda los registros reales que Sami cargó de una foto, con sus vínculos', () => {
    dadoUnBorradorConUnaVentaLeidaDeUnaFoto();

    cuandoSeAjustaParaGuardar();

    entoncesLaVentaGuardadaApuntaASuVelaYSuClienta();
  });

  it('lo que se cargó de una foto no se guarda como ejemplo', () => {
    dadoUnBorradorConUnaVentaLeidaDeUnaFoto();

    cuandoSeAjustaParaGuardar();

    entoncesNadaEsEjemplo();
  });
});

describe('crear el sistema que sale del taller', () => {
  it('sin nombre se llama Mi sección y conserva el id de la propuesta', () => {
    dadoUnBorradorConDosVentasRealesUnaApuntandoAUnEjemplo();

    cuandoSeCreaDesdeElBorrador();

    entoncesSeLlamaMiSeccionConElMismoId();
  });

  it('descarta los ejemplos', () => {
    dadoUnBorradorConDosVentasRealesUnaApuntandoAUnEjemplo();

    cuandoSeCreaDesdeElBorrador();

    entoncesNadaEsEjemplo();
  });

  it('conserva los registros reales', () => {
    dadoUnBorradorConDosVentasRealesUnaApuntandoAUnEjemplo();

    cuandoSeCreaDesdeElBorrador();

    entoncesConservaLosRegistrosReales();
  });

  it('vacía los vínculos que apuntaban a un ejemplo', () => {
    dadoUnBorradorConDosVentasRealesUnaApuntandoAUnEjemplo();

    cuandoSeCreaDesdeElBorrador();

    entoncesElVinculoAUnEjemploQuedaVacio();
  });

  it('la propuesta queda como estaba', () => {
    dadoUnBorradorConDosVentasRealesUnaApuntandoAUnEjemplo();

    cuandoSeCreaDesdeElBorrador();

    entoncesLaPropuestaQuedaComoEstaba();
  });

  it('sin registros reales, el sistema arranca vacío', () => {
    dadoUnAlmacenDeEjemploComoBorrador();

    cuandoSeCreaDesdeElBorrador();

    entoncesNingunaPiezaTieneFilas();
  });
});
