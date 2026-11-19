import { afterEach, describe, it } from 'vitest';
import {
  cuandoSeAbreLaFotoEnGrande,
  cuandoSeAdjuntaLaFotoConLaCamara,
  cuandoSeAdjuntaUnaFotoDeMasDe5MB,
  cuandoSeAdjuntaUnaFotoPngConElTextoDeLasVentas,
  cuandoSeAjustaConLaFotoYSeSaltanLasDudas,
  cuandoSeCierraLaFotoConEscape,
  cuandoSeCierraLaFotoConLaX,
  cuandoSeCierraLaFotoTocandoAfuera,
  cuandoSeCreaLaSeccionConLoLeido,
  cuandoSeDeshaceLoUltimo,
  cuandoSeDejanSinVincular,
  cuandoSeEnviaElMensajeSinTexto,
  cuandoSeEscribeGraciasYSeEnvia,
  cuandoSeGuardanLosCambiosDelSistema,
  cuandoSePideAjustarElPrecio,
  cuandoSeQuitaLaFoto,
  cuandoSeSaltanLasDudas,
  cuandoSeSumanLosRegistrosLeidos,
  dadoQueElTallerDeSamiSeEspia,
  dadoQueSamiLeyoLaFotoDeLaPropuesta,
  dadoQueSamiLeyoLaFotoDelAlmacenDeAna,
  dadoQueSamiLeyoLaFotoYSeSalteoronLasDudas,
  dadoQueSeAbreElTallerParaMandarFotos,
  dadoQueSeAbrioLaFotoEnGrande,
  dadoQueSeAdjuntoLaFoto,
  dadoQueSamiLeyoLaFotoDeUnAlmacenDeAna,
  dadoQueSeMandoLaFoto,
  dadoQueSeSumaronLosRegistrosLeidos,
  dejarDeEspiarLosPedidosQueRecibeSami,
  entoncesAvisaQueLaFotoPesaDemasiadoYNoLaAdjunta,
  entoncesElHistorialSoloLlevaElTextoDeLaFoto,
  entoncesElPedidoLlevaLaFotoPngYElTexto,
  entoncesElSistemaCreadoTieneLosRegistrosLeidos,
  entoncesElSistemaGuardadoTieneLaFotoLeida,
  entoncesElStockVuelveAComoEstaba,
  entoncesLaCharlaMuestraLaFotoYYaNoQuedaAdjunta,
  entoncesLaFotoSeAbreEnUnDialogoConElFocoEnCerrar,
  entoncesLaFotoSeVeEnMiniaturaYSePuedeCambiar,
  entoncesLaFotoSigueAdjunta,
  entoncesLaFotoViajaConElMensajeAutomatico,
  entoncesLaFotoYaNoEstaAdjunta,
  entoncesLaVentaDescuentaElStockYSamiAvisa,
  entoncesLasVentasQuedanSinVincularYNoSeCreaNada,
  entoncesLasVentasUsanLosRegistrosQueYaEstan,
  entoncesRecienAhoraApareceLaTarjetaDeLosVinculosNuevos,
  entoncesSamiCorrigeElPrecioSinVolverACargarlo,
  entoncesSamiRecibeLosNombresQueYaExisten,
  entoncesSamiSugiereLosRegistrosQueNoEncontro,
  entoncesSeAvisaQueElStockNoAlcanzo,
  entoncesSeCierraElDialogoDeLaFoto,
  entoncesSeCierraElDialogoYElFocoVuelveALaMiniatura,
  entoncesSoloPreguntaPorLosRegistrosQueFaltan,
  entoncesSoloSeMuestranLasDudasDeAUna,
} from '@/test/fotoDelTaller';
import { dadoQueSamiArmoUnaPropuestaDeAlmacen } from '@/test/taller';

afterEach(dejarDeEspiarLosPedidosQueRecibeSami);

describe('una foto para Sami en el taller', () => {
  it('se adjunta con la cámara y se ve en miniatura', async () => {
    await dadoQueSeAbreElTallerParaMandarFotos();

    await cuandoSeAdjuntaLaFotoConLaCamara();

    await entoncesLaFotoSeVeEnMiniaturaYSePuedeCambiar();
  });

  it('se puede quitar antes de mandarla', async () => {
    await dadoQueSeAbreElTallerParaMandarFotos();
    await dadoQueSeAdjuntoLaFoto();

    await cuandoSeQuitaLaFoto();

    entoncesLaFotoYaNoEstaAdjunta();
  });

  it('viaja sola con el próximo mensaje', async () => {
    dadoQueElTallerDeSamiSeEspia();
    await dadoQueSeAbreElTallerParaMandarFotos();
    await dadoQueSeAdjuntoLaFoto();

    await cuandoSeEnviaElMensajeSinTexto();

    await entoncesLaFotoViajaConElMensajeAutomatico();
    await entoncesLaCharlaMuestraLaFotoYYaNoQuedaAdjunta();
  });

  it('no queda en el historial que se le manda a Sami: sólo el texto', async () => {
    dadoQueElTallerDeSamiSeEspia();
    await dadoQueSeAbreElTallerParaMandarFotos();
    await dadoQueSeMandoLaFoto();

    await cuandoSeEscribeGraciasYSeEnvia();

    await entoncesElHistorialSoloLlevaElTextoDeLaFoto();
  });

  it('la miniatura se expande en un diálogo con el foco en la X', async () => {
    await dadoQueSeAbreElTallerParaMandarFotos();
    await dadoQueSeAdjuntoLaFoto();

    await cuandoSeAbreLaFotoEnGrande();

    await entoncesLaFotoSeAbreEnUnDialogoConElFocoEnCerrar();
  });

  it('el diálogo de la foto se cierra con la X y el foco vuelve a la miniatura', async () => {
    await dadoQueSeAbreElTallerParaMandarFotos();
    await dadoQueSeAbrioLaFotoEnGrande();

    await cuandoSeCierraLaFotoConLaX();

    entoncesSeCierraElDialogoYElFocoVuelveALaMiniatura();
  });

  it('el diálogo de la foto se cierra con Escape', async () => {
    await dadoQueSeAbreElTallerParaMandarFotos();
    await dadoQueSeAbrioLaFotoEnGrande();

    await cuandoSeCierraLaFotoConEscape();

    entoncesSeCierraElDialogoDeLaFoto();
  });

  it('el diálogo de la foto se cierra tocando afuera y la foto sigue adjunta', async () => {
    await dadoQueSeAbreElTallerParaMandarFotos();
    await dadoQueSeAbrioLaFotoEnGrande();

    await cuandoSeCierraLaFotoTocandoAfuera();

    entoncesSeCierraElDialogoDeLaFoto();
    entoncesLaFotoSigueAdjunta();
  });

  it('con texto, la foto viaja con lo que se escribió', async () => {
    dadoQueElTallerDeSamiSeEspia();
    await dadoQueSeAbreElTallerParaMandarFotos();

    await cuandoSeAdjuntaUnaFotoPngConElTextoDeLasVentas();

    await entoncesElPedidoLlevaLaFotoPngYElTexto();
  });

  it('si la foto pesa más de 5 MB aun achicada, avisa y no la adjunta', async () => {
    dadoQueElTallerDeSamiSeEspia();
    await dadoQueSeAbreElTallerParaMandarFotos();

    await cuandoSeAdjuntaUnaFotoDeMasDe5MB();

    await entoncesAvisaQueLaFotoPesaDemasiadoYNoLaAdjunta();
  });

  it('con el Sami simulado, lo leído de la foto sugiere los registros que no están', async () => {
    await dadoQueSamiArmoUnaPropuestaDeAlmacen();

    await cuandoSeAjustaConLaFotoYSeSaltanLasDudas();

    await entoncesSamiSugiereLosRegistrosQueNoEncontro();
  });

  it('al sumar los registros leídos se avisa si el stock no alcanzó', async () => {
    await dadoQueSamiLeyoLaFotoYSeSalteoronLasDudas();

    await cuandoSeSumanLosRegistrosLeidos();

    await entoncesSeAvisaQueElStockNoAlcanzo();
  });

  it('al crear el sistema, lo leído de la foto queda como registros reales', async () => {
    await dadoQueSeSumaronLosRegistrosLeidos();

    await cuandoSeCreaLaSeccionConLoLeido();

    await entoncesElSistemaCreadoTieneLosRegistrosLeidos();
  });

  it('Sami corrige un registro que leyó de la foto sin volver a cargarlo', async () => {
    await dadoQueSamiLeyoLaFotoYSeSalteoronLasDudas();

    await cuandoSePideAjustarElPrecio();

    await entoncesSamiCorrigeElPrecioSinVolverACargarlo();
  });

  it('al ajustar un sistema, Sami recibe los nombres que ya existen para usarlos tal cual', async () => {
    await dadoQueSamiLeyoLaFotoDelAlmacenDeAna();

    entoncesSamiRecibeLosNombresQueYaExisten();
  });

  it('al ajustar un sistema, sólo se pregunta por los registros que faltan', async () => {
    await dadoQueSamiLeyoLaFotoDelAlmacenDeAna();

    entoncesSoloPreguntaPorLosRegistrosQueFaltan();
  });

  it('al ajustar un sistema, las ventas usan la clienta y el producto que ya están', async () => {
    await dadoQueSamiLeyoLaFotoDelAlmacenDeAna();

    entoncesLasVentasUsanLosRegistrosQueYaEstan();
  });

  it('la venta descuenta el stock en la propuesta y Sami avisa si no alcanzó', async () => {
    await dadoQueSamiLeyoLaFotoDelAlmacenDeAna();

    entoncesLaVentaDescuentaElStockYSamiAvisa();
  });

  it('deshacer lo último devuelve el stock a como estaba', async () => {
    await dadoQueSamiLeyoLaFotoDelAlmacenDeAna();

    await cuandoSeDeshaceLoUltimo();

    entoncesElStockVuelveAComoEstaba();
  });

  it('si la persona no quiere sumarlos, las ventas quedan sin vincular y no se crea nada', async () => {
    await dadoQueSamiLeyoLaFotoYSeSalteoronLasDudas();

    await cuandoSeDejanSinVincular();

    entoncesLasVentasQuedanSinVincularYNoSeCreaNada();
  });

  it('al ajustar un sistema con Sami, la foto también va y los registros leídos se guardan', async () => {
    await dadoQueSamiLeyoLaFotoDeUnAlmacenDeAna();

    await cuandoSeGuardanLosCambiosDelSistema();

    await entoncesElSistemaGuardadoTieneLaFotoLeida();
  });

  it('suelta un aviso por vez: primero las dudas de a una', async () => {
    await dadoQueSamiLeyoLaFotoDeLaPropuesta();

    entoncesSoloSeMuestranLasDudasDeAUna();
  });

  it('recién después de las dudas aparece la tarjeta de los vínculos nuevos', async () => {
    await dadoQueSamiLeyoLaFotoDeLaPropuesta();

    await cuandoSeSaltanLasDudas();

    entoncesRecienAhoraApareceLaTarjetaDeLosVinculosNuevos();
  });
});
