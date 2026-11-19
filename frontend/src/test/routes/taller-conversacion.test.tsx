import { describe, it } from 'vitest';
import {
  cuandoSeAbreElTallerParaConversar,
  cuandoSeDeshaceLoUltimo,
  cuandoSeEligeElEjemplo,
  cuandoSeEligeLaRespuestaRapida,
  cuandoSeEscribe,
  cuandoSeEscribeUnMensajeVago,
  dadoQueElServidorRechazaElPedido,
  dadoQueSamiArmaClientasYEncargosConectados,
  dadoQueSamiArmaVentasYClientesYPreguntaSiLlevaStock,
  dadoQueSamiArmoLaPropuestaDeUnAlmacen,
  dadoQueSamiLlamaAlSistemaVelasDeAna,
  dadoQueSamiNoPuedeArmarNadaNiDecirNada,
  dadoQueSamiPreguntaSiSumaGastos,
  dadoQueSamiPreguntaSiVendeProductosOSoloServicios,
  dadoQueSamiSoloPiensa,
  dadoQueSamiSumaLosGastos,
  dadoQueSamiSumaTurnosSinDecirNada,
  dadoQueSeAbreElTallerParaConversar,
  dadoQueSeAplicoUnPedidoDeSami,
  dadoQueSeEscribioQueTieneUnaPeluqueria,
  dadoQueSeEscribioUnMensajeVago,
  entoncesElAjusteVaConElSistemaResumidoYLaCharlaReciente,
  entoncesElInventarioNoTieneProveedor,
  entoncesElNombreQueSamiPusoQuedaEnElCampo,
  entoncesElPrimerMensajeVaConUnSistemaVacioYLoQueEnsamblaSeVe,
  entoncesLaBotoneraDesapareceYSeEnfocaElCampo,
  entoncesLaCharlaCuentaLoQueSamiHizo,
  entoncesLaPreguntaDeLaBotoneraViajaEnLaCharla,
  entoncesLaPropuestaNoCambiaNiOfreceDeshacer,
  entoncesLaRespuestaCortaSeMandaTalCual,
  entoncesLosPedidosQuedanConectadosConLasVelasYSusClientas,
  entoncesSamiArmaElSistemaDesdeCeroYDejaUnaPregunta,
  entoncesSamiConfirmaQueVolvioAtrasYYaNoOfreceDeshacer,
  entoncesSamiCuentaQueNoPudoYSigueSinPropuesta,
  entoncesSamiEnsamblaLasPiezasDeLasVelas,
  entoncesSamiPreguntaElRubroConUnaBotoneraQueDejaEscribirOtraCosa,
  entoncesSamiRenombraElSistema,
  entoncesSamiSacaLosClientes,
  entoncesSamiSumaElProveedorAlInventario,
  entoncesSamiSumaLosGastosATravesDeLaOrden,
  entoncesSamiSumaUnaTablaDeTurnosAMedida,
  entoncesSeAvisaConElMensajeDelServidorYSeSigueEscribiendo,
  entoncesSeAvisaYSigueSinPropuesta,
  entoncesSigueTePidiendoMasDetalle,
} from '@/test/charlaConSami';

describe('la conversación de Sami en el taller', () => {
  it('el primer mensaje va a Sami con un sistema vacío y lo que ensambla se ve a la derecha', async () => {
    dadoQueSamiArmaClientasYEncargosConectados();
    await dadoQueSeAbreElTallerParaConversar();

    await cuandoSeEscribe('Tengo un almacén y vendo productos');

    await entoncesElPrimerMensajeVaConUnSistemaVacioYLoQueEnsamblaSeVe();
  });

  it('con una descripción vaga Sami pregunta el rubro con una botonera que siempre deja escribir otra cosa', async () => {
    await dadoQueSeAbreElTallerParaConversar();

    await cuandoSeEscribeUnMensajeVago();

    await entoncesSamiPreguntaElRubroConUnaBotoneraQueDejaEscribirOtraCosa();
  });

  it('elegir una opción es como escribirla: Sami ensambla las piezas de ese negocio', async () => {
    await dadoQueSeAbreElTallerParaConversar();
    await dadoQueSeEscribioUnMensajeVago();

    await cuandoSeEligeLaRespuestaRapida(/Hago velas artesanales/);

    await entoncesSamiEnsamblaLasPiezasDeLasVelas();
  });

  it('con el ejemplo de las velas, los pedidos quedan conectados con las velas y sus clientas', async () => {
    await dadoQueSeAbreElTallerParaConversar();

    await cuandoSeEligeElEjemplo(/Velas artesanales/);

    await entoncesLosPedidosQuedanConectadosConLasVelasYSusClientas();
  });

  it('con el ejemplo de la peluquería, Sami suma una tabla de turnos a medida', async () => {
    await dadoQueSeAbreElTallerParaConversar();

    await cuandoSeEligeElEjemplo(/Una peluquería/);

    await entoncesSamiSumaUnaTablaDeTurnosAMedida();
  });

  it('Sami puede armar el sistema desde cero con herramientas y dejar una pregunta', async () => {
    dadoQueSamiArmaVentasYClientesYPreguntaSiLlevaStock();
    await dadoQueSeAbreElTallerParaConversar();

    await cuandoSeEscribeUnMensajeVago();

    await entoncesSamiArmaElSistemaDesdeCeroYDejaUnaPregunta();
  });

  it('si Sami armó algo sin decir nada, la charla cuenta lo que hizo', async () => {
    dadoQueSamiSumaTurnosSinDecirNada();
    await dadoQueSeAbreElTallerParaConversar();

    await cuandoSeEscribe('Doy turnos para cortes');

    await entoncesLaCharlaCuentaLoQueSamiHizo();
  });

  it('si Sami no armó nada ni dijo nada, lo cuenta y sigue sin propuesta', async () => {
    dadoQueSamiNoPuedeArmarNadaNiDecirNada();
    await dadoQueSeAbreElTallerParaConversar();

    await cuandoSeEscribe('Doy turnos para cortes');

    await entoncesSamiCuentaQueNoPudoYSigueSinPropuesta();
  });

  it('«Escribir otra cosa…» deja la botonera y enfoca el campo de texto', async () => {
    await dadoQueSeAbreElTallerParaConversar();
    await dadoQueSeEscribioUnMensajeVago();

    await cuandoSeEligeLaRespuestaRapida(/Escribir otra cosa/);

    entoncesLaBotoneraDesapareceYSeEnfocaElCampo();
  });

  it('una respuesta rápida corta como «Sí» se manda tal cual', async () => {
    dadoQueSamiPreguntaSiSumaGastos();
    await dadoQueSeAbreElTallerParaConversar();
    await dadoQueSeEscribioUnMensajeVago();

    await cuandoSeEligeLaRespuestaRapida(/^Sí/);

    await entoncesLaRespuestaCortaSeMandaTalCual();
  });

  it('al elegir una respuesta rápida, la pregunta de la botonera viaja en la charla aunque Sami no la haya escrito', async () => {
    dadoQueSamiPreguntaSiVendeProductosOSoloServicios();
    await dadoQueSeAbreElTallerParaConversar();
    await dadoQueSeEscribioQueTieneUnaPeluqueria();

    await cuandoSeEligeLaRespuestaRapida(/^Sólo servicios/);

    await entoncesLaPreguntaDeLaBotoneraViajaEnLaCharla();
  });

  it('cada ajuste va a Sami con el sistema resumido y la charla reciente', async () => {
    await dadoQueSamiArmoLaPropuestaDeUnAlmacen();
    dadoQueSamiSoloPiensa();

    await cuandoSeEscribe('¿por qué me pusiste esas piezas?');

    await entoncesElAjusteVaConElSistemaResumidoYLaCharlaReciente();
  });

  it('una orden simple como «sumá gastos» también la resuelve Sami', async () => {
    await dadoQueSamiArmoLaPropuestaDeUnAlmacen();
    dadoQueSamiSumaLosGastos();

    await cuandoSeEscribe('sumá gastos');

    await entoncesSamiSumaLosGastosATravesDeLaOrden();
  });

  it('sacar una pieza se le pide a Sami', async () => {
    await dadoQueSamiArmoLaPropuestaDeUnAlmacen();

    await cuandoSeEscribe('sacá los clientes');

    await entoncesSamiSacaLosClientes();
  });

  it('renombrar el sistema también se le pide a Sami', async () => {
    await dadoQueSamiArmoLaPropuestaDeUnAlmacen();

    await cuandoSeEscribe('llamalo Almacén Don Luis');

    await entoncesSamiRenombraElSistema();
  });

  it('lo que Sami pide con herramientas se aplica a la propuesta', async () => {
    await dadoQueSamiArmoLaPropuestaDeUnAlmacen();

    await cuandoSeEscribe('agregá un proveedor al inventario');

    await entoncesSamiSumaElProveedorAlInventario();
  });

  it('lo que Sami aplicó con herramientas se puede deshacer', async () => {
    await dadoQueSamiArmoLaPropuestaDeUnAlmacen();
    await dadoQueSeAplicoUnPedidoDeSami();

    await cuandoSeDeshaceLoUltimo();

    entoncesElInventarioNoTieneProveedor();
    entoncesSamiConfirmaQueVolvioAtrasYYaNoOfreceDeshacer();
  });

  it('si Sami sólo conversa, no cambia la propuesta ni ofrece deshacer', async () => {
    await dadoQueSamiArmoLaPropuestaDeUnAlmacen();

    await cuandoSeEscribe('gracias, Sami');

    await entoncesLaPropuestaNoCambiaNiOfreceDeshacer();
  });

  it('si el servidor rechaza el pedido avisa con su mensaje y deja seguir escribiendo', async () => {
    await dadoQueSamiArmoLaPropuestaDeUnAlmacen();
    dadoQueElServidorRechazaElPedido();

    await cuandoSeEscribe('¿por qué me pusiste esas piezas?');

    await entoncesSeAvisaConElMensajeDelServidorYSeSigueEscribiendo();
  });

  it('si el primer mensaje falla, avisa y sigue sin propuesta', async () => {
    dadoQueElServidorRechazaElPedido();
    await dadoQueSeAbreElTallerParaConversar();

    await cuandoSeEscribe('Tengo un almacén y vendo productos');

    await entoncesSeAvisaYSigueSinPropuesta();
  });

  it('Sami puede renombrar el sistema sin pisar el nombre que la persona escribió mientras tanto', async () => {
    await dadoQueSamiArmoLaPropuestaDeUnAlmacen();
    dadoQueSamiLlamaAlSistemaVelasDeAna();

    await cuandoSeEscribe('ponele un nombre lindo');

    await entoncesElNombreQueSamiPusoQuedaEnElCampo();
  });

  it('un mensaje de una sola letra sigue pidiendo más detalle', async () => {
    await dadoQueSeAbreElTallerParaConversar();

    await cuandoSeEscribe('a');

    await entoncesSigueTePidiendoMasDetalle();
  });
});
