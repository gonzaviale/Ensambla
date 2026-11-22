package com.ensambla.backend.domain.useCase.sami.conversarEnTaller;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.model.sami.taller.AccionTaller;
import com.ensambla.backend.domain.model.sami.taller.CampoTaller;
import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.ImagenTaller;
import com.ensambla.backend.domain.model.sami.taller.LlamadaHerramienta;
import com.ensambla.backend.domain.model.sami.taller.MensajeTaller;
import com.ensambla.backend.domain.model.sami.taller.PiezaTaller;
import com.ensambla.backend.domain.model.sami.taller.RegistroTaller;
import com.ensambla.backend.domain.model.sami.taller.RespuestaTaller;
import com.ensambla.backend.domain.model.sami.taller.ResultadoTallerIa;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.TallerIaPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.support.SamiPruebas;
import com.ensambla.backend.support.TallerDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversarEnTallerTest {

    private static final UUID NEGOCIO_ID = TallerDePrueba.NEGOCIO_ID.valor();
    private static final int LIMITE = 60;
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");
    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final ConsumoIaSami CONSUMO = new ConsumoIaSami("bedrock", "haiku", 800, 90);
    private static final EstadoTaller ESTADO = new EstadoTaller("Mi almacén", "mosaic", List.of(
            new PiezaTaller("e1", "inventory", "Inventario", null, List.of())));
    private static final EstadoTaller ESTADO_CON_DATOS = new EstadoTaller("Mi almacén", "mosaic", List.of(
            new PiezaTaller("e1", "inventory", "Inventario", null, List.of(new CampoTaller("Producto", "text"))),
            new PiezaTaller("e2", "clients", "Clientes", null, List.of())));
    private static final EstadoTaller ESTADO_CON_REGISTROS = new EstadoTaller("Mi almacén", "mosaic", List.of(
            new PiezaTaller("e1", "inventory", "Inventario", null, List.of(new CampoTaller("Producto", "text")),
                    List.of(new RegistroTaller("r1", Map.of("Producto", "Yerba")),
                            new RegistroTaller("r2", Map.of("Producto", "Fideos")),
                            new RegistroTaller("r3", Map.of("Producto", "Arroz"))))));
    private static final EstadoTaller SIN_PIEZAS = new EstadoTaller("Mi sistema", "mosaic", List.of());
    private static final List<MensajeTaller> HISTORIAL = List.of(new MensajeTaller("sami", "Hola"));
    private static final ImagenTaller FOTO = new ImagenTaller("image/png", new byte[]{1, 2, 3});

    @Mock
    private TallerIaPort tallerIaPort;

    @Mock
    private ConsumoSamiRepository consumoSamiRepository;

    @Mock
    private ControlPlanService controlPlanService;

    private ConversarEnTaller conversacion;
    private EstadoTaller estado = ESTADO;
    private List<MensajeTaller> historial = HISTORIAL;
    private ImagenTaller imagen;

    private RespuestaTaller respuesta;
    private RespuestaTaller segundaRespuesta;
    private RespuestaTaller terceraRespuesta;
    private Throwable rechazo;
    private ConversarEnTaller conversacionSinNegocio;
    private ConversarEnTallerRequest pedidoSinNegocio;

    @BeforeEach
    void setUp() {
        conversacion = TallerDePrueba.conversacion(tallerIaPort, consumoSamiRepository, controlPlanService,
                SamiPruebas.kitsReales(), Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC), LIMITE,
                TallerDePrueba.LIMITE_DIARIO_FOTOS);
    }

    @Test
    @DisplayName("Una foto cuenta como un solo análisis de foto del plan, aunque el modelo se consulte varias veces")
    void unaFotoCuentaComoUnSoloAnalisisDeFotoDelPlanAunqueElModeloSeConsulteVariasVeces() {
        dadoElCupoDisponible();
        dadoUnaFotoSobreUnSistemaSinPiezas();
        dadoQueElModeloArmaElInventarioYPreguntaPorLosTildes();

        cuandoSeConversaCon("leé esta foto");

        entoncesSeReservaUnSoloAnalisisDeFotoYSeConsultaAlModeloVariasVeces();
    }

    @Test
    @DisplayName("Un mensaje sin foto no descuenta análisis de foto")
    void unMensajeSinFotoNoDescuentaAnalisisDeFoto() {
        dadoElCupoDisponible();
        dadoQueElModeloResponde(resultado("Hola"));

        cuandoSeConversaCon("hola");

        entoncesSoloSeVerificaElAccesoALaIaSinDescontarFotos();
    }

    @Test
    @DisplayName("Si la IA no está habilitada para el dueño, no reserva ni consulta")
    void siLaIaNoEstaHabilitadaParaElDuenioNoReservaNiConsulta() {
        dadoQueLaIaNoEstaHabilitadaParaElDuenio();

        cuandoSeIntentaConversarCon("hola");

        entoncesSeRechazaPorQueLaIaNoEstaHabilitadaSinReservarNiConsultar();
    }

    @Test
    @DisplayName("Si se agotaron los análisis de foto del plan, no consulta a la IA")
    void siSeAgotaronLosAnalisisDeFotoDelPlanNoConsultaALaIa() {
        dadoElCupoDisponible();
        dadoQueSeAgotaronLosAnalisisDeFotoDelPlan();
        dadoUnaFoto();

        cuandoSeIntentaConversarCon("leé esta foto");

        entoncesSeRechazaPorElPlanSinConsultarALaIa();
    }

    @Test
    @DisplayName("Reserva, consulta a la IA, registra el consumo y devuelve el texto con las acciones validadas")
    void reservaConsultaALaIaRegistraElConsumoYDevuelveElTextoConLasAccionesValidadas() {
        dadoElCupoDisponible();
        dadoQueElModeloSumaGastosYPideUnaHerramientaInexistente();

        cuandoSeConversaCon("sumá gastos");

        entoncesSoloSeAplicaSumarGastosYSeRegistraElConsumo();
    }

    @Test
    @DisplayName("Si Sami ya preguntó en su texto sin botonera, no suma la pregunta del kit; con botonera vale la suya")
    void siSamiYaPreguntoEnSuTextoSinBotoneraNoSumaLaPreguntaDelKitConBotoneraValeLaSuya() {
        dadoElCupoDisponible();
        dadoUnSistemaSinPiezas();
        dadoQueElModeloArmaElKitDePeluqueriaDeTresFormas();

        cuandoSeConversaTresVecesCon("peluquería");

        entoncesSoloLaBotoneraPropiaOLaDelKitHacenPregunta();
    }

    @Test
    @DisplayName("Saca las negritas de markdown: la charla muestra texto plano")
    void sacaLasNegritasDeMarkdownLaCharlaMuestraTextoPlano() {
        dadoElCupoDisponible();
        dadoQueElModeloResponde(resultado(" Tenés **Turnos** y **Clientes**. "));

        cuandoSeConversaCon("peluquería");

        entoncesElTextoEs("Tenés Turnos y Clientes.");
    }

    @Test
    @DisplayName("Le pasa a la IA el mensaje, el estado, la charla reciente y el kit del rubro reconocido")
    void lePasaALaIaElMensajeElEstadoLaCharlaRecienteYElKitDelRubroReconocido() {
        dadoElCupoDisponible();
        dadoQueElModeloResponde(resultado("Hola"));

        cuandoSeConversaCon("vendo velas");

        entoncesLaIaRecibeElMensajeElEstadoLaCharlaYElKitDeVelas();
    }

    @Test
    @DisplayName("Si no reconoce el rubro no le pasa ningún kit")
    void siNoReconoceElRubroNoLePasaNingunKit() {
        dadoElCupoDisponible();
        dadoUnSistemaSinPiezasYSinCharla();
        dadoQueElModeloResponde(resultado("Hola"));

        cuandoSeConversaCon("hola");

        entoncesLaIaRecibeLaConsultaSinKit();
    }

    @Test
    @DisplayName("Si el modelo sólo usó un kit, cuenta cada pieza y cada conexión por su nombre")
    void siElModeloSoloUsoUnKitCuentaCadaPiezaYCadaConexionPorSuNombre() {
        dadoElCupoDisponible();
        dadoUnSistemaSinPiezas();
        dadoQueElModeloResponde(resultado(null, llamada("usar_kit", Map.of("rubro", "peluqueria"))));

        cuandoSeConversaCon("peluquería");

        entoncesElTextoCuentaCadaPiezaYCadaConexionDelKit();
    }

    @Test
    @DisplayName("Cuenta cuántos registros cargó y en qué pieza, en singular o en plural")
    void cuentaCuantosRegistrosCargoYEnQuePiezaEnSingularOEnPlural() {
        dadoElCupoDisponible();
        dadoUnaFotoSobreUnSistemaConDatos();
        dadoQueElModeloCargaTresRegistrosEnInventarioYUnoEnGastos();

        cuandoSeConversaCon("te paso la foto");

        entoncesElTextoCuentaLosRegistrosCargadosEnSingularYEnPlural();
    }

    @Test
    @DisplayName("La foto llega a la IA en el turno y en cada reintento, y el turno cuenta una sola consulta del día")
    void laFotoLlegaALaIaEnElTurnoYEnCadaReintentoYElTurnoCuentaUnaSolaConsultaDelDia() {
        dadoElCupoDisponible();
        dadoUnaFoto();
        dadoQueElModeloDiceQueCargoVentasSinHacerNada();

        cuandoSeConversaCon("te paso la foto");

        entoncesLaFotoViajaEnLasCuatroConsultasYSeReservaUnaVez();
    }

    @Test
    @DisplayName("Con foto, si armó las piezas sin cargar lo que lee, se le piden los registros viendo las piezas armadas")
    void conFotoSiArmoLasPiezasSinCargarLoQueLeeSeLePidenLosRegistrosViendoLasPiezasArmadas() {
        dadoElCupoDisponible();
        dadoUnaFoto();
        dadoQueElModeloArmaVentasSinCargarYLuegoCargaRegistros();

        cuandoSeConversaCon("te paso la foto");

        entoncesSeLePidenLosRegistrosViendoLasPiezasArmadas();
    }

    @Test
    @DisplayName("Los ejemplos del kit sólo se conservan al armar un sistema nuevo sin foto")
    void losEjemplosDelKitSoloSeConservanAlArmarUnSistemaNuevoSinFoto() {
        dadoElCupoDisponible();
        dadoQueElModeloArmaElKitDeVelas();

        cuandoSePideElKitDeVelasEnUnSistemaNuevoConFotoYAjustando();

        entoncesSoloElSistemaNuevoSinFotoConservaLosEjemplos();
    }

    @Test
    @DisplayName("Con foto, la duda del segundo pedido va a la botonera salvo que el modelo ya hubiera preguntado")
    void conFotoLaDudaDelSegundoPedidoVaALaBotoneraSalvoQueElModeloYaHubieraPreguntado() {
        dadoElCupoDisponible();
        dadoUnaFoto();
        dadoQueElModeloDejaLaDudaEnElSegundoPedidoYEnElPrimeroPregunta();

        cuandoSeConversaDosVecesCon("foto");

        entoncesAmbasRespuestasPreguntanPorLosTildes();
    }

    @Test
    @DisplayName("Con foto, si ya cargó registros sólo se le piden las dudas; sin foto, no se le pide nada")
    void conFotoSiYaCargoRegistrosSoloSeLePidenLasDudasSinFotoNoSeLePideNada() {
        dadoElCupoDisponible();
        dadoQueElModeloCargaRegistrosYLuegoSaluda();

        cuandoSeConversaConFotoYLuegoSinFotoSobreUnSistemaConDatos();

        entoncesLaIaSeConsultaTresVeces();
    }

    @Test
    @DisplayName("Con foto, si el modelo no dejó dudas se le piden juntas; si ya preguntó en el texto, no")
    void conFotoSiElModeloNoDejoDudasSeLePidenJuntasSiYaPreguntoEnElTextoNo() {
        dadoElCupoDisponible();
        dadoUnaFotoSobreUnSistemaConDatos();
        dadoQueElModeloNoDejaDudasYLaSegundaVezPreguntaEnElTexto();

        cuandoSeConversaDosVecesCon("foto");

        entoncesSePidenLasDudasJuntasSoloCuandoElModeloNoPregunto();
    }

    @Test
    @DisplayName("Con foto, si el segundo pedido tampoco carga registros, queda lo del primero")
    void conFotoSiElSegundoPedidoTampocoCargaRegistrosQuedaLoDelPrimero() {
        dadoElCupoDisponible();
        dadoUnaFoto();
        dadoQueElModeloArmaGastosYLuegoNoLeeNada();

        cuandoSeConversaCon("foto");

        entoncesQuedaLoDelPrimerPedido();
    }

    @Test
    @DisplayName("Cuenta que borró registros, con los criterios en palabras")
    void cuentaQueBorroRegistrosConLosCriteriosEnPalabras() {
        dadoElCupoDisponible();
        dadoUnSistemaConDatos();
        dadoQueElModeloBorraDosRegistrosPorCriterio();

        cuandoSeConversaCon("borrá");

        entoncesElTextoCuentaElBorradoConLosCriteriosEnPalabras();
    }

    @Test
    @DisplayName("Cuenta cuántos registros corrigió y cuántos borró por id, con o sin criterios")
    void cuentaCuantosRegistrosCorrigioYCuantosBorroPorIdConOSinCriterios() {
        dadoElCupoDisponible();
        dadoUnSistemaConRegistros();
        dadoQueElModeloCorrigeYBorraRegistrosPorId();

        cuandoSeConversaCon("corregí");

        entoncesElTextoCuentaLasCorreccionesYLosBorrados();
    }

    @Test
    @DisplayName("Si dice que borró pero no invocó la herramienta, se le pide que lo haga")
    void siDiceQueBorroPeroNoInvocoLaHerramientaSeLePideQueLoHaga() {
        dadoElCupoDisponible();
        dadoUnSistemaConDatos();
        dadoQueElModeloDiceQueBorroSinInvocarLaHerramienta();

        cuandoSeConversaCon("borrá");

        entoncesRespondeQueNoHuboCambiosTrasReintentarUnaVez();
    }

    @Test
    @DisplayName("Si algunos renglones no se pudieron cargar, Sami lo dice en vez de afirmar que cargó todo")
    void siAlgunosRenglonesNoSePudieronCargarSamiLoDiceEnVezDeAfirmarQueCargoTodo() {
        dadoElCupoDisponible();
        dadoUnaFotoSobreUnSistemaConDatos();
        dadoQueElModeloCargaRenglonesSinElProductoEnDosTurnos();

        cuandoSeConversaDosVecesCon("foto");

        entoncesSamiAvisaQueNoPudoCargarDosRenglonesYLuegoUno();
    }

    @Test
    @DisplayName("Si el segundo pedido de la foto también descarta renglones, se suman a los del primero")
    void siElSegundoPedidoDeLaFotoTambienDescartaRenglonesSeSumanALosDelPrimero() {
        dadoElCupoDisponible();
        dadoUnaFotoSobreUnSistemaConDatos();
        dadoQueElModeloRenombraYEnElSegundoPedidoCargaRenglonesSinProducto();

        cuandoSeConversaCon("foto");

        entoncesSamiAvisaQueNoPudoCargarUnRenglon();
    }

    @Test
    @DisplayName("Si borra para recargar, como pidió la persona, y no carga, se le pide una vez que cargue lo corregido")
    void siBorraParaRecargarComoPidioLaPersonaYNoCargaSeLePideUnaVezQueCargueLoCorregido() {
        dadoElCupoDisponible();
        dadoUnSistemaConDatos();
        dadoQueElModeloBorraYLuegoCargaLoCorregido();

        cuandoSeConversaCon("borrá la yerba y cargala de nuevo como Yerba Playadito");

        entoncesSeLePideUnaVezQueCargueLoCorregido();
    }

    @Test
    @DisplayName("Si sólo pidió borrar, no se le pide cargar nada; y si el pedido extra no carga, queda el borrado")
    void siSoloPidioBorrarNoSeLePideCargarNadaYSiElPedidoExtraNoCargaQuedaElBorrado() {
        dadoElCupoDisponible();
        dadoUnSistemaConDatos();
        dadoQueElModeloSoloBorraEnTresConsultas();

        cuandoSeConversaPidiendoSoloBorrarYPidiendoBorrarYRecargar();

        entoncesQuedaSoloElBorradoYSeConsultaTresVeces();
    }

    @Test
    @DisplayName("Si borra y carga en la misma respuesta, no se le pide nada más")
    void siBorraYCargaEnLaMismaRespuestaNoSeLePideNadaMas() {
        dadoElCupoDisponible();
        dadoUnSistemaConDatos();
        dadoQueElModeloBorraYCargaEnLaMismaRespuesta();

        cuandoSeConversaCon("borrala y cargala de nuevo");

        entoncesSeBorraYSeCargaConUnaSolaConsulta();
    }

    @Test
    @DisplayName("Con el límite diario agotado no llama a la IA")
    void conElLimiteDiarioAgotadoNoLlamaALaIa() {
        dadoQueSeAgotoElLimiteDiario();

        cuandoSeIntentaConversarCon("hola");

        entoncesSeRechazaPorElLimiteSinLlamarALaIa();
    }

    @Test
    @DisplayName("Si la IA falla avisa con un mensaje simple")
    void siLaIaFallaAvisaConUnMensajeSimple() {
        dadoElCupoDisponible();
        dadoQueLaIaFallaPorTimeout();

        cuandoSeIntentaConversarCon("hola");

        entoncesSeAvisaQueLaIaNoEstaDisponibleSinRegistrarConsumo();
    }

    @Test
    @DisplayName("Si el modelo no escribió texto usa la pregunta de la botonera")
    void siElModeloNoEscribioTextoUsaLaPreguntaDeLaBotonera() {
        dadoElCupoDisponible();
        dadoQueElModeloSoloOfreceOpciones();

        cuandoSeConversaCon("ayuda");

        entoncesElTextoEsLaPreguntaDeLaBotonera();
    }

    @Test
    @DisplayName("Si el modelo no escribió texto cuenta lo que quedó hecho")
    void siElModeloNoEscribioTextoCuentaLoQueQuedoHecho() {
        dadoElCupoDisponible();
        dadoQueElModeloSoloSumaGastosConTextoEnBlanco();

        cuandoSeConversaCon("gastos");

        entoncesElTextoEs("Sumé gastos. Mirá cómo queda.");
    }

    @Test
    @DisplayName("Si el modelo no escribió texto cuenta cada cosa que quedó hecha")
    void siElModeloNoEscribioTextoCuentaCadaCosaQueQuedoHecha() {
        dadoElCupoDisponible();
        dadoQueElModeloSumaGastosYCambiaLaDistribucionSinTexto();

        cuandoSeConversaCon("gastos");

        entoncesElTextoEs("Sumé gastos. Cambié la distribución. Mirá cómo queda.");
    }

    @Test
    @DisplayName("Describe con el nombre de cada pieza las piezas y los datos que se tocaron")
    void describeConElNombreDeCadaPiezaLasPiezasYLosDatosQueSeTocaron() {
        dadoElCupoDisponible();
        dadoUnSistemaConDatosYUnaPiezaDePedidos();
        dadoQueElModeloQuitaRenombraYAmpliaPiezas();

        cuandoSeConversaCon("ordená");

        entoncesElTextoDescribeCadaPiezaYDatoTocado();
    }

    @Test
    @DisplayName("Cuenta qué dato cambió y en qué pieza, con la etiqueta que tenía")
    void cuentaQueDatoCambioYEnQuePiezaConLaEtiquetaQueTenia() {
        dadoElCupoDisponible();
        dadoUnSistemaConDatos();
        dadoQueElModeloHaceObligatorioElDatoProducto();

        cuandoSeConversaCon("cambialo");

        entoncesElTextoEs("Cambié Producto en inventario. Mirá cómo queda.");
    }

    @Test
    @DisplayName("Describe una pieza que se sumó en el mismo turno con su nombre del catálogo")
    void describeUnaPiezaQueSeSumoEnElMismoTurnoConSuNombreDelCatalogo() {
        dadoElCupoDisponible();
        dadoUnSistemaConDatos();
        dadoQueElModeloSumaGastosLeAgregaUnDatoYSeparaClientes();

        cuandoSeConversaCon("gastos");

        entoncesElTextoDescribeLaPiezaSumadaConSuNombreDelCatalogo();
    }

    @Test
    @DisplayName("Si el modelo no escribió texto y nada de lo que pidió sirve, lo dice")
    void siElModeloNoEscribioTextoYNadaDeLoQuePidioSirveLoDice() {
        dadoElCupoDisponible();
        dadoQueElModeloPideUnaPiezaDeUnTipoInexistente();

        cuandoSeConversaCon("turnos");

        entoncesRespondeQueNoHuboCambiosYNoHayAcciones();
    }

    @Test
    @DisplayName("Lleva el texto del modelo sin espacios sobrantes")
    void llevaElTextoDelModeloSinEspaciosSobrantes() {
        dadoElCupoDisponible();
        dadoQueElModeloResponde(resultado("  Hola  \n"));

        cuandoSeConversaCon("hola");

        entoncesElTextoEs("Hola");
    }

    @Test
    @DisplayName("Si dice que armó algo sin invocar herramientas, se le pide una vez que lo haga y vale el reintento")
    void siDiceQueArmoAlgoSinInvocarHerramientasSeLePideUnaVezQueLoHagaYValeElReintento() {
        dadoElCupoDisponible();
        dadoQueElModeloDiceQueArmoAlgoYEnElReintentoLoHace();

        cuandoSeConversaCon("llevo arreglos");

        entoncesVaLeElReintentoYSeRegistranDosConsumosConUnaSolaReserva();
    }

    @Test
    @DisplayName("Si después del reintento sigue diciendo que hizo algo sin hacerlo, contesta con honestidad")
    void siDespuesDelReintentoSigueDiciendoQueHizoAlgoSinHacerloContestaConHonestidad() {
        dadoElCupoDisponible();
        dadoQueElModeloSiempreDiceQueMovioAlClienteSinHacerlo();

        cuandoSeConversaCon("poné el cliente primero");

        entoncesRespondeQueNoHuboCambiosTrasReintentarUnaVezSinAcciones();
    }

    @Test
    @DisplayName("Una explicación sin cambios no se reintenta")
    void unaExplicacionSinCambiosNoSeReintenta() {
        dadoElCupoDisponible();
        dadoQueElModeloSoloExplicaQueYaEstaVinculado();

        cuandoSeConversaCon("¿están vinculadas?");

        entoncesLaExplicacionLlegaSinReintentar();
    }

    @Test
    @DisplayName("Si el usuario no tiene negocio no consulta a la IA")
    void siElUsuarioNoTieneNegocioNoConsultaALaIa() {
        dadoUnUsuarioSinNegocio();

        cuandoSeIntentaConversarSinNegocio();

        entoncesSeRechazaComoInexistenteSinConsultarNada();
    }

    // --- dado ---

    private void dadoElCupoDisponible() {
        when(consumoSamiRepository.reservar(NegocioId.de(NEGOCIO_ID), HOY, LIMITE, AHORA)).thenReturn(true);
    }

    private void dadoUnaFoto() {
        imagen = FOTO;
    }

    private void dadoUnSistemaSinPiezas() {
        estado = SIN_PIEZAS;
    }

    private void dadoUnSistemaSinPiezasYSinCharla() {
        estado = SIN_PIEZAS;
        historial = List.of();
    }

    private void dadoUnSistemaConDatos() {
        estado = ESTADO_CON_DATOS;
    }

    private void dadoUnSistemaConDatosYUnaPiezaDePedidos() {
        estado = new EstadoTaller("Mi almacén", "mosaic", List.of(
                new PiezaTaller("e1", "inventory", "Inventario", null, List.of(new CampoTaller("Producto", "text"))),
                new PiezaTaller("e2", "orders", "Pedidos", null, List.of())));
    }

    private void dadoUnSistemaConRegistros() {
        estado = ESTADO_CON_REGISTROS;
    }

    private void dadoUnaFotoSobreUnSistemaSinPiezas() {
        dadoUnaFoto();
        dadoUnSistemaSinPiezas();
    }

    private void dadoUnaFotoSobreUnSistemaConDatos() {
        dadoUnaFoto();
        dadoUnSistemaConDatos();
    }

    private void dadoQueElModeloResponde(ResultadoTallerIa primero, ResultadoTallerIa... siguientes) {
        when(tallerIaPort.conversar(any())).thenReturn(primero, siguientes);
    }

    private void dadoQueElModeloArmaElInventarioYPreguntaPorLosTildes() {
        dadoQueElModeloResponde(resultado("Armé el inventario. ¿Qué significan los tildes?",
                llamada("agregar_pieza", Map.of("tipo", "inventory"))));
    }

    private void dadoQueLaIaNoEstaHabilitadaParaElDuenio() {
        doThrow(new RestriccionPlanException("IA_NO_HABILITADA", "La IA no está habilitada para tu cuenta."))
                .when(controlPlanService).exigirIa(NegocioId.de(NEGOCIO_ID));
    }

    private void dadoQueSeAgotaronLosAnalisisDeFotoDelPlan() {
        doThrow(new RestriccionPlanException("LIMITE_IA_MENSUAL", "Sin análisis de foto"))
                .when(controlPlanService).reservarPaginaIaInmediata(NegocioId.de(NEGOCIO_ID),
                        TallerDePrueba.LIMITE_DIARIO_FOTOS, AHORA);
    }

    private void dadoQueElModeloSumaGastosYPideUnaHerramientaInexistente() {
        dadoQueElModeloResponde(resultado("Listo, sumé gastos.",
                llamada("agregar_pieza", Map.of("tipo", "expenses")),
                llamada("borrar_todo", Map.of())));
    }

    private void dadoQueElModeloArmaElKitDePeluqueriaDeTresFormas() {
        LlamadaHerramienta kit = llamada("usar_kit", Map.of("rubro", "peluqueria"));
        LlamadaHerramienta botonera = llamada("ofrecer_opciones",
                Map.of("pregunta", "¿Sumamos productos?", "opciones", List.of("Sí", "No")));
        dadoQueElModeloResponde(
                resultado("Armé turnos. ¿Le agrego algo?", kit),
                resultado("Armé turnos. ¿Sumamos productos?", kit, botonera),
                resultado("Armé turnos.", kit));
    }

    private void dadoQueElModeloCargaTresRegistrosEnInventarioYUnoEnGastos() {
        dadoQueElModeloResponde(resultado(null,
                llamada("cargar_registros", Map.of("pieza", "e1", "filas", List.of(
                        Map.of("Producto", "Yerba"), Map.of("Producto", "Azúcar"), Map.of("Producto", "Té")))),
                llamada("agregar_pieza", Map.of("tipo", "expenses")),
                llamada("cargar_registros", Map.of("pieza", "expenses", "filas", List.of(
                        Map.of("Concepto", "Luz"))))));
    }

    private void dadoQueElModeloDiceQueCargoVentasSinHacerNada() {
        dadoQueElModeloResponde(resultado("Cargué tus ventas."), resultado("Listo."));
    }

    private void dadoQueElModeloArmaVentasSinCargarYLuegoCargaRegistros() {
        dadoQueElModeloResponde(
                resultado("Te armo las ventas.",
                        llamada("agregar_pieza", Map.of("tipo", "sales", "nombre", "Ventas"))),
                resultado("",
                        llamada("cargar_registros", Map.of("pieza", "Ventas", "filas", List.of(
                                Map.of("Producto", "Vela lavanda", "Cantidad", 2)))),
                        llamada("quitar_pieza", Map.of("pieza", "e1"))));
    }

    private void dadoQueElModeloArmaElKitDeVelas() {
        dadoQueElModeloResponde(resultado("Armé tus velas.", llamada("usar_kit", Map.of("rubro", "velas"))));
    }

    private void dadoQueElModeloDejaLaDudaEnElSegundoPedidoYEnElPrimeroPregunta() {
        LlamadaHerramienta registros = cargaDeYerba();
        LlamadaHerramienta duda = llamada("ofrecer_opciones", Map.of(
                "pregunta", "¿Qué significan los tildes?", "opciones", List.of("Pagado", "Entregado")));
        LlamadaHerramienta otra = llamada("ofrecer_opciones", Map.of(
                "pregunta", "¿Otra cosa?", "opciones", List.of("Sí", "No")));
        dadoQueElModeloResponde(
                resultado("Veo un cuaderno.", llamada("agregar_pieza", Map.of("tipo", "sales", "nombre", "Ventas"))),
                resultado("", registros, duda),
                resultado("Veo un cuaderno.", duda),
                resultado("", registros, otra));
    }

    private void dadoQueElModeloCargaRegistrosYLuegoSaluda() {
        dadoQueElModeloResponde(
                resultado("Listo.", llamada("cargar_registros",
                        Map.of("pieza", "e1", "filas", List.of(Map.of("Producto", "Yerba"))))),
                resultado("Hola"));
    }

    private void dadoQueElModeloNoDejaDudasYLaSegundaVezPreguntaEnElTexto() {
        LlamadaHerramienta registros = cargaDeYerba();
        LlamadaHerramienta dudas = llamada("ofrecer_opciones", Map.of("preguntas", List.of(
                Map.of("tema", "Tildes", "pregunta", "¿Qué significan los tildes?",
                        "opciones", List.of("Entregado", "Pagado")),
                Map.of("tema", "Número", "pregunta", "¿Qué es el número de la derecha?",
                        "opciones", List.of("Costo", "Deuda")))));
        dadoQueElModeloResponde(
                resultado("Cargué lo que leí.", registros),
                resultado("", dudas),
                resultado("Cargué lo que leí. ¿Los tildes son pagos?", registros));
    }

    private void dadoQueElModeloArmaGastosYLuegoNoLeeNada() {
        dadoQueElModeloResponde(
                resultado("Te armo gastos.", llamada("agregar_pieza", Map.of("tipo", "expenses"))),
                resultado("No se lee nada."));
    }

    private void dadoQueElModeloBorraDosRegistrosPorCriterio() {
        dadoQueElModeloResponde(resultado(null, llamada("borrar_registros", Map.of("pieza", "e1", "donde", List.of(
                Map.of("Producto", "Yerba"), Map.of("Producto", "Fideos"))))));
    }

    private void dadoQueElModeloCorrigeYBorraRegistrosPorId() {
        dadoQueElModeloResponde(resultado(null,
                llamada("editar_registros", Map.of("pieza", "e1", "registros", List.of(
                        Map.of("id", "r1", "datos", Map.of("Producto", "Yerba mate")),
                        Map.of("id", "r2", "datos", Map.of("Producto", "Fideos largos"))))),
                llamada("editar_registros", Map.of("pieza", "e1", "registros", List.of(
                        Map.of("id", "r3", "datos", Map.of("Producto", "Arroz largo"))))),
                llamada("borrar_registros", Map.of("pieza", "e1", "ids", List.of("r1"))),
                llamada("borrar_registros", Map.of("pieza", "e1", "ids", List.of("r2", "r3"),
                        "donde", Map.of("Producto", "Té")))));
    }

    private void dadoQueElModeloDiceQueBorroSinInvocarLaHerramienta() {
        dadoQueElModeloResponde(resultado("Listo, borré las ventas de Anto."), resultado("Eliminé todo."));
    }

    private void dadoQueElModeloCargaRenglonesSinElProductoEnDosTurnos() {
        LlamadaHerramienta registros = llamada("cargar_registros", Map.of("pieza", "e1", "filas", List.of(
                Map.of("Producto", "Yerba"), Map.of("Cantidad", 2), Map.of("Cantidad", 3))));
        LlamadaHerramienta otra = llamada("cargar_registros", Map.of("pieza", "e1", "filas", List.of(
                Map.of("Producto", "Fideos"), Map.of("Cantidad", 2))));
        dadoQueElModeloResponde(resultado("Cargué todo.", registros), resultado("Cargué todo.", otra));
    }

    private void dadoQueElModeloRenombraYEnElSegundoPedidoCargaRenglonesSinProducto() {
        LlamadaHerramienta registros = llamada("cargar_registros", Map.of("pieza", "e1", "filas", List.of(
                Map.of("Producto", "Yerba"), Map.of("Cantidad", 2))));
        dadoQueElModeloResponde(
                resultado("Armé.", llamada("renombrar_sistema", Map.of("nombre", "Mis ventas"))),
                resultado("", registros));
    }

    private void dadoQueElModeloBorraYLuegoCargaLoCorregido() {
        LlamadaHerramienta cargar = llamada("cargar_registros",
                Map.of("pieza", "e1", "filas", List.of(Map.of("Producto", "Yerba Playadito"))));
        dadoQueElModeloResponde(resultado("Borro y recargo.", borrarYerba()), resultado("", cargar));
    }

    private void dadoQueElModeloSoloBorraEnTresConsultas() {
        dadoQueElModeloResponde(
                resultado("Listo.", borrarYerba()),
                resultado("Listo.", borrarYerba()),
                resultado("No sé."));
    }

    private void dadoQueElModeloBorraYCargaEnLaMismaRespuesta() {
        dadoQueElModeloResponde(resultado("Listo.", borrarYerba(), llamada("cargar_registros", Map.of("pieza", "e1",
                "filas", List.of(Map.of("Producto", "Yerba Playadito"))))));
    }

    private void dadoQueSeAgotoElLimiteDiario() {
        when(consumoSamiRepository.reservar(NegocioId.de(NEGOCIO_ID), HOY, LIMITE, AHORA)).thenReturn(false);
    }

    private void dadoQueLaIaFallaPorTimeout() {
        when(tallerIaPort.conversar(any())).thenThrow(new IllegalStateException("timeout"));
    }

    private void dadoQueElModeloSoloOfreceOpciones() {
        dadoQueElModeloResponde(resultado(null, llamada("ofrecer_opciones", Map.of(
                "pregunta", "¿Qué sumamos?", "opciones", List.of("Gastos", "Clientes")))));
    }

    private void dadoQueElModeloSoloSumaGastosConTextoEnBlanco() {
        dadoQueElModeloResponde(resultado("  ", llamada("agregar_pieza", Map.of("tipo", "expenses"))));
    }

    private void dadoQueElModeloSumaGastosYCambiaLaDistribucionSinTexto() {
        dadoQueElModeloResponde(resultado("",
                llamada("agregar_pieza", Map.of("tipo", "expenses")),
                llamada("cambiar_distribucion", Map.of("layout", "single"))));
    }

    private void dadoQueElModeloQuitaRenombraYAmpliaPiezas() {
        dadoQueElModeloResponde(resultado(null,
                llamada("quitar_pieza", Map.of("pieza", "e2")),
                llamada("renombrar_pieza", Map.of("pieza", "e1", "nombre", "Productos")),
                llamada("renombrar_sistema", Map.of("nombre", "Velas")),
                llamada("agregar_campo", Map.of("pieza", "e1", "etiqueta", "Proveedor", "tipo", "text")),
                llamada("quitar_campo", Map.of("pieza", "e1", "campo", "proveedor"))));
    }

    private void dadoQueElModeloHaceObligatorioElDatoProducto() {
        dadoQueElModeloResponde(resultado(null,
                llamada("editar_campo", Map.of("pieza", "e1", "campo", "producto", "obligatorio", true))));
    }

    private void dadoQueElModeloSumaGastosLeAgregaUnDatoYSeparaClientes() {
        dadoQueElModeloResponde(resultado(null,
                llamada("agregar_pieza", Map.of("tipo", "expenses")),
                llamada("agregar_campo", Map.of("pieza", "expenses", "etiqueta", "Rubro", "tipo", "text")),
                llamada("separar_en_seccion", Map.of("pieza", "e2", "seccion", "Aparte"))));
    }

    private void dadoQueElModeloPideUnaPiezaDeUnTipoInexistente() {
        dadoQueElModeloResponde(resultado(null, llamada("agregar_pieza", Map.of("tipo", "turnos"))));
    }

    private void dadoQueElModeloDiceQueArmoAlgoYEnElReintentoLoHace() {
        dadoQueElModeloResponde(
                resultado("Armé tu sistema con Arreglos y Clientes."),
                resultado("Armé Gastos.", llamada("agregar_pieza", Map.of("tipo", "expenses"))));
    }

    private void dadoQueElModeloSiempreDiceQueMovioAlClienteSinHacerlo() {
        dadoQueElModeloResponde(resultado("Moví el cliente adelante."));
    }

    private void dadoQueElModeloSoloExplicaQueYaEstaVinculado() {
        dadoQueElModeloResponde(resultado("Ya está vinculado: al registrar una venta elegís la clienta de una lista."));
    }

    private void dadoUnUsuarioSinNegocio() {
        UsuarioId usuarioSinNegocio = UsuarioId.de(UUID.randomUUID());
        NegocioRepository negocioRepository = mock(NegocioRepository.class);
        when(negocioRepository.obtenerIdDelUsuario(usuarioSinNegocio))
                .thenThrow(new ResourceNotFoundException("Sin negocio"));
        conversacionSinNegocio = new ConversarEnTaller(negocioRepository, tallerIaPort, consumoSamiRepository,
                SamiPruebas.catalogoReal(), SamiPruebas.kitsReales(), controlPlanService, Clock.systemUTC(), LIMITE,
                TallerDePrueba.LIMITE_DIARIO_FOTOS);
        pedidoSinNegocio = new ConversarEnTallerRequest(usuarioSinNegocio, "hola",
                new EstadoTallerItem("", "mosaic", List.of()), List.of(), null);
    }

    // --- cuando ---

    private void cuandoSeConversaCon(String mensaje) {
        respuesta = TallerDePrueba.conversar(conversacion, mensaje, imagen, estado, historial);
    }

    private void cuandoSeIntentaConversarCon(String mensaje) {
        rechazo = catchThrowable(() -> cuandoSeConversaCon(mensaje));
    }

    private void cuandoSeConversaDosVecesCon(String mensaje) {
        cuandoSeConversaCon(mensaje);
        segundaRespuesta = TallerDePrueba.conversar(conversacion, mensaje, imagen, estado, historial);
    }

    private void cuandoSeConversaTresVecesCon(String mensaje) {
        cuandoSeConversaDosVecesCon(mensaje);
        terceraRespuesta = TallerDePrueba.conversar(conversacion, mensaje, imagen, estado, historial);
    }

    private void cuandoSePideElKitDeVelasEnUnSistemaNuevoConFotoYAjustando() {
        respuesta = TallerDePrueba.conversar(conversacion, "velas", null, SIN_PIEZAS, HISTORIAL);
        segundaRespuesta = TallerDePrueba.conversar(conversacion, "velas", FOTO, SIN_PIEZAS, HISTORIAL);
        terceraRespuesta = TallerDePrueba.conversar(conversacion, "velas", null, ESTADO_CON_DATOS, HISTORIAL);
    }

    private void cuandoSeConversaConFotoYLuegoSinFotoSobreUnSistemaConDatos() {
        TallerDePrueba.conversar(conversacion, "te paso la foto", FOTO, ESTADO_CON_DATOS, HISTORIAL);
        TallerDePrueba.conversar(conversacion, "hola", null, ESTADO, HISTORIAL);
    }

    private void cuandoSeConversaPidiendoSoloBorrarYPidiendoBorrarYRecargar() {
        respuesta = TallerDePrueba.conversar(conversacion, "borrá la yerba", null, ESTADO_CON_DATOS, HISTORIAL);
        segundaRespuesta = TallerDePrueba.conversar(conversacion, "borrala y cargala otra vez", null,
                ESTADO_CON_DATOS, HISTORIAL);
    }

    private void cuandoSeIntentaConversarSinNegocio() {
        rechazo = catchThrowable(() -> conversacionSinNegocio.execute(pedidoSinNegocio));
    }

    // --- entonces ---

    private void entoncesSeReservaUnSoloAnalisisDeFotoYSeConsultaAlModeloVariasVeces() {
        verify(controlPlanService, times(1)).reservarPaginaIaInmediata(NegocioId.de(NEGOCIO_ID),
                TallerDePrueba.LIMITE_DIARIO_FOTOS, AHORA);
        verify(tallerIaPort, atLeast(2)).conversar(any());
    }

    private void entoncesSoloSeVerificaElAccesoALaIaSinDescontarFotos() {
        verify(controlPlanService).exigirIa(NegocioId.de(NEGOCIO_ID));
        verifyNoMoreInteractions(controlPlanService);
    }

    private void entoncesSeRechazaPorQueLaIaNoEstaHabilitadaSinReservarNiConsultar() {
        assertThat(rechazo).isInstanceOf(RestriccionPlanException.class)
                .hasMessage("La IA no está habilitada para tu cuenta.");
        verifyNoInteractions(tallerIaPort, consumoSamiRepository);
    }

    private void entoncesSeRechazaPorElPlanSinConsultarALaIa() {
        assertThat(rechazo).isInstanceOf(RestriccionPlanException.class);
        verifyNoInteractions(tallerIaPort);
    }

    private void entoncesSoloSeAplicaSumarGastosYSeRegistraElConsumo() {
        assertThat(respuesta.texto()).isEqualTo("Listo, sumé gastos.");
        assertThat(respuesta.acciones()).hasSize(1);
        assertThat(respuesta.acciones().getFirst().herramienta()).isEqualTo("agregar_pieza");
        assertThat(respuesta.preguntas()).isEmpty();
        verify(consumoSamiRepository).registrar(NegocioId.de(NEGOCIO_ID), HOY, CONSUMO, AHORA);
    }

    private void entoncesSoloLaBotoneraPropiaOLaDelKitHacenPregunta() {
        assertThat(respuesta.preguntas()).isEmpty();
        assertThat(segundaRespuesta.preguntas().getFirst().pregunta()).isEqualTo("¿Sumamos productos?");
        assertThat(terceraRespuesta.preguntas().getFirst().pregunta())
                .isEqualTo("¿Atendés vos o trabajan más personas en el local?");
    }

    private void entoncesElTextoEs(String esperado) {
        assertThat(respuesta.texto()).isEqualTo(esperado);
    }

    private void entoncesLaIaRecibeElMensajeElEstadoLaCharlaYElKitDeVelas() {
        ConsultaTaller consulta = consultaEnviada();
        assertThat(consulta.mensaje()).isEqualTo("vendo velas");
        assertThat(consulta.estado()).isEqualTo(ESTADO);
        assertThat(consulta.historial()).isEqualTo(HISTORIAL);
        assertThat(consulta.kit().id()).isEqualTo("velas");
    }

    private void entoncesLaIaRecibeLaConsultaSinKit() {
        assertThat(consultaEnviada()).isEqualTo(new ConsultaTaller("hola", SIN_PIEZAS, List.of()));
    }

    private void entoncesElTextoCuentaCadaPiezaYCadaConexionDelKit() {
        assertThat(respuesta.texto()).isEqualTo("Sumé turnos. Sumé servicios. Sumé clientes."
                + " Conecté turnos con clientes. Conecté turnos con servicios. Mirá cómo queda."
                + " ¿Atendés vos o trabajan más personas en el local?");
        assertThat(respuesta.acciones()).hasSize(5);
    }

    private void entoncesElTextoCuentaLosRegistrosCargadosEnSingularYEnPlural() {
        entoncesElTextoEs("Cargué 3 registros en inventario. Sumé gastos."
                + " Cargué 1 registro en gastos. Mirá cómo queda.");
    }

    private void entoncesLaFotoViajaEnLasCuatroConsultasYSeReservaUnaVez() {
        // Turno, reintento por «dijo que hizo sin hacer», pedido de los registros y pedido de las dudas de la foto.
        assertThat(consultasEnviadas(4)).extracting(ConsultaTaller::imagen).containsExactly(FOTO, FOTO, FOTO, FOTO);
        verify(consumoSamiRepository, times(1)).reservar(NegocioId.de(NEGOCIO_ID), HOY, LIMITE, AHORA);
        verify(consumoSamiRepository, times(4)).registrar(NegocioId.de(NEGOCIO_ID), HOY, CONSUMO, AHORA);
    }

    private void entoncesSeLePidenLosRegistrosViendoLasPiezasArmadas() {
        assertThat(respuesta.acciones()).extracting(AccionTaller::herramienta)
                .containsExactly("agregar_pieza", "cargar_registros");
        assertThat(respuesta.texto()).isEqualTo("Te armo las ventas. Cargué 1 registro en ventas.");
        List<ConsultaTaller> consultas = consultasEnviadas(3);
        ConsultaTaller segunda = consultas.get(1);
        assertThat(segunda.mensaje()).isEqualTo(ConversarEnTaller.PEDIDO_REGISTROS);
        assertThat(segunda.estado().piezas()).extracting(PiezaTaller::nombre).containsExactly("Inventario", "Ventas");
        assertThat(segunda.kit()).isNull();
        assertThat(consultas.get(2).mensaje()).isEqualTo(ConversarEnTaller.PEDIDO_DUDAS);
        verify(consumoSamiRepository, times(3)).registrar(NegocioId.de(NEGOCIO_ID), HOY, CONSUMO, AHORA);
    }

    private void entoncesSoloElSistemaNuevoSinFotoConservaLosEjemplos() {
        assertThat(respuesta.acciones()).anyMatch(accion -> accion.argumentos().containsKey("ejemplos"));
        assertThat(segundaRespuesta.acciones()).noneMatch(accion -> accion.argumentos().containsKey("ejemplos"));
        assertThat(terceraRespuesta.acciones()).noneMatch(accion -> accion.argumentos().containsKey("ejemplos"));
    }

    private void entoncesAmbasRespuestasPreguntanPorLosTildes() {
        assertThat(respuesta.preguntas().getFirst().pregunta()).isEqualTo("¿Qué significan los tildes?");
        assertThat(segundaRespuesta.preguntas().getFirst().pregunta()).isEqualTo("¿Qué significan los tildes?");
    }

    private void entoncesLaIaSeConsultaTresVeces() {
        verify(tallerIaPort, times(3)).conversar(any());
    }

    private void entoncesSePidenLasDudasJuntasSoloCuandoElModeloNoPregunto() {
        assertThat(respuesta.preguntas()).extracting("tema").containsExactly("Tildes", "Número");
        assertThat(segundaRespuesta.preguntas()).isEmpty();
        verify(tallerIaPort, times(3)).conversar(any());
    }

    private void entoncesQuedaLoDelPrimerPedido() {
        assertThat(respuesta.texto()).isEqualTo("Te armo gastos.");
        assertThat(respuesta.acciones()).extracting(AccionTaller::herramienta).containsExactly("agregar_pieza");
    }

    private void entoncesElTextoCuentaElBorradoConLosCriteriosEnPalabras() {
        entoncesElTextoEs("Borré los registros de inventario donde Producto es Yerba, o Producto es Fideos."
                + " Mirá cómo queda.");
    }

    private void entoncesElTextoCuentaLasCorreccionesYLosBorrados() {
        entoncesElTextoEs("Corregí 2 registros en inventario. Corregí 1 registro en inventario."
                + " Borré 1 registro de inventario. Borré 2 registros de inventario y los registros de inventario"
                + " donde Producto es Té. Mirá cómo queda.");
    }

    private void entoncesRespondeQueNoHuboCambiosTrasReintentarUnaVez() {
        assertThat(respuesta.texto()).isEqualTo(ConversarEnTaller.SIN_CAMBIOS);
        verify(tallerIaPort, times(2)).conversar(any());
    }

    private void entoncesSamiAvisaQueNoPudoCargarDosRenglonesYLuegoUno() {
        assertThat(respuesta.texto())
                .endsWith("No pude cargar 2 renglones de la foto: faltan en la tabla, revisalos.");
        assertThat(segundaRespuesta.texto())
                .endsWith("No pude cargar 1 renglón de la foto: faltan en la tabla, revisalos.");
    }

    private void entoncesSamiAvisaQueNoPudoCargarUnRenglon() {
        assertThat(respuesta.texto()).endsWith("No pude cargar 1 renglón de la foto: faltan en la tabla, revisalos.");
    }

    private void entoncesSeLePideUnaVezQueCargueLoCorregido() {
        assertThat(respuesta.acciones()).extracting(AccionTaller::herramienta)
                .containsExactly("borrar_registros", "cargar_registros");
        assertThat(respuesta.texto()).isEqualTo("Borro y recargo. Cargué 1 registro en inventario.");
        assertThat(consultasEnviadas(2).get(1).mensaje()).startsWith(ConversarEnTaller.PEDIDO_RECARGA)
                .endsWith("como Yerba Playadito");
    }

    private void entoncesQuedaSoloElBorradoYSeConsultaTresVeces() {
        assertThat(respuesta.acciones()).hasSize(1);
        assertThat(segundaRespuesta.acciones()).hasSize(1);
        assertThat(segundaRespuesta.texto()).isEqualTo("Listo.");
        verify(tallerIaPort, times(3)).conversar(any());
    }

    private void entoncesSeBorraYSeCargaConUnaSolaConsulta() {
        assertThat(respuesta.acciones()).extracting(AccionTaller::herramienta)
                .containsExactly("borrar_registros", "cargar_registros");
        verify(tallerIaPort, times(1)).conversar(any());
    }

    private void entoncesSeRechazaPorElLimiteSinLlamarALaIa() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessage(ConversarEnTaller.LIMITE_ALCANZADO);
        verify(tallerIaPort, never()).conversar(any());
        verify(consumoSamiRepository, never()).registrar(any(), any(), any(), any());
    }

    private void entoncesSeAvisaQueLaIaNoEstaDisponibleSinRegistrarConsumo() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessage(ConversarEnTaller.IA_NO_DISPONIBLE);
        verify(consumoSamiRepository, never()).registrar(any(), any(), any(), any());
    }

    private void entoncesElTextoEsLaPreguntaDeLaBotonera() {
        assertThat(respuesta.texto()).isEqualTo("¿Qué sumamos?");
        assertThat(respuesta.preguntas().getFirst().opciones()).containsExactly("Gastos", "Clientes");
    }

    private void entoncesElTextoDescribeCadaPiezaYDatoTocado() {
        entoncesElTextoEs("Quité pedidos. Renombré inventario a Productos. Llamé al sistema Velas."
                + " Sumé Proveedor a inventario. Quité Proveedor de inventario. Mirá cómo queda.");
    }

    private void entoncesElTextoDescribeLaPiezaSumadaConSuNombreDelCatalogo() {
        entoncesElTextoEs("Sumé gastos. Sumé Rubro a gastos. Pasé clientes a la sección Aparte. Mirá cómo queda.");
    }

    private void entoncesRespondeQueNoHuboCambiosYNoHayAcciones() {
        assertThat(respuesta.texto()).isEqualTo(ConversarEnTaller.SIN_CAMBIOS);
        assertThat(respuesta.acciones()).isEmpty();
    }

    private void entoncesVaLeElReintentoYSeRegistranDosConsumosConUnaSolaReserva() {
        assertThat(respuesta.texto()).isEqualTo("Armé Gastos.");
        assertThat(respuesta.acciones()).hasSize(1);
        assertThat(consultasEnviadas(2).get(1).mensaje())
                .isEqualTo("llevo arreglos" + ConversarEnTaller.RECORDATORIO);
        verify(consumoSamiRepository, times(1)).reservar(NegocioId.de(NEGOCIO_ID), HOY, LIMITE, AHORA);
        verify(consumoSamiRepository, times(2)).registrar(NegocioId.de(NEGOCIO_ID), HOY, CONSUMO, AHORA);
    }

    private void entoncesRespondeQueNoHuboCambiosTrasReintentarUnaVezSinAcciones() {
        assertThat(respuesta.texto()).isEqualTo(ConversarEnTaller.SIN_CAMBIOS);
        assertThat(respuesta.acciones()).isEmpty();
        verify(tallerIaPort, times(2)).conversar(any());
    }

    private void entoncesLaExplicacionLlegaSinReintentar() {
        assertThat(respuesta.texto()).startsWith("Ya está vinculado");
        verify(tallerIaPort, times(1)).conversar(any());
    }

    private void entoncesSeRechazaComoInexistenteSinConsultarNada() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(tallerIaPort, consumoSamiRepository, controlPlanService);
    }

    // --- datos ---

    private static ResultadoTallerIa resultado(String texto, LlamadaHerramienta... llamadas) {
        return new ResultadoTallerIa(texto, List.of(llamadas), CONSUMO);
    }

    private static LlamadaHerramienta llamada(String nombre, Map<String, Object> argumentos) {
        return new LlamadaHerramienta(nombre, argumentos);
    }

    private static LlamadaHerramienta cargaDeYerba() {
        return llamada("cargar_registros", Map.of("pieza", "e1", "filas", List.of(Map.of("Producto", "Yerba"))));
    }

    private static LlamadaHerramienta borrarYerba() {
        return llamada("borrar_registros", Map.of("pieza", "e1", "donde", Map.of("Producto", "Yerba")));
    }

    private ConsultaTaller consultaEnviada() {
        return consultasEnviadas(1).getFirst();
    }

    private List<ConsultaTaller> consultasEnviadas(int veces) {
        ArgumentCaptor<ConsultaTaller> captor = ArgumentCaptor.forClass(ConsultaTaller.class);
        verify(tallerIaPort, times(veces)).conversar(captor.capture());
        return captor.getAllValues();
    }
}
