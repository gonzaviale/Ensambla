import { describe, it, vi } from 'vitest';
import type * as ConsejosDeFoto from '@/lib/sistema/consejosDeFoto';
import {
  cuandoSamiArmaLaPropuesta,
  cuandoSeAbreElChatMinimizado,
  cuandoSeAbreElPrimerRegistroDeEjemplo,
  cuandoSeAbreElTallerDeSami,
  cuandoSeAnchaLaPiezaDeClientesAFilaCompleta,
  cuandoSeArmaElSistemaACrear,
  cuandoSeArrastraLaSegundaPiezaSobreLaPrimera,
  cuandoSeBorranLosEjemplosConfirmando,
  cuandoSeCreaLaSeccionConElNombre,
  cuandoSeEditaElPrimerRegistroDeEjemplo,
  cuandoSeEligeTomarFoto,
  cuandoSeElijeLaDistribucionDeUnaColumna,
  cuandoSeEscribeUnMensajeMuyCorto,
  cuandoSeIntentaRegistrarUnProductoSinDatos,
  cuandoSeMinimizaElChat,
  cuandoSePideSumarUnProveedor,
  cuandoSePulsaEscape,
  cuandoSeQuitaLaPiezaDePedidos,
  cuandoSeRegistraUnProductoPropio,
  cuandoSeRenombraLaPiezaDeClientes,
  cuandoSeSumaLaPiezaDePedidosDesdeLaPaleta,
  cuandoSeTocaLaCamara,
  cuandoSeVuelveAEntrarAlTaller,
  dadoQueElSelectorDeFotoSeEspia,
  dadoQueNoSeQuierenVerLosConsejosDeLaFoto,
  dadoQueSamiArmoLaPropuesta,
  dadoQueSeAbreElTallerDeSami,
  dadoQueSeAbrioElPopUpDeLosConsejos,
  dadoQueSeAgregoUnRegistroPropio,
  dadoQueSeEditoElPrimerRegistroDeEjemplo,
  dadoQueSeMinimizoElChat,
  dadoQueSePidioQuitarLosEjemplos,
  dadoQueSeSumoLaPiezaDePedidosDesdeLaPaleta,
  dadoQueSeVieronLasFilasDeProductos,
  dadoQueSeVieronLasPiezasEnOrden,
  entoncesElChatQuedaMinimizadoYElFocoVaAlBotonParaAbrirlo,
  entoncesElFormularioNoOfreceEliminarElRegistroDeEjemplo,
  entoncesElRegistroEditadoDejaDeSerDeEjemplo,
  entoncesElRegistroPropioApareceEnLaPieza,
  entoncesEmpiezaConElSaludoLosEjemplosYElLienzoVacio,
  entoncesLaConversacionEmpiezaDeNuevo,
  entoncesLaPrimeraPiezaPasaALaSegundaPosicion,
  entoncesLaPiezaDeClientesNoSePuedeQuitar,
  entoncesLaPiezaOcupaLaFilaCompleta,
  entoncesLaPiezaSeLlamaComoLaPersonaQuiso,
  entoncesLaPiezaYaNoEstaEnLaPropuesta,
  entoncesLaPropuestaTieneUnaPiezaMas,
  entoncesLaPropuestaYaTraeLasCuatroSeccionesQueEnsamblaNecesita,
  entoncesLaSeccionSeGuardaSinEjemplosYSeAbreLista,
  entoncesLosRegistrosEditadosYPropiosPasanAlSistema,
  entoncesNoSeGuardaYElFormularioSigueAbierto,
  entoncesPideUnPocoMasYNoArmaNingunaPropuesta,
  entoncesSamiArmaUnaBaseQueSeVeALaDerecha,
  entoncesSamiSumaElProveedorALaPropuesta,
  entoncesSeAbreElSelectorUnaVezYSeCierraElPopUp,
  entoncesSeCierraElPopUpSinAbrirElSelector,
  entoncesSeMuestranLosConsejosSinAbrirElSelector,
  entoncesSeSacaronLasFilasDeEjemploYDesapareceElBoton,
  entoncesSeVuelveALaConversacionSinPerderla,
  entoncesSigueHabiendoEjemplosYSePideConfirmar,
  entoncesTodasLasPiezasOcupanLaFilaCompleta,
} from '@/test/conversacionDelTaller';

// Por defecto vale lo real (siempre mostrar); un test lo cambia para probar el otro camino.
vi.mock('@/lib/sistema/consejosDeFoto', async (original) => {
  const real = await original<typeof ConsejosDeFoto>();
  return { ...real, debeMostrarConsejosDeFoto: vi.fn(real.debeMostrarConsejosDeFoto) };
});

describe('el taller de Sami', () => {
  it('empieza con el saludo, los ejemplos y el lienzo vacío', async () => {
    await cuandoSeAbreElTallerDeSami();

    entoncesEmpiezaConElSaludoLosEjemplosYElLienzoVacio();
  });

  it('la cámara abre un pop up con los consejos sin abrir todavía el selector', async () => {
    await dadoQueSeAbreElTallerDeSami();
    dadoQueElSelectorDeFotoSeEspia();

    await cuandoSeTocaLaCamara();

    await entoncesSeMuestranLosConsejosSinAbrirElSelector();
  });

  it('«Tomar foto» abre el selector y cierra el pop up', async () => {
    await dadoQueSeAbreElTallerDeSami();
    dadoQueElSelectorDeFotoSeEspia();
    await dadoQueSeAbrioElPopUpDeLosConsejos();

    await cuandoSeEligeTomarFoto();

    entoncesSeAbreElSelectorUnaVezYSeCierraElPopUp();
  });

  it('el pop up de la foto se cierra con Escape sin abrir el selector', async () => {
    await dadoQueSeAbreElTallerDeSami();
    dadoQueElSelectorDeFotoSeEspia();
    await dadoQueSeAbrioElPopUpDeLosConsejos();

    await cuandoSePulsaEscape();

    entoncesSeCierraElPopUpSinAbrirElSelector();
  });

  it('si se decide no mostrar los consejos, la cámara abre el selector directo', async () => {
    await dadoQueSeAbreElTallerDeSami();
    dadoQueElSelectorDeFotoSeEspia();
    dadoQueNoSeQuierenVerLosConsejosDeLaFoto();

    await cuandoSeTocaLaCamara();

    entoncesSeAbreElSelectorUnaVezYSeCierraElPopUp();
  });

  it('el chat flota: se minimiza a un botón', async () => {
    await dadoQueSeAbreElTallerDeSami();

    await cuandoSeMinimizaElChat();

    await entoncesElChatQuedaMinimizadoYElFocoVaAlBotonParaAbrirlo();
  });

  it('el chat minimizado se vuelve a abrir sin perder la conversación', async () => {
    await dadoQueSeAbreElTallerDeSami();
    await dadoQueSeMinimizoElChat();

    await cuandoSeAbreElChatMinimizado();

    await entoncesSeVuelveALaConversacionSinPerderla();
  });

  it('pide un poco más cuando lo escrito es muy corto', async () => {
    await dadoQueSeAbreElTallerDeSami();

    await cuandoSeEscribeUnMensajeMuyCorto();

    await entoncesPideUnPocoMasYNoArmaNingunaPropuesta();
  });

  it('con un ejemplo Sami arma una base que se ve a la derecha', async () => {
    await cuandoSamiArmaLaPropuesta();

    await entoncesSamiArmaUnaBaseQueSeVeALaDerecha();
  });

  it('lo que se escribe después ajusta la propuesta', async () => {
    await dadoQueSamiArmoLaPropuesta();

    await cuandoSePideSumarUnProveedor();

    await entoncesSamiSumaElProveedorALaPropuesta();
  });

  it('lo que Sami arma desde cero ya trae las cuatro secciones que Ensambla necesita', async () => {
    await cuandoSamiArmaLaPropuesta();

    entoncesLaPropuestaYaTraeLasCuatroSeccionesQueEnsamblaNecesita();
  });

  it('se puede cambiar el nombre de una pieza', async () => {
    await dadoQueSamiArmoLaPropuesta();

    await cuandoSeRenombraLaPiezaDeClientes();

    await entoncesLaPiezaSeLlamaComoLaPersonaQuiso();
  });

  it('se puede cambiar el ancho de una pieza', async () => {
    await dadoQueSamiArmoLaPropuesta();

    await cuandoSeAnchaLaPiezaDeClientesAFilaCompleta();

    entoncesLaPiezaOcupaLaFilaCompleta();
  });

  it('se puede quitar de la propuesta una pieza que Ensambla no necesita', async () => {
    await dadoQueSamiArmoLaPropuesta();
    await dadoQueSeSumoLaPiezaDePedidosDesdeLaPaleta();

    await cuandoSeQuitaLaPiezaDePedidos();

    entoncesLaPiezaYaNoEstaEnLaPropuesta();
  });

  it('no se puede quitar de la propuesta una pieza que Ensambla necesita', async () => {
    await dadoQueSamiArmoLaPropuesta();

    entoncesLaPiezaDeClientesNoSePuedeQuitar();
  });

  it('el formulario de un registro de ejemplo no ofrece eliminarlo', async () => {
    await dadoQueSamiArmoLaPropuesta();

    await cuandoSeAbreElPrimerRegistroDeEjemplo();

    await entoncesElFormularioNoOfreceEliminarElRegistroDeEjemplo();
  });

  it('un registro de ejemplo editado deja de ser de ejemplo', async () => {
    await dadoQueSamiArmoLaPropuesta();

    await cuandoSeEditaElPrimerRegistroDeEjemplo();

    await entoncesElRegistroEditadoDejaDeSerDeEjemplo();
  });

  it('se puede agregar un registro propio a una pieza', async () => {
    await dadoQueSamiArmoLaPropuesta();

    await cuandoSeRegistraUnProductoPropio();

    await entoncesElRegistroPropioApareceEnLaPieza();
  });

  it('al crear el sistema pasan el registro editado y el propio', async () => {
    await dadoQueSamiArmoLaPropuesta();
    await dadoQueSeEditoElPrimerRegistroDeEjemplo();
    await dadoQueSeAgregoUnRegistroPropio();

    cuandoSeArmaElSistemaACrear();

    entoncesLosRegistrosEditadosYPropiosPasanAlSistema();
  });

  it('un registro sin los datos obligatorios no se guarda y el formulario sigue abierto', async () => {
    await dadoQueSamiArmoLaPropuesta();
    dadoQueSeVieronLasFilasDeProductos();

    await cuandoSeIntentaRegistrarUnProductoSinDatos();

    await entoncesNoSeGuardaYElFormularioSigueAbierto();
  });

  it('suma una pieza de la paleta', async () => {
    await dadoQueSamiArmoLaPropuesta();

    await cuandoSeSumaLaPiezaDePedidosDesdeLaPaleta();

    entoncesLaPropuestaTieneUnaPiezaMas();
  });

  it('cambia la distribución de las piezas', async () => {
    await dadoQueSamiArmoLaPropuesta();

    await cuandoSeElijeLaDistribucionDeUnaColumna();

    entoncesTodasLasPiezasOcupanLaFilaCompleta();
  });

  it('arrastrar una pieza sobre otra la reubica', async () => {
    await dadoQueSamiArmoLaPropuesta();
    dadoQueSeVieronLasPiezasEnOrden();

    cuandoSeArrastraLaSegundaPiezaSobreLaPrimera();

    await entoncesLaPrimeraPiezaPasaALaSegundaPosicion();
  });

  it('crear el sistema lo guarda sin los ejemplos y lo abre listo para usar', async () => {
    await dadoQueSamiArmoLaPropuesta();

    await cuandoSeCreaLaSeccionConElNombre('Almacén Don Luis');

    await entoncesLaSeccionSeGuardaSinEjemplosYSeAbreLista('Almacén Don Luis');
  });

  it('«Borrar ejemplos» pide confirmación antes de sacar las filas', async () => {
    await dadoQueSamiArmoLaPropuesta();

    await dadoQueSePidioQuitarLosEjemplos();

    await entoncesSigueHabiendoEjemplosYSePideConfirmar();
  });

  it('confirmar «Borrar ejemplos» saca las filas de ejemplo y el botón desaparece', async () => {
    await dadoQueSamiArmoLaPropuesta();

    await cuandoSeBorranLosEjemplosConfirmando();

    await entoncesSeSacaronLasFilasDeEjemploYDesapareceElBoton();
  });

  it('volver a entrar empieza una conversación nueva', async () => {
    await dadoQueSamiArmoLaPropuesta();

    await cuandoSeVuelveAEntrarAlTaller();

    entoncesLaConversacionEmpiezaDeNuevo();
  });
});
