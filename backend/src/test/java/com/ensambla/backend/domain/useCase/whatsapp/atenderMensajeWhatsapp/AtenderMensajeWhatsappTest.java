package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.lectura.BloquePropuesto;
import com.ensambla.backend.domain.model.lectura.CampoPropuesto;
import com.ensambla.backend.domain.model.lectura.ConsumoIa;
import com.ensambla.backend.domain.model.lectura.DatoLeido;
import com.ensambla.backend.domain.model.lectura.FilaLeida;
import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.TipoRegistroCuaderno;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.procesamiento.BloqueLectura;
import com.ensambla.backend.domain.model.procesamiento.CampoConocido;
import com.ensambla.backend.domain.model.procesamiento.TablaDisponible;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.ArchivoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.EstadoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.EstadoPropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.FilaPropuesta;
import com.ensambla.backend.domain.model.whatsapp.InterpretacionRegistros;
import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.OrigenPropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.PoliticaAtencionWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TipoMensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;
import com.ensambla.backend.domain.port.InterpreteRegistrosIaPort;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.procesamiento.ProcesamientoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.repository.sami.ConsumoSamiRepository;
import com.ensambla.backend.domain.repository.whatsapp.MensajeWhatsappRepository;
import com.ensambla.backend.domain.repository.whatsapp.PropuestaWhatsappRepository;
import com.ensambla.backend.domain.repository.whatsapp.VinculoWhatsappRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.support.ImagenTestData;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AtenderMensajeWhatsappTest {

    private static final Instant INSTANTE = Instant.parse("2026-10-27T15:00:00Z");
    private static final OffsetDateTime AHORA = OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);
    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId GASTOS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final BloqueId VENTAS_ID = BloqueId.de("b0000000-0000-4000-8000-000000000002");
    private static final CampoId CONCEPTO_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final CampoId MONTO_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");
    private static final CampoId PRODUCTO_ID = CampoId.de("c0000000-0000-4000-8000-000000000003");
    private static final CampoId PRODUCTO_AJENO_ID = CampoId.de("c0000000-0000-4000-8000-000000000009");
    private static final RegistroId REGISTRO_ID = RegistroId.de("e0000000-0000-4000-8000-000000000001");
    private static final String TELEFONO = "5491122334455";

    private final MensajeWhatsappRepository mensajeRepository = mock(MensajeWhatsappRepository.class);
    private final VinculoWhatsappRepository vinculoRepository = mock(VinculoWhatsappRepository.class);
    private final PropuestaWhatsappRepository propuestaRepository = mock(PropuestaWhatsappRepository.class);
    private final ProcesamientoRepository procesamientoRepository = mock(ProcesamientoRepository.class);
    private final CampoRepository campoRepository = mock(CampoRepository.class);
    private final BloqueRepository bloqueRepository = mock(BloqueRepository.class);
    private final RegistroRepository registroRepository = mock(RegistroRepository.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private final ConsumoSamiRepository consumoSamiRepository = mock(ConsumoSamiRepository.class);
    private final InterpreteRegistrosIaPort interpreteIa = mock(InterpreteRegistrosIaPort.class);
    private final LectorCuadernoPort lectorCuadernoPort = mock(LectorCuadernoPort.class);
    private final MensajeriaWhatsappPort mensajeria = mock(MensajeriaWhatsappPort.class);
    private final AtenderMensajeWhatsapp atender = new AtenderMensajeWhatsapp(mensajeRepository, vinculoRepository,
            propuestaRepository, procesamientoRepository, campoRepository, bloqueRepository, registroRepository,
            controlPlanService, consumoSamiRepository, interpreteIa, lectorCuadernoPort, mensajeria,
            new TransaccionDirecta(), Clock.fixed(INSTANTE, ZoneOffset.UTC),
            new PoliticaAtencionWhatsapp(60, 100, 0.6, Duration.ofMinutes(2)));

    private PropuestaWhatsapp anterior;
    private PropuestaWhatsapp eligiendo;
    private Registro existente;
    private MensajeWhatsapp mensaje;
    private PropuestaWhatsapp pendiente;
    private MensajeWhatsapp perdido;
    private PropuestaWhatsapp propuesta;
    private PropuestaWhatsapp vencida;
    private boolean resultado;

    private final List<Registro> existentes = new java.util.ArrayList<>();
    private final TablaDisponible gastos = new TablaDisponible(new BloqueLectura(GASTOS_ID, "Gastos", List.of(
            new CampoConocido(CONCEPTO_ID, "Concepto", TipoCampo.TEXTO, List.of()),
            new CampoConocido(MONTO_ID, "Monto", TipoCampo.MONEDA, List.of()))), "Mi panadería");
    private final TablaDisponible ventas = new TablaDisponible(new BloqueLectura(VENTAS_ID, "Ventas", List.of(
            new CampoConocido(PRODUCTO_ID, "Producto", TipoCampo.TEXTO, List.of()))), "Mi panadería");

    @BeforeEach
    void dadoNegocioVinculadoConTablas() {
        when(vinculoRepository.findByTelefono(TELEFONO)).thenReturn(Optional.of(
                VinculoWhatsapp.nuevo(USUARIO_ID, NEGOCIO_ID, new TelefonoWhatsapp(TELEFONO), AHORA)));
        when(procesamientoRepository.tablas(NEGOCIO_ID)).thenReturn(List.of(gastos, ventas));
        when(consumoSamiRepository.reservar(eq(NEGOCIO_ID), any(), anyInt(), any())).thenReturn(true);
        when(propuestaRepository.save(any(PropuestaWhatsapp.class))).thenAnswer(i -> i.getArgument(0));
        when(mensajeRepository.vencidos(any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("Sin mensajes en la cola no hace nada y avisa que no había")
    void sinMensajesEnLaColaNoHaceNadaYAvisaQueNoHabia() {
        dadoUnaColaSinMensajes();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesNoSeHaceNadaYSeAvisaQueNoHabia();
    }

    @Test
    @DisplayName("Un mensaje reclamado que nunca terminó se marca fallido y la persona recibe el aviso")
    void unMensajeReclamadoQueNuncaTerminoSeMarcaFallidoYLaPersonaRecibeElAviso() {
        dadoUnMensajeReclamadoQueNuncaTermino();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesElMensajeQuedaFallidoYLaPersonaRecibeElAviso();
    }

    @Test
    @DisplayName("Si el número se desvinculó mientras esperaba en la cola, recibe las instrucciones")
    void siElNumeroSeDesvinculoMientrasEsperabaEnLaColaRecibeLasInstrucciones() {
        dadoUnNumeroDesvinculadoConUnMensajeEnCola();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesLaPersonaRecibeLasInstruccionesSinLlamarALaIA();
    }

    @Test
    @DisplayName("Una nota de voz recibe la respuesta de que por ahora sólo se entienden textos y fotos")
    void unaNotaDeVozRecibeLaRespuestaDeQuePorAhoraSoloSeEntiendenTextosYFotos() {
        dadoUnaNotaDeVozEnLaCola();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeAvisaQueSoloSeEntiendenTextosYFotos();
    }

    // --- texto ---

    @Test
    @DisplayName("Un texto claro arma una propuesta pendiente con el resumen y descuenta una consulta de Sami")
    void unTextoClaroArmaUnaPropuestaPendienteConElResumenYDescuentaUnaConsultaDeSami() {
        dadoUnTextoClaroQueLaIAEntiendeConSeguridad();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeArmaLaPropuestaPendienteConSuResumenYSeDescuentaUnaConsulta();
    }

    @Test
    @DisplayName("Si la IA no está segura de la tabla, ofrece la lista numerada con la sugerida primero")
    void siLaIANoEstaSeguraDeLaTablaOfreceLaListaNumeradaConLaSugeridaPrimero() {
        dadoUnTextoQueLaIANoSabeEnQueTablaVa();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeOfreceLaListaNumeradaConLaSugeridaPrimero();
    }

    @Test
    @DisplayName("Si ninguna tabla corresponde pide aclaración y no guarda ninguna propuesta")
    void siNingunaTablaCorrespondePideAclaracionYNoGuardaNingunaPropuesta() {
        dadoUnTextoQueNingunaTablaExplica();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSePideAclaracionSinGuardarPropuestaYSeCobraLaConsulta();
    }

    @Test
    @DisplayName("Con la IA no habilitada para el dueño responde el motivo y no llama a la IA")
    void conLaIANoHabilitadaParaElDuenoRespondeElMotivoYNoLlamaALaIA() {
        dadoUnDuenoSinIAHabilitada();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeRespondeElMotivoSinLlamarALaIA();
    }

    @Test
    @DisplayName("Con el límite diario de Sami agotado responde el motivo y no llama a la IA")
    void conElLimiteDiarioDeSamiAgotadoRespondeElMotivoYNoLlamaALaIA() {
        dadoUnDiaConElLimiteDeSamiAgotado();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeRespondeElMotivoSinLlamarALaIAYElMensajeQuedaAtendido();
    }

    @Test
    @DisplayName("Un negocio sin tablas recibe el aviso de que arme su sistema primero")
    void unNegocioSinTablasRecibeElAvisoDeQueArmeSuSistemaPrimero() {
        dadoUnNegocioSinTablas();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSePideArmarElSistemaPrimero();
    }

    @Test
    @DisplayName("Un error inesperado deja el mensaje fallido, avisa a la persona y no se reintenta")
    void unErrorInesperadoDejaElMensajeFallidoAvisaALaPersonaYNoSeReintenta() {
        dadoUnProveedorDeIAQueFalla();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesElMensajeQuedaFallidoYSeAvisaALaPersona();
    }

    // --- confirmación ---

    @Test
    @DisplayName("Responder sí guarda las filas al final de la tabla, marcadas como cargadas por WhatsApp")
    void responderSiGuardaLasFilasAlFinalDeLaTablaMarcadasComoCargadasPorWhatsApp() {
        dadoUnaPropuestaPendienteDeDosGastosYUnSi();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeGuardanLasFilasAlFinalMarcadasComoCargadasPorWhatsApp();
    }

    @Test
    @DisplayName("Si una fila no cumple las reglas del campo no guarda nada y dice cuál campo está mal")
    void siUnaFilaNoCumpleLasReglasDelCampoNoGuardaNadaYDiceCualCampoEstaMal() {
        dadoUnaPropuestaPendienteConUnMontoInvalido();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesNoSeGuardaNadaYSeDiceCualCampoEstaMal();
    }

    @Test
    @DisplayName("Si el plan no admite más registros responde la restricción y no guarda")
    void siElPlanNoAdmiteMasRegistrosRespondeLaRestriccionYNoGuarda() {
        dadoUnPlanQueYaNoAdmiteMasRegistros();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeRespondeLaRestriccionSinGuardar();
    }

    @Test
    @DisplayName("Si la tabla ya no existe al confirmar lo avisa y no guarda")
    void siLaTablaYaNoExisteAlConfirmarLoAvisaYNoGuarda() {
        dadoUnaPropuestaPendienteDeUnaTablaBorrada();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeAvisaQueLaTablaNoExisteSinGuardar();
    }

    @Test
    @DisplayName("Responder no descarta la propuesta sin guardar nada")
    void responderNoDescartaLaPropuestaSinGuardarNada() {
        dadoUnaPropuestaPendienteYUnNo();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeDescartaLaPropuestaSinGuardarNada();
    }

    @Test
    @DisplayName("Un mensaje nuevo mientras hay una propuesta abierta la descarta, lo avisa y atiende el mensaje")
    void unMensajeNuevoMientrasHayUnaPropuestaAbiertaLaDescartaLoAvisaYAtiendeElMensaje() {
        dadoUnaPropuestaPendienteYUnTextoNuevo();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeDescartaLaAnteriorSeAvisaYSeInterpretaElTextoNuevo();
    }

    @Test
    @DisplayName("Responder sí a una propuesta vencida avisa que venció y no guarda")
    void responderSiAUnaPropuestaVencidaAvisaQueVencioYNoGuarda() {
        dadoUnaPropuestaVencidaYUnSi();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeAvisaQueVencioSinGuardar();
    }

    @Test
    @DisplayName("Un texto nuevo con una propuesta vencida la cierra y se atiende como mensaje nuevo")
    void unTextoNuevoConUnaPropuestaVencidaLaCierraYSeAtiendeComoMensajeNuevo() {
        dadoUnaPropuestaVencidaYUnTextoNuevo();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeCierraLaVencidaSinAvisoDeDescarteYSeInterpretaElTexto();
    }

    @Test
    @DisplayName("Elegir la tabla por su número vuelve a interpretar el texto con esa tabla y queda pendiente")
    void elegirLaTablaPorSuNumeroVuelveAInterpretarElTextoConEsaTablaYQuedaPendiente() {
        dadoUnaListaDeTablasYElNumeroDeGastos();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeInterpretaElTextoConEsaTablaYQuedaPendiente();
    }

    @Test
    @DisplayName("Elegir un número que no está en la lista lo pide de nuevo y mantiene la propuesta")
    void elegirUnNumeroQueNoEstaEnLaListaLoPideDeNuevoYMantieneLaPropuesta() {
        dadoUnaListaDeTablasYUnNumeroFueraDeLista();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSePideElegirDeNuevoYLaPropuestaSigueEligiendo();
    }

    // --- fotos ---

    @Test
    @DisplayName("Una foto cuya estructura coincide con una tabla arma la propuesta y marca lo dudoso con (?)")
    void unaFotoCuyaEstructuraCoincideConUnaTablaArmaLaPropuestaYMarcaLoDudosoCon() {
        dadoUnaFotoCuyaEstructuraCoincideConUnaTabla();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeArmaLaPropuestaYSeMarcaLoDudosoConSignoDePregunta();
    }

    @Test
    @DisplayName("Una foto que no se parece a ninguna tabla ofrece la lista y guarda la lectura para no leerla de nuevo")
    void unaFotoQueNoSePareceANingunaTablaOfreceLaListaYGuardaLaLecturaParaNoLeerlaDeNuevo() {
        dadoUnaFotoQueNoSePareceANingunaTabla();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeOfreceLaListaYSeGuardaLaLecturaParaNoLeerlaDeNuevo();
    }

    @Test
    @DisplayName("Elegir la tabla de una foto la asigna sin volver a leerla ni cobrar otra página")
    void elegirLaTablaDeUnaFotoLaAsignaSinVolverALeerlaNiCobrarOtraPagina() {
        dadoUnaListaDeTablasParaUnaFotoYElNumeroDeGastos();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesLaFotoSeAsignaSinLeerlaDeNuevoNiCobrarOtraPagina();
    }

    @Test
    @DisplayName("Si el pie de la foto nombra una tabla, la lectura se hace contra esa tabla")
    void siElPieDeLaFotoNombraUnaTablaLaLecturaSeHaceContraEsaTabla() {
        dadoUnaFotoConElNombreDeUnaTablaEnElPie();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesLaLecturaSeHaceContraEsaTabla();
    }

    @Test
    @DisplayName("Si la foto no se puede descargar no se cobra la página y se pide mandarla de nuevo")
    void siLaFotoNoSePuedeDescargarNoSeCobraLaPaginaYSePideMandarlaDeNuevo() {
        dadoUnaFotoQueNoSePuedeDescargar();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesNoSeCobraLaPaginaYSePideMandarlaDeNuevo();
    }

    @Test
    @DisplayName("Con el cupo mensual de fotos agotado responde la restricción y no llama a la IA")
    void conElCupoMensualDeFotosAgotadoRespondeLaRestriccionYNoLlamaALaIA() {
        dadoUnCupoMensualDeFotosAgotado();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeRespondeLaRestriccionSinLlamarALaIA();
    }

    @Test
    @DisplayName("Una foto que no es JPG, PNG ni WEBP se rechaza antes de cobrar")
    void unaFotoQueNoEsJPGPNGNiWEBPSeRechazaAntesDeCobrar() {
        dadoUnaFotoQueNoEsJpgPngNiWebp();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeRechazaAntesDeCobrar();
    }

    // --- ramas restantes ---

    @Test
    @DisplayName("Una foto con una propuesta abierta la descarta y lo avisa antes de leer la foto nueva")
    void unaFotoConUnaPropuestaAbiertaLaDescartaYLoAvisaAntesDeLeerLaFotoNueva() {
        dadoUnaPropuestaAbiertaYUnaFotoNueva();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeDescartaLaAnteriorYSeAvisaAntesDeLeerLaFoto();
    }

    @Test
    @DisplayName("Una nota de voz con una propuesta vencida no se toma como respuesta")
    void unaNotaDeVozConUnaPropuestaVencidaNoSeTomaComoRespuesta() {
        dadoUnaNotaDeVozConUnaPropuestaVencida();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeAvisaQueSoloSeEntiendenTextosYFotosYLaPropuestaQuedaVencida();
    }

    @Test
    @DisplayName("Un número mientras hay una carga pendiente es un mensaje nuevo, no la elección de una tabla")
    void unNumeroMientrasHayUnaCargaPendienteEsUnMensajeNuevoNoLaEleccionDeUnaTabla() {
        dadoUnaCargaPendienteYUnNumeroSuelto();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeDescartaLaCargaYSeInterpretaElNumeroComoMensajeNuevo();
    }

    @Test
    @DisplayName("Una tabla sin filas o con una fila sin datos cargables pide aclaración")
    void unaTablaSinFilasOConUnaFilaSinDatosCargablesPideAclaracion() {
        dadoUnaTablaSinFilasYOtraConUnaFilaSinDatosCargables();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSePideAclaracionEnLosDosCasosSinCobrarConsumo();
    }

    @Test
    @DisplayName("Una foto cuyas filas no traen datos cargables pide aclaración en lugar de proponer")
    void unaFotoCuyasFilasNoTraenDatosCargablesPideAclaracionEnLugarDeProponer() {
        dadoUnaFotoCuyasFilasNoTraenDatosCargables();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSePideAclaracionEnLugarDeProponer();
    }

    @Test
    @DisplayName("Un pie que nombra dos tablas o está en blanco no elige ninguna")
    void unPieQueNombraDosTablasOEstaEnBlancoNoEligeNinguna() {
        dadoUnaFotoConUnPieQueNombraDosTablasYOtraConPieEnBlanco();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesLaLecturaSeHaceSinElegirNingunaTabla();
    }

    @Test
    @DisplayName("Elegir una tabla que ya no existe lo avisa")
    void elegirUnaTablaQueYaNoExisteLoAvisa() {
        dadoUnaListaConUnaTablaQueYaNoExiste();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeAvisaQueLaTablaNoExiste();
    }

    @Test
    @DisplayName("Elegir una tabla cuyo texto no deja filas descarta la propuesta y pide aclaración")
    void elegirUnaTablaCuyoTextoNoDejaFilasDescartaLaPropuestaYPideAclaracion() {
        dadoUnaListaDeTablasYUnTextoQueNoDejaFilas();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeDescartaLaPropuestaYSePideAclaracion();
    }

    @Test
    @DisplayName("Las columnas calculadas o conectadas no se cargan por WhatsApp")
    void lasColumnasCalculadasOConectadasNoSeCarganPorWhatsApp() {
        dadoUnaTablaConColumnasCalculadasYConectadas();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSoloSeCargaLaColumnaDeTexto();
    }

    @Test
    @DisplayName("En una foto las columnas nuevas sin mapear se ignoran")
    void enUnaFotoLasColumnasNuevasSinMapearSeIgnoran() {
        dadoUnaFotoConUnaColumnaNuevaSinMapear();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesLaColumnaNuevaSeIgnora();
    }

    @Test
    @DisplayName("Un valor nulo en la respuesta de la IA se descarta")
    void unValorNuloEnLaRespuestaDeLaIASeDescarta() {
        dadoUnaRespuestaDeLaIAConUnValorNulo();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesElValorNuloSeDescarta();
    }

    @Test
    @DisplayName("En una foto una columna que corresponde a un campo calculado no se carga")
    void enUnaFotoUnaColumnaQueCorrespondeAUnCampoCalculadoNoSeCarga() {
        dadoUnaFotoConUnaColumnaQueCorrespondeAUnCampoCalculado();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesLaColumnaCalculadaNoSeCarga();
    }

    // --- actualizar filas existentes ---

    @Test
    @DisplayName("Si el producto ya existe, aunque lo escriban a medias, propone actualizarlo sin tocar su nombre")
    void siElProductoYaExisteAunqueLoEscribanAMediasProponeActualizarloSinTocarSuNombre() {
        dadoUnProductoExistenteEscritoAMedias();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeProponeActualizarloSinTocarSuNombre();
    }

    @Test
    @DisplayName("Confirmar una actualización cambia sólo los datos que vinieron y no agrega una fila nueva")
    void confirmarUnaActualizacionCambiaSoloLosDatosQueVinieronYNoAgregaUnaFilaNueva() {
        dadoUnaActualizacionPendienteDeUnProductoExistente();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeCambianSoloLosDatosQueVinieronSinAgregarUnaFilaNueva();
    }

    @Test
    @DisplayName("Una confirmación con filas nuevas y actualizadas hace las dos cosas y descuenta sólo las nuevas")
    void unaConfirmacionConFilasNuevasYActualizadasHaceLasDosCosasYDescuentaSoloLasNuevas() {
        dadoUnaCargaPendienteConUnaFilaNuevaYUnaActualizada();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeHacenLasDosCosasYSeDescuentaSoloLaNueva();
    }

    @Test
    @DisplayName("Si la fila a actualizar se borró o es de otra tabla, el dato se agrega como fila nueva")
    void siLaFilaAActualizarSeBorroOEsDeOtraTablaElDatoSeAgregaComoFilaNueva() {
        dadoUnaActualizacionPendienteDeUnaFilaBorrada();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesLosDatosSeAgreganComoFilaNueva();
    }

    @Test
    @DisplayName("Una actualización cuyo resultado no cumple las reglas del campo no guarda nada")
    void unaActualizacionCuyoResultadoNoCumpleLasReglasDelCampoNoGuardaNada() {
        dadoUnaActualizacionPendienteQueDejaUnMontoInvalido();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesNoSeGuardaNadaYSeExplicaElMonto();
    }

    @Test
    @DisplayName("Una tabla de movimientos (gastos) suma una fila nueva aunque ya haya otra con el mismo nombre")
    void unaTablaDeMovimientosGastosSumaUnaFilaNuevaAunqueYaHayaOtraConElMismoNombre() {
        dadoUnaTablaDeGastosConUnaFilaDelMismoNombre();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeProponeAgregarUnaFilaNuevaEnVezDeActualizar();
    }

    @Test
    @DisplayName("Si no hay un producto parecido, o hay dos igual de parecidos, se agrega una fila nueva")
    void siNoHayUnProductoParecidoOHayDosIgualDeParecidosSeAgregaUnaFilaNueva() {
        dadoDosProductosIgualDeParecidosYUnoSinParecido();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesTodasLasFilasSeAgreganComoNuevas();
    }

    @Test
    @DisplayName("Un dato sin nombre de producto, o sólo con el nombre, queda como fila nueva")
    void unDatoSinNombreDeProductoOSoloConElNombreQuedaComoFilaNueva() {
        dadoUnDatoSinNombreYOtroSoloConElNombre();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesAmbosQuedanComoFilasNuevas();
    }

    @Test
    @DisplayName("Una tabla sin columna de texto no tiene cómo nombrar sus filas y siempre agrega")
    void unaTablaSinColumnaDeTextoNoTieneComoNombrarSusFilasYSiempreAgrega() {
        dadoUnaTablaSinColumnaDeTexto();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesLaFilaSiempreSeAgregaComoNueva();
    }

    @Test
    @DisplayName("En una foto también actualiza el producto existente en lugar de sumar uno nuevo")
    void enUnaFotoTambienActualizaElProductoExistenteEnLugarDeSumarUnoNuevo() {
        dadoUnProductoExistenteYUnaFotoQueLoNombra();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesSeProponeActualizarElProductoExistente();
    }

    @Test
    @DisplayName("Al elegir la tabla de un texto también empareja con las filas existentes")
    void alElegirLaTablaDeUnTextoTambienEmparejaConLasFilasExistentes() {
        dadoUnaListaDeTablasYUnTextoQueNombraUnProductoExistente();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesLaFilaSeEmparejaConElProductoExistente();
    }

    @Test
    @DisplayName("Las filas existentes sin nombre se ignoran al buscar el producto")
    void lasFilasExistentesSinNombreSeIgnoranAlBuscarElProducto() {
        dadoUnaFilaExistenteSinNombreYOtraConNombre();

        cuandoSeAtiendeElSiguienteMensaje();

        entoncesLaFilaSinNombreSeIgnoraAlBuscarElProducto();
    }

    private Registro dadoFilaExistente(String nombre, String monto) {
        Registro registro = Registro.nuevo(NEGOCIO_ID, GASTOS_ID, new java.util.HashMap<>(Map.of(
                CONCEPTO_ID.toString(), nombre, MONTO_ID.toString(), new java.math.BigDecimal(monto))), AHORA);
        registro.setId(existentes.isEmpty() ? REGISTRO_ID : RegistroId.nuevo());
        existentes.add(registro);
        when(registroRepository.findAllByBloqueIdAndNegocioId(GASTOS_ID, NEGOCIO_ID)).thenReturn(existentes);
        return registro;
    }

    // --- dado (escenarios) ---

    private void dadoUnaColaSinMensajes() {
        when(mensajeRepository.reclamarSiguiente(any(), any())).thenReturn(Optional.empty());
    }

    private void dadoUnMensajeReclamadoQueNuncaTermino() {
        perdido = dadoTexto("hola");
        when(mensajeRepository.vencidos(any())).thenReturn(List.of(perdido));
        when(mensajeRepository.reclamarSiguiente(any(), any())).thenReturn(Optional.empty());
    }

    private void dadoUnNumeroDesvinculadoConUnMensajeEnCola() {
        when(vinculoRepository.findByTelefono(TELEFONO)).thenReturn(Optional.empty());
        mensaje = dadoReclamado(dadoTexto("gasté 5000"));
    }

    private void dadoUnaNotaDeVozEnLaCola() {
        dadoReclamado(MensajeWhatsapp.builder().negocioId(NEGOCIO_ID).telefono(TELEFONO).idExterno("A1")
                        .tipo(TipoMensajeWhatsapp.AUDIO).mimeMedia("audio/ogg").recibidoEn(AHORA).build());
    }

    private void dadoUnTextoClaroQueLaIAEntiendeConSeguridad() {
        mensaje = dadoReclamado(dadoTexto("gasté 5000 en harina"));
        dadoInterpretacion(GASTOS_ID, 0.9);
    }

    private void dadoUnTextoQueLaIANoSabeEnQueTablaVa() {
        dadoReclamado(dadoTexto("5000 de harina"));
        dadoInterpretacion(GASTOS_ID, 0.3);
    }

    private void dadoUnTextoQueNingunaTablaExplica() {
        dadoReclamado(dadoTexto("hola"));
        when(interpreteIa.interpretar(any())).thenReturn(InterpretacionRegistros.sinResultado(consumo()));
    }

    private void dadoUnDuenoSinIAHabilitada() {
        MensajeWhatsapp mensaje = dadoReclamado(dadoTexto("gasté 5000 en harina"));
        doThrow(new RestriccionPlanException("IA_NO_HABILITADA", "La IA no está habilitada para tu cuenta."))
                        .when(controlPlanService).exigirIa(NEGOCIO_ID);
    }

    private void dadoUnDiaConElLimiteDeSamiAgotado() {
        mensaje = dadoReclamado(dadoTexto("gasté 5000 en harina"));
        when(consumoSamiRepository.reservar(eq(NEGOCIO_ID), any(), eq(60), any())).thenReturn(false);
    }

    private void dadoUnNegocioSinTablas() {
        when(procesamientoRepository.tablas(NEGOCIO_ID)).thenReturn(List.of());
        dadoReclamado(dadoTexto("gasté 5000 en harina"));
    }

    private void dadoUnProveedorDeIAQueFalla() {
        mensaje = dadoReclamado(dadoTexto("gasté 5000 en harina"));
        when(interpreteIa.interpretar(any())).thenThrow(new IllegalStateException("se cayó el proveedor"));
    }

    private void dadoUnaPropuestaPendienteDeDosGastosYUnSi() {
        dadoReclamado(dadoTexto("Sí"));
        propuesta = dadoPendiente(List.of(
                        new FilaPropuesta(Map.of(CONCEPTO_ID.toString(), "harina", MONTO_ID.toString(), "5000"), Set.of()),
                        new FilaPropuesta(Map.of(CONCEPTO_ID.toString(), "huevos", MONTO_ID.toString(), "2500"), Set.of())));
        dadoTablaGuardable(3);
    }

    private void dadoUnaPropuestaPendienteConUnMontoInvalido() {
        dadoReclamado(dadoTexto("sí"));
        propuesta = dadoPendiente(List.of(
                        new FilaPropuesta(Map.of(CONCEPTO_ID.toString(), "harina", MONTO_ID.toString(), "mucho"), Set.of())));
        dadoTablaGuardable(0);
    }

    private void dadoUnPlanQueYaNoAdmiteMasRegistros() {
        dadoReclamado(dadoTexto("sí"));
        dadoPendiente(List.of(new FilaPropuesta(Map.of(MONTO_ID.toString(), "5000"), Set.of())));
        dadoTablaGuardable(0);
        doThrow(new RestriccionPlanException("LIMITE_REGISTROS", "Llegaste al límite de registros de tu plan"))
                        .when(controlPlanService).validarAltaRegistros(NEGOCIO_ID, 1, AHORA);
    }

    private void dadoUnaPropuestaPendienteDeUnaTablaBorrada() {
        dadoReclamado(dadoTexto("sí"));
        dadoPendiente(List.of(new FilaPropuesta(Map.of(MONTO_ID.toString(), "5000"), Set.of())));
        when(bloqueRepository.findByIdAndNegocioId(GASTOS_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private void dadoUnaPropuestaPendienteYUnNo() {
        dadoReclamado(dadoTexto("NO"));
        propuesta = dadoPendiente(List.of());
    }

    private void dadoUnaPropuestaPendienteYUnTextoNuevo() {
        dadoReclamado(dadoTexto("vendí 3 tortas"));
        anterior = dadoPendiente(List.of());
        dadoInterpretacion(GASTOS_ID, 0.9);
    }

    private void dadoUnaPropuestaVencidaYUnSi() {
        dadoReclamado(dadoTexto("sí"));
        vencida = PropuestaWhatsapp.pendiente(NEGOCIO_ID, TELEFONO, OrigenPropuestaWhatsapp.TEXTO,
                        "gasté", GASTOS_ID, List.of(), AHORA.minusHours(1));
        when(propuestaRepository.findAbiertaByTelefono(TELEFONO)).thenReturn(Optional.of(vencida));
    }

    private void dadoUnaPropuestaVencidaYUnTextoNuevo() {
        dadoReclamado(dadoTexto("gasté 5000 en harina"));
        vencida = PropuestaWhatsapp.pendiente(NEGOCIO_ID, TELEFONO, OrigenPropuestaWhatsapp.TEXTO,
                        "gasté", GASTOS_ID, List.of(), AHORA.minusHours(1));
        when(propuestaRepository.findAbiertaByTelefono(TELEFONO)).thenReturn(Optional.of(vencida));
        dadoInterpretacion(GASTOS_ID, 0.9);
    }

    private void dadoUnaListaDeTablasYElNumeroDeGastos() {
        dadoReclamado(dadoTexto("2"));
        eligiendo = PropuestaWhatsapp.eligiendoTabla(NEGOCIO_ID, TELEFONO,
                        OrigenPropuestaWhatsapp.TEXTO, "5000 de harina", null, List.of(VENTAS_ID, GASTOS_ID), AHORA);
        when(propuestaRepository.findAbiertaByTelefono(TELEFONO)).thenReturn(Optional.of(eligiendo));
        dadoInterpretacion(GASTOS_ID, 0.9);
    }

    private void dadoUnaListaDeTablasYUnNumeroFueraDeLista() {
        dadoReclamado(dadoTexto("7"));
        eligiendo = PropuestaWhatsapp.eligiendoTabla(NEGOCIO_ID, TELEFONO,
                        OrigenPropuestaWhatsapp.TEXTO, "5000", null, List.of(VENTAS_ID, GASTOS_ID), AHORA);
        when(propuestaRepository.findAbiertaByTelefono(TELEFONO)).thenReturn(Optional.of(eligiendo));
    }

    private void dadoUnaFotoCuyaEstructuraCoincideConUnaTabla() {
        dadoReclamado(dadoFoto(null));
        when(mensajeria.descargarMedia("F1")).thenReturn(new ArchivoWhatsapp(ImagenTestData.jpeg(), "image/jpeg"));
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(lectura("Gastos",
                        new FilaLeida(1, 1, Map.of("concepto", new DatoLeido("harina", 0.9),
                                "monto", new DatoLeido("5000", 0.3)))));
    }

    private void dadoUnaFotoQueNoSePareceANingunaTabla() {
        dadoReclamado(dadoFoto(null));
        when(mensajeria.descargarMedia("F1")).thenReturn(new ArchivoWhatsapp(ImagenTestData.jpeg(), "image/jpeg"));
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(lectura("Hoja",
                        new FilaLeida(1, 1, Map.of("concepto", new DatoLeido("harina", 0.9)))));
    }

    private void dadoUnaListaDeTablasParaUnaFotoYElNumeroDeGastos() {
        dadoReclamado(dadoTexto("1"));
        eligiendo = PropuestaWhatsapp.eligiendoTabla(NEGOCIO_ID, TELEFONO,
                        OrigenPropuestaWhatsapp.FOTO, null, lectura("Hoja", new FilaLeida(1, 1, Map.of("concepto",
                                new DatoLeido("harina", 0.9), "monto", new DatoLeido("5000", 0.9)))),
                        List.of(GASTOS_ID, VENTAS_ID), AHORA);
        when(propuestaRepository.findAbiertaByTelefono(TELEFONO)).thenReturn(Optional.of(eligiendo));
    }

    private void dadoUnaFotoConElNombreDeUnaTablaEnElPie() {
        dadoReclamado(dadoFoto("las ventas de hoy"));
        when(mensajeria.descargarMedia("F1")).thenReturn(new ArchivoWhatsapp(ImagenTestData.jpeg(), "image/jpeg"));
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(lectura("Ventas",
                        new FilaLeida(1, 1, Map.of("producto", new DatoLeido("torta", 0.9)))));
    }

    private void dadoUnaFotoQueNoSePuedeDescargar() {
        dadoReclamado(dadoFoto(null));
        when(mensajeria.descargarMedia("F1")).thenThrow(new IllegalStateException("sin archivo"));
    }

    private void dadoUnCupoMensualDeFotosAgotado() {
        dadoReclamado(dadoFoto(null));
        when(mensajeria.descargarMedia("F1")).thenReturn(new ArchivoWhatsapp(ImagenTestData.jpeg(), "image/jpeg"));
        doThrow(new RestriccionPlanException("LIMITE_IA_MENSUAL", "Llegaste al límite de análisis de foto de tu plan"))
                        .when(controlPlanService).reservarPaginaIaInmediata(NEGOCIO_ID, 100, AHORA);
    }

    private void dadoUnaFotoQueNoEsJpgPngNiWebp() {
        dadoReclamado(dadoFoto(null));
        when(mensajeria.descargarMedia("F1")).thenReturn(new ArchivoWhatsapp(new byte[]{1, 2, 3}, "image/gif"));
    }

    private void dadoUnaPropuestaAbiertaYUnaFotoNueva() {
        dadoReclamado(dadoFoto(null));
        anterior = dadoPendiente(List.of());
        when(mensajeria.descargarMedia("F1")).thenReturn(new ArchivoWhatsapp(ImagenTestData.jpeg(), "image/jpeg"));
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(lectura("Gastos",
                        new FilaLeida(1, 1, Map.of("concepto", new DatoLeido("harina", 0.9)))));
    }

    private void dadoUnaNotaDeVozConUnaPropuestaVencida() {
        dadoReclamado(MensajeWhatsapp.builder().negocioId(NEGOCIO_ID).telefono(TELEFONO).idExterno("A2")
                        .tipo(TipoMensajeWhatsapp.AUDIO).mimeMedia("audio/ogg").recibidoEn(AHORA).build());
        vencida = PropuestaWhatsapp.pendiente(NEGOCIO_ID, TELEFONO, OrigenPropuestaWhatsapp.TEXTO,
                        "gasté", GASTOS_ID, List.of(), AHORA.minusHours(1));
        when(propuestaRepository.findAbiertaByTelefono(TELEFONO)).thenReturn(Optional.of(vencida));
    }

    private void dadoUnaCargaPendienteYUnNumeroSuelto() {
        dadoReclamado(dadoTexto("2"));
        pendiente = dadoPendiente(List.of());
        dadoInterpretacion(GASTOS_ID, 0.9);
    }

    private void dadoUnaTablaSinFilasYOtraConUnaFilaSinDatosCargables() {
        dadoReclamado(dadoTexto("algo"));
        when(interpreteIa.interpretar(any())).thenReturn(new InterpretacionRegistros(GASTOS_ID, List.of(), 0.9, null));
        atender.execute();
        when(mensajeRepository.reclamarSiguiente(any(), any())).thenReturn(Optional.of(dadoTexto("otra")));
        when(interpreteIa.interpretar(any())).thenReturn(new InterpretacionRegistros(GASTOS_ID,
                        List.of(Map.of(PRODUCTO_AJENO_ID, "x", CONCEPTO_ID, " ")), 0.9, null));
    }

    private void dadoUnaFotoCuyasFilasNoTraenDatosCargables() {
        dadoReclamado(dadoFoto(null));
        when(mensajeria.descargarMedia("F1")).thenReturn(new ArchivoWhatsapp(ImagenTestData.jpeg(), "image/jpeg"));
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(lectura("Gastos",
                        new FilaLeida(1, 1, Map.of("concepto", new DatoLeido(" ", 0.9), "monto", new DatoLeido(null, 0.9)))));
    }

    private void dadoUnaFotoConUnPieQueNombraDosTablasYOtraConPieEnBlanco() {
        dadoReclamado(dadoFoto("gastos y ventas"));
        when(mensajeria.descargarMedia("F1")).thenReturn(new ArchivoWhatsapp(ImagenTestData.jpeg(), "image/jpeg"));
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(lectura("Gastos",
                        new FilaLeida(1, 1, Map.of("concepto", new DatoLeido("harina", 0.9)))));
        atender.execute();
        when(mensajeRepository.reclamarSiguiente(any(), any())).thenReturn(Optional.of(dadoFoto("   ")));
    }

    private void dadoUnaListaConUnaTablaQueYaNoExiste() {
        dadoReclamado(dadoTexto("1"));
        PropuestaWhatsapp eligiendo = PropuestaWhatsapp.eligiendoTabla(NEGOCIO_ID, TELEFONO,
                        OrigenPropuestaWhatsapp.TEXTO, "5000", null, List.of(BloqueId.nuevo()), AHORA);
        when(propuestaRepository.findAbiertaByTelefono(TELEFONO)).thenReturn(Optional.of(eligiendo));
    }

    private void dadoUnaListaDeTablasYUnTextoQueNoDejaFilas() {
        dadoReclamado(dadoTexto("1"));
        eligiendo = PropuestaWhatsapp.eligiendoTabla(NEGOCIO_ID, TELEFONO,
                        OrigenPropuestaWhatsapp.TEXTO, "5000", null, List.of(GASTOS_ID), AHORA);
        when(propuestaRepository.findAbiertaByTelefono(TELEFONO)).thenReturn(Optional.of(eligiendo));
        when(interpreteIa.interpretar(any())).thenReturn(InterpretacionRegistros.sinResultado(null));
    }

    private void dadoUnaTablaConColumnasCalculadasYConectadas() {
        TablaDisponible conCalculadas = new TablaDisponible(new BloqueLectura(GASTOS_ID, "Gastos", List.of(
                        new CampoConocido(CONCEPTO_ID, "Concepto", TipoCampo.TEXTO, List.of()),
                        new CampoConocido(MONTO_ID, "Total", TipoCampo.FORMULA, List.of()),
                        new CampoConocido(PRODUCTO_ID, "Cliente", TipoCampo.RELACION, List.of()))), "Mi panadería");
        when(procesamientoRepository.tablas(NEGOCIO_ID)).thenReturn(List.of(conCalculadas));
        dadoReclamado(dadoTexto("gasté 5000 en harina"));
        dadoInterpretacion(GASTOS_ID, 0.9);
    }

    private void dadoUnaFotoConUnaColumnaNuevaSinMapear() {
        dadoReclamado(dadoFoto(null));
        when(mensajeria.descargarMedia("F1")).thenReturn(new ArchivoWhatsapp(ImagenTestData.jpeg(), "image/jpeg"));
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(lectura("Gastos",
                        new FilaLeida(1, 1, Map.of("concepto", new DatoLeido("harina", 0.9),
                                "extra", new DatoLeido("algo", 0.9)))));
    }

    private void dadoUnaRespuestaDeLaIAConUnValorNulo() {
        dadoReclamado(dadoTexto("gasté 5000 en harina"));
        Map<CampoId, String> fila = new java.util.HashMap<>();
        fila.put(CONCEPTO_ID, "harina");
        fila.put(MONTO_ID, null);
        when(interpreteIa.interpretar(any())).thenReturn(new InterpretacionRegistros(GASTOS_ID, List.of(fila), 0.9,
                        consumo()));
    }

    private void dadoUnaFotoConUnaColumnaQueCorrespondeAUnCampoCalculado() {
        TablaDisponible conFormula = new TablaDisponible(new BloqueLectura(GASTOS_ID, "Gastos", List.of(
                        new CampoConocido(CONCEPTO_ID, "Concepto", TipoCampo.TEXTO, List.of()),
                        new CampoConocido(MONTO_ID, "Total", TipoCampo.FORMULA, List.of()))), "Mi panadería");
        when(procesamientoRepository.tablas(NEGOCIO_ID)).thenReturn(List.of(conFormula));
        dadoReclamado(dadoFoto("gastos"));
        when(mensajeria.descargarMedia("F1")).thenReturn(new ArchivoWhatsapp(ImagenTestData.jpeg(), "image/jpeg"));
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(new LecturaCuaderno(
                        TipoRegistroCuaderno.OTRO, new BloquePropuesto("Gastos", null, List.of(
                                new CampoPropuesto("concepto", "Concepto", TipoCampo.TEXTO, List.of()),
                                new CampoPropuesto("total", "Total", TipoCampo.NUMERO, List.of()))),
                        List.of(new FilaLeida(1, 1, Map.of("concepto", new DatoLeido("harina", 0.9),
                                "total", new DatoLeido("5000", 0.9)))), List.of(),
                        new ConsumoIa("simulado", "demo", "v1", 1, 10, 20, 30)));
    }

    private void dadoUnProductoExistenteEscritoAMedias() {
        dadoFilaExistente("Harina 000", "1000");
        dadoReclamado(dadoTexto("harina 5000"));
        when(interpreteIa.interpretar(any())).thenReturn(new InterpretacionRegistros(GASTOS_ID,
                        List.of(Map.of(CONCEPTO_ID, "harina", MONTO_ID, "5000")), 0.9, consumo()));
    }

    private void dadoUnaActualizacionPendienteDeUnProductoExistente() {
        dadoReclamado(dadoTexto("sí"));
        existente = dadoFilaExistente("Harina 000", "1000");
        dadoPendiente(List.of(new FilaPropuesta(Map.of(CONCEPTO_ID.toString(), "harina",
                        MONTO_ID.toString(), "5000"), Set.of()).actualizando(REGISTRO_ID.toString(), "Harina 000",
                        CONCEPTO_ID.toString(), Map.of())));
        dadoTablaGuardable(3);
        when(registroRepository.findByIdAndNegocioId(any(), eq(NEGOCIO_ID))).thenReturn(Optional.of(existente));
    }

    private void dadoUnaCargaPendienteConUnaFilaNuevaYUnaActualizada() {
        dadoReclamado(dadoTexto("sí"));
        Registro existente = dadoFilaExistente("Harina 000", "1000");
        dadoPendiente(List.of(new FilaPropuesta(Map.of(MONTO_ID.toString(), "5000"), Set.of())
                                .actualizando(REGISTRO_ID.toString(), "Harina 000", CONCEPTO_ID.toString(), Map.of()),
                        new FilaPropuesta(Map.of(CONCEPTO_ID.toString(), "azúcar", MONTO_ID.toString(), "800"), Set.of())));
        dadoTablaGuardable(3);
        when(registroRepository.findByIdAndNegocioId(any(), eq(NEGOCIO_ID))).thenReturn(Optional.of(existente));
    }

    private void dadoUnaActualizacionPendienteDeUnaFilaBorrada() {
        dadoReclamado(dadoTexto("sí"));
        Registro deOtraTabla = Registro.nuevo(NEGOCIO_ID, VENTAS_ID, Map.of(), AHORA);
        dadoPendiente(List.of(new FilaPropuesta(Map.of(CONCEPTO_ID.toString(), "harina", MONTO_ID.toString(), "5000"),
                        Set.of()).actualizando(REGISTRO_ID.toString(), "Harina 000", CONCEPTO_ID.toString(), Map.of())));
        dadoTablaGuardable(0);
        when(registroRepository.findByIdAndNegocioId(any(), eq(NEGOCIO_ID))).thenReturn(Optional.empty(),
                        Optional.of(deOtraTabla));
    }

    private void dadoUnaActualizacionPendienteQueDejaUnMontoInvalido() {
        dadoReclamado(dadoTexto("sí"));
        Registro existente = dadoFilaExistente("Harina 000", "1000");
        dadoPendiente(List.of(new FilaPropuesta(Map.of(MONTO_ID.toString(), "mucho"), Set.of())
                        .actualizando(REGISTRO_ID.toString(), "Harina 000", CONCEPTO_ID.toString(), Map.of())));
        dadoTablaGuardable(0);
        when(registroRepository.findByIdAndNegocioId(any(), eq(NEGOCIO_ID))).thenReturn(Optional.of(existente));
    }

    private void dadoUnaTablaDeGastosConUnaFilaDelMismoNombre() {
        TablaDisponible movimientos = new TablaDisponible(gastos.bloque(), "Mi panadería",
                        com.ensambla.backend.domain.model.bloque.TipoBloque.GASTOS);
        when(procesamientoRepository.tablas(NEGOCIO_ID)).thenReturn(List.of(movimientos));
        dadoFilaExistente("harina", "1000");
        dadoReclamado(dadoTexto("gasté 5000 en harina"));
        dadoInterpretacion(GASTOS_ID, 0.9);
    }

    private void dadoDosProductosIgualDeParecidosYUnoSinParecido() {
        dadoFilaExistente("Harina 000", "1000");
        dadoFilaExistente("Harina 0000", "1200");
        dadoReclamado(dadoTexto("harina 5000"));
        when(interpreteIa.interpretar(any())).thenReturn(new InterpretacionRegistros(GASTOS_ID,
                        List.of(Map.of(CONCEPTO_ID, "harina", MONTO_ID, "5000"), Map.of(CONCEPTO_ID, "manteca",
                                MONTO_ID, "900")), 0.9, consumo()));
    }

    private void dadoUnDatoSinNombreYOtroSoloConElNombre() {
        dadoFilaExistente("Harina 000", "1000");
        dadoReclamado(dadoTexto("cosas"));
        when(interpreteIa.interpretar(any())).thenReturn(new InterpretacionRegistros(GASTOS_ID,
                        List.of(Map.of(MONTO_ID, "700"), Map.of(CONCEPTO_ID, "harina")), 0.9, consumo()));
    }

    private void dadoUnaTablaSinColumnaDeTexto() {
        TablaDisponible sinTexto = new TablaDisponible(new BloqueLectura(GASTOS_ID, "Solo montos", List.of(
                        new CampoConocido(MONTO_ID, "Monto", TipoCampo.MONEDA, List.of()))), "Mi panadería");
        when(procesamientoRepository.tablas(NEGOCIO_ID)).thenReturn(List.of(sinTexto));
        dadoReclamado(dadoTexto("5000"));
        dadoInterpretacion(GASTOS_ID, 0.9);
    }

    private void dadoUnProductoExistenteYUnaFotoQueLoNombra() {
        dadoFilaExistente("Harina 000", "1000");
        dadoReclamado(dadoFoto("gastos"));
        when(mensajeria.descargarMedia("F1")).thenReturn(new ArchivoWhatsapp(ImagenTestData.jpeg(), "image/jpeg"));
        when(lectorCuadernoPort.leer(any(), any(), any())).thenReturn(lectura("Gastos",
                        new FilaLeida(1, 1, Map.of("concepto", new DatoLeido("harina", 0.9),
                                "monto", new DatoLeido("5000", 0.9)))));
    }

    private void dadoUnaListaDeTablasYUnTextoQueNombraUnProductoExistente() {
        dadoFilaExistente("Harina 000", "1000");
        dadoReclamado(dadoTexto("1"));
        eligiendo = PropuestaWhatsapp.eligiendoTabla(NEGOCIO_ID, TELEFONO,
                        OrigenPropuestaWhatsapp.TEXTO, "harina 5000", null, List.of(GASTOS_ID), AHORA);
        when(propuestaRepository.findAbiertaByTelefono(TELEFONO)).thenReturn(Optional.of(eligiendo));
        when(interpreteIa.interpretar(any())).thenReturn(new InterpretacionRegistros(GASTOS_ID,
                        List.of(Map.of(CONCEPTO_ID, "harina", MONTO_ID, "5000")), 0.9, consumo()));
    }

    private void dadoUnaFilaExistenteSinNombreYOtraConNombre() {
        Registro sinNombre = Registro.nuevo(NEGOCIO_ID, GASTOS_ID, Map.of(MONTO_ID.toString(), 10), AHORA);
        sinNombre.setId(RegistroId.nuevo());
        existentes.add(sinNombre);
        dadoFilaExistente("Harina 000", "1000");
        dadoReclamado(dadoTexto("harina 5000"));
        when(interpreteIa.interpretar(any())).thenReturn(new InterpretacionRegistros(GASTOS_ID,
                        List.of(Map.of(CONCEPTO_ID, "harina", MONTO_ID, "5000")), 0.9, consumo()));
    }

    // --- cuando ---

    private void cuandoSeAtiendeElSiguienteMensaje() {
        resultado = atender.execute();
    }

    // --- entonces (verificaciones) ---

    private void entoncesNoSeHaceNadaYSeAvisaQueNoHabia() {
        assertThat(resultado).isFalse();
        verifyNoInteractions(mensajeria, interpreteIa);
    }

    private void entoncesElMensajeQuedaFallidoYLaPersonaRecibeElAviso() {
        assertThat(perdido.getEstado()).isEqualTo(EstadoMensajeWhatsapp.FALLIDO);
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.ERROR_TEMPORAL);
    }

    private void entoncesLaPersonaRecibeLasInstruccionesSinLlamarALaIA() {
        assertThat(resultado).isTrue();
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.NO_VINCULADO);
        assertThat(mensaje.getEstado()).isEqualTo(EstadoMensajeWhatsapp.ATENDIDO);
        verifyNoInteractions(interpreteIa);
    }

    private void entoncesSeAvisaQueSoloSeEntiendenTextosYFotos() {
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.SOLO_TEXTO_Y_FOTOS);
    }

    private void entoncesSeArmaLaPropuestaPendienteConSuResumenYSeDescuentaUnaConsulta() {
        PropuestaWhatsapp propuesta = entoncesPropuestaGuardada();
        assertThat(propuesta.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.PENDIENTE);
        assertThat(propuesta.getBloqueId()).isEqualTo(GASTOS_ID);
        assertThat(propuesta.getFilas().getFirst().valores())
                        .containsEntry(MONTO_ID.toString(), "5000").containsEntry(CONCEPTO_ID.toString(), "harina");
        assertThat(entoncesRespuesta()).contains("Para *Gastos* (Mi panadería) entendí:", "Monto: 5000", "SÍ");
        assertThat(mensaje.getEstado()).isEqualTo(EstadoMensajeWhatsapp.ATENDIDO);
        verify(consumoSamiRepository).registrar(eq(NEGOCIO_ID), any(), any(ConsumoIaSami.class), any());
    }

    private void entoncesSeOfreceLaListaNumeradaConLaSugeridaPrimero() {
        PropuestaWhatsapp propuesta = entoncesPropuestaGuardada();
        assertThat(propuesta.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.ELIGIENDO_TABLA);
        assertThat(propuesta.getOpciones()).containsExactly(GASTOS_ID, VENTAS_ID);
        assertThat(entoncesRespuesta()).contains("Respondé con el número", "1) Mi panadería › Gastos");
    }

    private void entoncesSePideAclaracionSinGuardarPropuestaYSeCobraLaConsulta() {
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.NO_ENTENDI);
        verify(propuestaRepository, never()).save(any());
        verify(consumoSamiRepository).registrar(eq(NEGOCIO_ID), any(), any(ConsumoIaSami.class), any());
    }

    private void entoncesSeRespondeElMotivoSinLlamarALaIA() {
        verify(mensajeria).enviarTexto(TELEFONO, "La IA no está habilitada para tu cuenta.");
        verifyNoInteractions(interpreteIa);
        verify(consumoSamiRepository, never()).reservar(any(), any(), anyInt(), any());
    }

    private void entoncesSeRespondeElMotivoSinLlamarALaIAYElMensajeQuedaAtendido() {
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.LIMITE_SAMI);
        verifyNoInteractions(interpreteIa);
        assertThat(mensaje.getEstado()).isEqualTo(EstadoMensajeWhatsapp.ATENDIDO);
    }

    private void entoncesSePideArmarElSistemaPrimero() {
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.SIN_TABLAS);
    }

    private void entoncesElMensajeQuedaFallidoYSeAvisaALaPersona() {
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.ERROR_TEMPORAL);
        assertThat(mensaje.getEstado()).isEqualTo(EstadoMensajeWhatsapp.FALLIDO);
        assertThat(mensaje.getError()).contains("IllegalStateException", "se cayó el proveedor");
    }

    private void entoncesSeGuardanLasFilasAlFinalMarcadasComoCargadasPorWhatsApp() {
        ArgumentCaptor<List<Registro>> captor = ArgumentCaptor.forClass(List.class);
        verify(registroRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(Registro::getOrden).containsExactly(3, 4);
        assertThat(captor.getValue()).extracting(Registro::getOrigen).containsOnly("WhatsApp");
        assertThat(captor.getValue().getFirst().getValores()).containsEntry(CONCEPTO_ID.toString(), "harina");
        assertThat(propuesta.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.CONFIRMADA);
        assertThat(entoncesRespuesta()).contains("guardé 2 filas en *Gastos*");
        verify(controlPlanService).validarAltaRegistros(NEGOCIO_ID, 2, AHORA);
    }

    private void entoncesNoSeGuardaNadaYSeDiceCualCampoEstaMal() {
        verify(registroRepository, never()).saveAll(any());
        assertThat(entoncesRespuesta()).contains("No guardé nada", "Monto (tiene que ser un número)");
        assertThat(propuesta.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.PENDIENTE);
    }

    private void entoncesSeRespondeLaRestriccionSinGuardar() {
        verify(registroRepository, never()).saveAll(any());
        verify(mensajeria).enviarTexto(TELEFONO, "Llegaste al límite de registros de tu plan");
    }

    private void entoncesSeAvisaQueLaTablaNoExisteSinGuardar() {
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.TABLA_INEXISTENTE);
        verify(registroRepository, never()).saveAll(any());
    }

    private void entoncesSeDescartaLaPropuestaSinGuardarNada() {
        assertThat(propuesta.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.DESCARTADA);
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.DESCARTADO);
        verify(registroRepository, never()).saveAll(any());
    }

    private void entoncesSeDescartaLaAnteriorSeAvisaYSeInterpretaElTextoNuevo() {
        assertThat(anterior.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.DESCARTADA);
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.DESCARTE_ANTERIOR);
        verify(interpreteIa).interpretar(any());
    }

    private void entoncesSeAvisaQueVencioSinGuardar() {
        assertThat(vencida.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.VENCIDA);
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.VENCIDA);
        verify(registroRepository, never()).saveAll(any());
    }

    private void entoncesSeCierraLaVencidaSinAvisoDeDescarteYSeInterpretaElTexto() {
        assertThat(vencida.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.VENCIDA);
        verify(mensajeria, never()).enviarTexto(TELEFONO, TextosWhatsapp.DESCARTE_ANTERIOR);
        verify(interpreteIa).interpretar(any());
    }

    private void entoncesSeInterpretaElTextoConEsaTablaYQuedaPendiente() {
        ArgumentCaptor<com.ensambla.backend.domain.model.whatsapp.ConsultaInterpretacion> consulta =
                        ArgumentCaptor.forClass(com.ensambla.backend.domain.model.whatsapp.ConsultaInterpretacion.class);
        verify(interpreteIa).interpretar(consulta.capture());
        assertThat(consulta.getValue().destino()).isEqualTo(GASTOS_ID);
        assertThat(eligiendo.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.PENDIENTE);
        assertThat(entoncesRespuesta()).contains("Para *Gastos*");
    }

    private void entoncesSePideElegirDeNuevoYLaPropuestaSigueEligiendo() {
        verify(mensajeria).enviarTexto(TELEFONO, "Elegí un número de la lista");
        assertThat(eligiendo.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.ELIGIENDO_TABLA);
    }

    private void entoncesSeArmaLaPropuestaYSeMarcaLoDudosoConSignoDePregunta() {
        verify(controlPlanService).reservarPaginaIaInmediata(NEGOCIO_ID, 100, AHORA);
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.LEYENDO_FOTO);
        PropuestaWhatsapp propuesta = entoncesPropuestaGuardada();
        assertThat(propuesta.getOrigen()).isEqualTo(OrigenPropuestaWhatsapp.FOTO);
        assertThat(propuesta.getFilas().getFirst().dudosos()).containsExactly(MONTO_ID.toString());
        assertThat(entoncesRespuesta()).contains("Monto: 5000 (?)", "(?) = dato que no pude leer bien.");
    }

    private void entoncesSeOfreceLaListaYSeGuardaLaLecturaParaNoLeerlaDeNuevo() {
        PropuestaWhatsapp propuesta = entoncesPropuestaGuardada();
        assertThat(propuesta.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.ELIGIENDO_TABLA);
        assertThat(propuesta.getLectura()).isNotNull();
        assertThat(propuesta.getOpciones()).containsExactly(GASTOS_ID, VENTAS_ID);
    }

    private void entoncesLaFotoSeAsignaSinLeerlaDeNuevoNiCobrarOtraPagina() {
        assertThat(eligiendo.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.PENDIENTE);
        assertThat(eligiendo.getBloqueId()).isEqualTo(GASTOS_ID);
        verifyNoInteractions(lectorCuadernoPort, controlPlanService);
    }

    private void entoncesLaLecturaSeHaceContraEsaTabla() {
        verify(lectorCuadernoPort).leer(any(), any(), eq(VENTAS_ID));
        assertThat(entoncesPropuestaGuardada().getBloqueId()).isEqualTo(VENTAS_ID);
    }

    private void entoncesNoSeCobraLaPaginaYSePideMandarlaDeNuevo() {
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.FOTO_NO_DESCARGADA);
        verifyNoInteractions(controlPlanService, lectorCuadernoPort);
    }

    private void entoncesSeRespondeLaRestriccionSinLlamarALaIA() {
        verify(mensajeria).enviarTexto(TELEFONO, "Llegaste al límite de análisis de foto de tu plan");
        verifyNoInteractions(lectorCuadernoPort);
    }

    private void entoncesSeRechazaAntesDeCobrar() {
        verify(mensajeria).enviarTexto(TELEFONO, "Elegí una imagen JPG, PNG o WEBP");
        verifyNoInteractions(controlPlanService);
    }

    private void entoncesSeDescartaLaAnteriorYSeAvisaAntesDeLeerLaFoto() {
        assertThat(anterior.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.DESCARTADA);
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.DESCARTE_ANTERIOR);
    }

    private void entoncesSeAvisaQueSoloSeEntiendenTextosYFotosYLaPropuestaQuedaVencida() {
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.SOLO_TEXTO_Y_FOTOS);
        assertThat(vencida.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.VENCIDA);
    }

    private void entoncesSeDescartaLaCargaYSeInterpretaElNumeroComoMensajeNuevo() {
        assertThat(pendiente.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.DESCARTADA);
        verify(interpreteIa).interpretar(any());
    }

    private void entoncesSePideAclaracionEnLosDosCasosSinCobrarConsumo() {
        verify(mensajeria, org.mockito.Mockito.times(2)).enviarTexto(TELEFONO, TextosWhatsapp.NO_ENTENDI);
        verify(consumoSamiRepository, never()).registrar(any(), any(), any(), any());
    }

    private void entoncesSePideAclaracionEnLugarDeProponer() {
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.NO_ENTENDI);
    }

    private void entoncesLaLecturaSeHaceSinElegirNingunaTabla() {
        verify(lectorCuadernoPort, org.mockito.Mockito.times(2)).leer(any(), any(),
                        org.mockito.ArgumentMatchers.isNull());
    }

    private void entoncesSeAvisaQueLaTablaNoExiste() {
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.TABLA_INEXISTENTE);
    }

    private void entoncesSeDescartaLaPropuestaYSePideAclaracion() {
        assertThat(eligiendo.getEstado()).isEqualTo(EstadoPropuestaWhatsapp.DESCARTADA);
        verify(mensajeria).enviarTexto(TELEFONO, TextosWhatsapp.NO_ENTENDI);
    }

    private void entoncesSoloSeCargaLaColumnaDeTexto() {
        assertThat(entoncesPropuestaGuardada().getFilas().getFirst().valores()).containsOnlyKeys(CONCEPTO_ID.toString());
    }

    private void entoncesLaColumnaNuevaSeIgnora() {
        assertThat(entoncesPropuestaGuardada().getFilas().getFirst().valores()).containsOnlyKeys(CONCEPTO_ID.toString());
    }

    private void entoncesElValorNuloSeDescarta() {
        assertThat(entoncesPropuestaGuardada().getFilas().getFirst().valores()).containsOnlyKeys(CONCEPTO_ID.toString());
    }

    private void entoncesLaColumnaCalculadaNoSeCarga() {
        assertThat(entoncesPropuestaGuardada().getFilas().getFirst().valores()).containsOnlyKeys(CONCEPTO_ID.toString());
    }

    private void entoncesSeProponeActualizarloSinTocarSuNombre() {
        FilaPropuesta fila = entoncesPropuestaGuardada().getFilas().getFirst();
        assertThat(fila.actualizaUnaFila()).isTrue();
        assertThat(fila.registroId()).isEqualTo(REGISTRO_ID.toString());
        assertThat(fila.nombreExistente()).isEqualTo("Harina 000");
        assertThat(entoncesRespuesta()).contains("1) Actualizo *Harina 000*: Monto: 5000").doesNotContain("Concepto");
    }

    private void entoncesSeCambianSoloLosDatosQueVinieronSinAgregarUnaFilaNueva() {
        assertThat(existente.getValores()).containsEntry(CONCEPTO_ID.toString(), "Harina 000")
                        .containsEntry(MONTO_ID.toString(), new java.math.BigDecimal("5000"));
        assertThat(existente.getActualizadoEn()).isEqualTo(AHORA);
        verify(registroRepository).saveAll(List.of(existente));
        verify(controlPlanService, never()).validarAltaRegistros(any(), org.mockito.ArgumentMatchers.anyLong(), any());
        assertThat(entoncesRespuesta()).contains("actualicé 1 fila en *Gastos*");
    }

    private void entoncesSeHacenLasDosCosasYSeDescuentaSoloLaNueva() {
        verify(controlPlanService).validarAltaRegistros(NEGOCIO_ID, 1, AHORA);
        assertThat(entoncesRespuesta()).contains("actualicé 1 y agregué 1 filas en *Gastos*");
    }

    private void entoncesLosDatosSeAgreganComoFilaNueva() {
        verify(controlPlanService).validarAltaRegistros(NEGOCIO_ID, 1, AHORA);
        assertThat(entoncesRespuesta()).contains("guardé 1 fila en *Gastos*");
    }

    private void entoncesNoSeGuardaNadaYSeExplicaElMonto() {
        verify(registroRepository, never()).saveAll(any());
        assertThat(entoncesRespuesta()).contains("No guardé nada", "Monto");
    }

    private void entoncesSeProponeAgregarUnaFilaNuevaEnVezDeActualizar() {
        assertThat(entoncesPropuestaGuardada().getFilas().getFirst().actualizaUnaFila()).isFalse();
        assertThat(entoncesRespuesta()).contains("1) Agrego:");
    }

    private void entoncesTodasLasFilasSeAgreganComoNuevas() {
        assertThat(entoncesPropuestaGuardada().getFilas()).noneMatch(FilaPropuesta::actualizaUnaFila);
    }

    private void entoncesAmbosQuedanComoFilasNuevas() {
        assertThat(entoncesPropuestaGuardada().getFilas()).noneMatch(FilaPropuesta::actualizaUnaFila);
    }

    private void entoncesLaFilaSiempreSeAgregaComoNueva() {
        assertThat(entoncesPropuestaGuardada().getFilas().getFirst().actualizaUnaFila()).isFalse();
    }

    private void entoncesSeProponeActualizarElProductoExistente() {
        assertThat(entoncesPropuestaGuardada().getFilas().getFirst().nombreExistente()).isEqualTo("Harina 000");
    }

    private void entoncesLaFilaSeEmparejaConElProductoExistente() {
        assertThat(eligiendo.getFilas().getFirst().actualizaUnaFila()).isTrue();
    }

    private void entoncesLaFilaSinNombreSeIgnoraAlBuscarElProducto() {
        assertThat(entoncesPropuestaGuardada().getFilas().getFirst().nombreExistente()).isEqualTo("Harina 000");
    }

    // --- datos y verificaciones comunes ---

    private MensajeWhatsapp dadoTexto(String texto) {
        return MensajeWhatsapp.builder().negocioId(NEGOCIO_ID).telefono(TELEFONO).idExterno("T1")
                .tipo(TipoMensajeWhatsapp.TEXTO).texto(texto).recibidoEn(AHORA).build();
    }

    private MensajeWhatsapp dadoFoto(String pie) {
        return MensajeWhatsapp.builder().negocioId(NEGOCIO_ID).telefono(TELEFONO).idExterno("F1")
                .tipo(TipoMensajeWhatsapp.IMAGEN).texto(pie).mimeMedia("image/jpeg").recibidoEn(AHORA).build();
    }

    private MensajeWhatsapp dadoReclamado(MensajeWhatsapp mensaje) {
        when(mensajeRepository.reclamarSiguiente(any(), any())).thenReturn(Optional.of(mensaje));
        return mensaje;
    }

    private void dadoInterpretacion(BloqueId bloqueId, double confianza) {
        when(interpreteIa.interpretar(any())).thenReturn(new InterpretacionRegistros(bloqueId,
                List.of(Map.of(CONCEPTO_ID, "harina", MONTO_ID, "5000")), confianza, consumo()));
    }

    private PropuestaWhatsapp dadoPendiente(List<FilaPropuesta> filas) {
        PropuestaWhatsapp propuesta = PropuestaWhatsapp.pendiente(NEGOCIO_ID, TELEFONO, OrigenPropuestaWhatsapp.TEXTO,
                "gasté", GASTOS_ID, filas, AHORA);
        when(propuestaRepository.findAbiertaByTelefono(TELEFONO)).thenReturn(Optional.of(propuesta));
        return propuesta;
    }

    private void dadoTablaGuardable(int ordenSiguiente) {
        Bloque bloque = Bloque.builder().id(GASTOS_ID).negocioId(NEGOCIO_ID).nombre("Gastos").build();
        when(bloqueRepository.findByIdAndNegocioId(GASTOS_ID, NEGOCIO_ID)).thenReturn(Optional.of(bloque));
        when(campoRepository.findByBloqueIdAndNegocioId(GASTOS_ID, NEGOCIO_ID)).thenReturn(List.of(
                Campo.builder().id(CONCEPTO_ID).nombre("Concepto").tipo(TipoCampo.TEXTO).posicion((short) 0).build(),
                Campo.builder().id(MONTO_ID).nombre("Monto").tipo(TipoCampo.MONEDA).posicion((short) 1).build()));
        when(registroRepository.siguienteOrden(GASTOS_ID, NEGOCIO_ID)).thenReturn(ordenSiguiente);
    }

    private static LecturaCuaderno lectura(String nombreTabla, FilaLeida... filas) {
        List<CampoPropuesto> campos = nombreTabla.equals("Ventas")
                ? List.of(new CampoPropuesto("producto", "Producto", TipoCampo.TEXTO, List.of()))
                : List.of(new CampoPropuesto("concepto", "Concepto", TipoCampo.TEXTO, List.of()),
                        new CampoPropuesto("monto", "Monto", TipoCampo.MONEDA, List.of()));
        return new LecturaCuaderno(TipoRegistroCuaderno.OTRO, new BloquePropuesto(nombreTabla, null, campos),
                List.of(filas), List.of(), new ConsumoIa("simulado", "demo", "v1", 1, 10, 20, 30));
    }

    private static ConsumoIaSami consumo() {
        return new ConsumoIaSami("simulado", "demo", 10, 20);
    }

    private PropuestaWhatsapp entoncesPropuestaGuardada() {
        ArgumentCaptor<PropuestaWhatsapp> captor = ArgumentCaptor.forClass(PropuestaWhatsapp.class);
        verify(propuestaRepository).save(captor.capture());
        return captor.getValue();
    }

    private String entoncesRespuesta() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mensajeria, org.mockito.Mockito.atLeastOnce()).enviarTexto(eq(TELEFONO), captor.capture());
        return String.join("\n", captor.getAllValues());
    }
}
