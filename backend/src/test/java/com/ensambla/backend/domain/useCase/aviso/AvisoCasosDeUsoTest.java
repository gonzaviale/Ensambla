package com.ensambla.backend.domain.useCase.aviso;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.aviso.AccionDeAviso;
import com.ensambla.backend.domain.model.aviso.AccionRegistrar;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.TipoDecision;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.objetivo.DecisionesObjetivoService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.domain.useCase.aviso.aplicarAviso.AplicarAviso;
import com.ensambla.backend.domain.useCase.aviso.contarAvisos.ContadorAvisos;
import com.ensambla.backend.domain.useCase.aviso.contarAvisos.ContarAvisos;
import com.ensambla.backend.domain.useCase.aviso.descartarAviso.DescartarAviso;
import com.ensambla.backend.domain.useCase.aviso.listarAvisos.ListarAvisos;
import com.ensambla.backend.domain.useCase.aviso.listarAvisos.ListarAvisosRequest;
import com.ensambla.backend.support.AvisoTestData;
import com.ensambla.backend.support.AvisosDePrueba;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ensambla.backend.support.AvisosDePrueba.AHORA;
import static com.ensambla.backend.support.AvisosDePrueba.AVISO_ID;
import static com.ensambla.backend.support.AvisosDePrueba.CAMPO_PRECIO_ID;
import static com.ensambla.backend.support.AvisosDePrueba.REGISTRO_ID;
import static com.ensambla.backend.support.AvisosDePrueba.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AvisoCasosDeUsoTest {

    private static final String SIN_TABLA = "La acción del aviso no indica en qué tabla crear la fila";
    private static final String SIN_REGISTRO = "La acción del aviso no indica qué registro cambiar";
    private static final String SIN_VALORES = "La acción del aviso no indica qué valores cambiar";
    private static final String SIN_CAMPOS = "La tabla ya no tiene los campos que el aviso quería cambiar";

    private static final NegocioId NEGOCIO_ID = AvisoTestData.NEGOCIO_ID;
    private static final Clock CLOCK = Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final AvisoRepository avisoRepository = mock(AvisoRepository.class, CALLS_REAL_METHODS);
    private final RegistroDatosService registroDatosService = mock(RegistroDatosService.class);
    private final RegistroRepository registroRepository = mock(RegistroRepository.class);
    private final CampoRepository campoRepository = mock(CampoRepository.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class, invocation -> {
        if (invocation.getMethod().getName().equals("avisosAutorizados")) {
            return invocation.getArgument(1);
        }
        return Answers.RETURNS_DEFAULTS.answer(invocation);
    });
    private final ListarAvisos listarAvisos =
            new ListarAvisos(negocioRepository, avisoRepository, controlPlanService, CLOCK);
    private final ContarAvisos contarAvisos = new ContarAvisos(negocioRepository, avisoRepository);
    private final DecisionesObjetivoService decisionesObjetivoService = mock(DecisionesObjetivoService.class);
    private final AplicarAviso aplicarAviso = new AplicarAviso(negocioRepository, controlPlanService,
            registroDatosService,
            registroRepository, campoRepository, avisoRepository, decisionesObjetivoService,
            new TransaccionDirecta(), CLOCK);
    private final DescartarAviso descartarAviso = new DescartarAviso(negocioRepository,
            controlPlanService, avisoRepository, decisionesObjetivoService, new TransaccionDirecta(), CLOCK);

    private Aviso aviso;
    private Registro registro;
    private List<Campo> campos;
    private List<AvisoItem> listado;
    private ContadorAvisos contador;
    private AvisoItem resultado;
    private Throwable error;

    @BeforeEach
    void dadoUnNegocioConUnaPersonaConectada() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(avisoRepository.save(any(Aviso.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // --- listar ---

    @Test
    @DisplayName("Listar pone los pendientes primero, del más urgente al menos urgente y del más nuevo al más viejo")
    void listarPoneLosPendientesPrimeroDelMasUrgenteAlMenosUrgenteYDelMasNuevoAlMasViejo() {
        dadoAvisosDeDistintaUrgenciaYAntiguedadIncluidoUnoDescartado();

        cuandoLaPersonaListaTodosLosAvisos();

        entoncesLosAvisosSeVenEnOrden("nuevo urgente", "viejo urgente", "poco urgente", "descartado");
    }

    @Test
    @DisplayName("Listar con estado devuelve solo los avisos de ese estado del negocio de la persona")
    void listarConEstadoDevuelveSoloLosAvisosDeEseEstadoDelNegocioDeLaPersona() {
        dadoUnAvisoPendienteEnElNegocio();

        cuandoLaPersonaListaLosAvisosPendientes();

        entoncesSoloVeElAvisoPendienteSinConsultarLosDemas();
    }

    // --- contar ---

    @Test
    @DisplayName("Contar devuelve los pendientes y cuántos son urgentes")
    void contarDevuelveLosPendientesYCuantosSonUrgentes() {
        dadoTresAvisosPendientesDeLosQueDosSonUrgentes();

        cuandoLaPersonaCuentaSusAvisos();

        entoncesHayTresPendientesYDosUrgentes();
    }

    // --- aplicar ---

    @Test
    @DisplayName("Aplicar un aviso lo guarda como aplicado con la hora del reloj")
    void aplicarUnAvisoLoGuardaAplicadoConLaHoraDelReloj() {
        dadoUnAvisoPendienteSinAcciones();

        cuandoLaPersonaAplicaElAviso();

        entoncesElAvisoQuedaAplicadoYGuardadoConLaHoraDelReloj();
    }

    @Test
    @DisplayName("Aplicar un aviso anota la decisión en los objetivos con el aviso ya guardado")
    void aplicarUnAvisoAnotaLaDecisionEnLosObjetivosConElAvisoYaGuardado() {
        dadoUnAvisoPendienteSinAcciones();

        cuandoLaPersonaAplicaElAviso();

        entoncesSeAnotaLaDecision(TipoDecision.AVISO_APLICADO);
    }

    @Test
    @DisplayName("Si no se puede anotar la decisión, el aviso tampoco queda aplicado")
    void siNoSePuedeAnotarLaDecisionElAvisoTampocoQuedaAplicado() {
        dadoUnAvisoPendienteCuyaDecisionNoSePuedeAnotar();

        cuandoLaPersonaIntentaAplicarElAviso();

        entoncesSeRechazaPorque(IllegalStateException.class, "falló");
    }

    @Test
    @DisplayName("Aplicar un aviso de otro negocio responde que no existe y no aplica nada")
    void aplicarUnAvisoDeOtroNegocioRespondeQueNoExisteYNoAplicaNada() {
        dadoUnAvisoQueEsDeOtroNegocio();

        cuandoLaPersonaIntentaAplicarElAviso();

        entoncesElAvisoNoExisteYNoSeGuardaNada();
    }

    @Test
    @DisplayName("Aplicar un aviso ya resuelto no ejecuta su acción ni lo guarda")
    void aplicarUnAvisoYaResueltoNoEjecutaSuAccionNiLoGuarda() {
        dadoUnAvisoYaDescartadoQueSugiereBajarElPrecioDeYerbaA(4200);

        cuandoLaPersonaIntentaAplicarElAviso();

        entoncesSeRechazaSinEjecutarLaAccionNiGuardarNada();
    }

    @Test
    @DisplayName("Aplicar un aviso de cambio de precio actualiza el precio del producto antes de guardar el aviso")
    void aplicarUnAvisoDeCambioDePrecioActualizaElPrecioDelProductoAntesDeGuardarElAviso() {
        dadoUnAvisoQueSugiereBajarElPrecioDeYerbaA(4200);

        cuandoLaPersonaAplicaElAviso();

        entoncesElPrecioDeLaYerbaSeCambiaAntesDeGuardarElAviso(4200);
    }

    @Test
    @DisplayName("Aplicar un aviso de crear un combo agrega la fila con origen de aviso antes de guardarlo")
    void aplicarUnAvisoDeCrearUnComboAgregaLaFilaConOrigenDeAvisoAntesDeGuardarElAviso() {
        dadoUnAvisoQueSugiereCrearUnComboEnUnInventarioConSusCampos();

        cuandoLaPersonaAplicaElAviso();

        entoncesSeCreaLaFilaDelComboConOrigenDeAvisoAntesDeGuardarElAviso();
    }

    @Test
    @DisplayName("Un aviso para crear una fila sin tabla válida se rechaza")
    void unAvisoParaCrearUnaFilaSinTablaValidaSeRechaza() {
        dadoUnAvisoConUnaAccionIncompletaDe("CREAR_REGISTRO", SIN_TABLA);

        cuandoLaPersonaIntentaAplicarElAviso();

        entoncesSeRechazaPorque(BusinessRuleException.class, SIN_TABLA);
    }

    @Test
    @DisplayName("Un aviso para crear una fila no la crea si la tabla ya no tiene los campos a cargar")
    void unAvisoParaCrearUnaFilaNoLaCreaSiLaTablaYaNoTieneLosCamposQueQueriaCargar() {
        dadoUnAvisoQueSugiereCrearUnComboEnUnaTablaSinCampos();

        cuandoLaPersonaIntentaAplicarElAviso();

        entoncesSeRechazaPorqueLaTablaPerdioSusCamposYNoSeGuardaNingunRegistro();
    }

    @Test
    @DisplayName("Un aviso que solo abre una pantalla no cambia ningún dato")
    void unAvisoQueSoloAbreUnaPantallaNoCambiaNingunDato() {
        dadoUnAvisoQueSoloAbreLaPantallaDeRegistrarEntradas();

        cuandoLaPersonaAplicaElAviso();

        entoncesNoSeCambiaNingunDato();
    }

    @Test
    @DisplayName("Un aviso para editar una fila que no es un id válido se rechaza")
    void unAvisoParaEditarUnaFilaQueNoEsUnIdValidoSeRechaza() {
        dadoUnAvisoConUnaAccionIncompletaDe("EDITAR_REGISTRO", SIN_REGISTRO);

        cuandoLaPersonaIntentaAplicarElAviso();

        entoncesSeRechazaPorque(BusinessRuleException.class, SIN_REGISTRO);
    }

    @Test
    @DisplayName("Un aviso para editar una fila sin valores se rechaza")
    void unAvisoParaEditarUnaFilaSinValoresSeRechaza() {
        dadoUnAvisoConUnaAccionIncompletaDe("EDITAR_REGISTRO", SIN_VALORES);

        cuandoLaPersonaIntentaAplicarElAviso();

        entoncesSeRechazaPorque(BusinessRuleException.class, SIN_VALORES);
    }

    @Test
    @DisplayName("Un aviso para editar una fila con valores vacíos se rechaza")
    void unAvisoParaEditarUnaFilaConValoresVaciosSeRechaza() {
        dadoUnAvisoQueEditaUnaFilaDeYerbaConValoresVacios();

        cuandoLaPersonaIntentaAplicarElAviso();

        entoncesSeRechazaPorque(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Un aviso que cambia un campo que ya no existe en la tabla se rechaza y no edita nada")
    void unAvisoQueCambiaUnCampoQueYaNoExisteEnLaTablaSeRechazaYNoEditaNada() {
        dadoUnAvisoQueCambiaElCostoDeYerbaPeroLaTablaNoTieneEseCampo();

        cuandoLaPersonaIntentaAplicarElAviso();

        entoncesSeRechazaPorqueLaTablaPerdioSusCamposYNoSeEditaNingunRegistro();
    }

    // --- descartar ---

    @Test
    @DisplayName("Descartar un aviso lo guarda como descartado con la hora del reloj")
    void descartarUnAvisoLoGuardaDescartadoConLaHoraDelReloj() {
        dadoUnAvisoPendienteSinAcciones();

        cuandoLaPersonaDescartaElAviso();

        entoncesElAvisoQuedaDescartadoYGuardadoConLaHoraDelReloj();
    }

    @Test
    @DisplayName("Descartar un aviso anota la decisión en los objetivos con el aviso ya guardado")
    void descartarUnAvisoAnotaLaDecisionEnLosObjetivosConElAvisoYaGuardado() {
        dadoUnAvisoPendienteSinAcciones();

        cuandoLaPersonaDescartaElAviso();

        entoncesSeAnotaLaDecision(TipoDecision.AVISO_DESCARTADO);
    }

    @Test
    @DisplayName("Descartar un aviso ya resuelto no anota ninguna decisión")
    void descartarUnAvisoYaResueltoNoAnotaNingunaDecision() {
        dadoUnAvisoYaAplicado();

        cuandoLaPersonaIntentaDescartarElAviso();

        entoncesSeRechazaSinAnotarNingunaDecision();
    }

    // --- dado ---

    private void dadoAvisosDeDistintaUrgenciaYAntiguedadIncluidoUnoDescartado() {
        when(avisoRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(List.of(
                avisoDe("descartado", 3, EstadoAviso.DESCARTADO, 5),
                avisoDe("poco urgente", 1, EstadoAviso.PENDIENTE, 5),
                avisoDe("viejo urgente", 3, EstadoAviso.PENDIENTE, 1),
                avisoDe("nuevo urgente", 3, EstadoAviso.PENDIENTE, 4)));
    }

    private void dadoUnAvisoPendienteEnElNegocio() {
        aviso = avisoDe("pendiente", 2, EstadoAviso.PENDIENTE, 1);
        avisosPendientesDelNegocio(aviso);
    }

    private void dadoTresAvisosPendientesDeLosQueDosSonUrgentes() {
        avisosPendientesDelNegocio(
                avisoDe("a", 3, EstadoAviso.PENDIENTE, 1),
                avisoDe("b", 2, EstadoAviso.PENDIENTE, 1),
                avisoDe("c", 1, EstadoAviso.PENDIENTE, 1));
    }

    private void dadoUnAvisoPendienteSinAcciones() {
        aviso = elNegocioGuarda(AvisosDePrueba.avisoPendienteConAcciones());
    }

    private void dadoUnAvisoPendienteCuyaDecisionNoSePuedeAnotar() {
        dadoUnAvisoPendienteSinAcciones();
        doThrow(new IllegalStateException("falló")).when(decisionesObjetivoService)
                .registrarAviso(any(Aviso.class), any(TipoDecision.class), any(OffsetDateTime.class));
    }

    private void dadoUnAvisoQueEsDeOtroNegocio() {
        when(avisoRepository.findByIdAndNegocioId(AVISO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private void dadoUnAvisoYaDescartadoQueSugiereBajarElPrecioDeYerbaA(int precio) {
        aviso = elNegocioGuarda(AvisosDePrueba.avisoPendienteConAcciones(cambiarPrecio(precio)));
        aviso.setEstado(EstadoAviso.DESCARTADO);
    }

    private void dadoUnAvisoYaAplicado() {
        dadoUnAvisoPendienteSinAcciones();
        aviso.setEstado(EstadoAviso.APLICADO);
    }

    private void dadoUnAvisoQueSugiereBajarElPrecioDeYerbaA(int precio) {
        aviso = elNegocioGuarda(AvisosDePrueba.avisoPendienteConAcciones(cambiarPrecio(precio)));
        registro = AvisoTestData.registro(REGISTRO_ID.valor(), "Yerba", 2, 5);
        campos = List.of(AvisoTestData.campo(CAMPO_PRECIO_ID.valor(), "price"),
                AvisoTestData.campo(AvisoTestData.CAMPO_NOMBRE_ID.valor(), "name"));
        laTablaDelInventarioTiene(registro, campos);
    }

    private void dadoUnAvisoQueSugiereCrearUnComboEnUnInventarioConSusCampos() {
        aviso = elNegocioGuarda(AvisosDePrueba.avisoPendienteConAcciones(crearCombo(
                Map.of("name", "Combo A + B", "quantity", 0, "price", 1710))));
        when(campoRepository.findByBloqueIdAndNegocioId(AvisoTestData.INVENTARIO_ID, NEGOCIO_ID))
                .thenReturn(AvisosDePrueba.camposDelInventario());
    }

    private void dadoUnAvisoQueSugiereCrearUnComboEnUnaTablaSinCampos() {
        aviso = elNegocioGuarda(AvisosDePrueba.avisoPendienteConAcciones(crearCombo(Map.of("name", "Combo"))));
    }

    private void dadoUnAvisoConUnaAccionIncompletaDe(String tipo, String motivo) {
        aviso = elNegocioGuarda(AvisosDePrueba.avisoPendienteConAcciones(
                AvisosDePrueba.accionIncompleta(tipo, motivo)));
    }

    private void dadoUnAvisoQueSoloAbreLaPantallaDeRegistrarEntradas() {
        aviso = elNegocioGuarda(AvisosDePrueba.avisoPendienteConAcciones(new AccionRegistrar(
                "Registrar entrada", AvisoTestData.PROYECTO_ID, AvisoTestData.ENTRADAS_ID)));
    }

    private void dadoUnAvisoQueEditaUnaFilaDeYerbaConValoresVacios() {
        laTablaDelInventarioTieneUnaFilaDeYerbaConPrecio();
        aviso = elNegocioGuarda(AvisosDePrueba.avisoPendienteConAcciones(AvisosDePrueba.editarPrecio(Map.of())));
    }

    private void dadoUnAvisoQueCambiaElCostoDeYerbaPeroLaTablaNoTieneEseCampo() {
        laTablaDelInventarioTieneUnaFilaDeYerbaConPrecio();
        aviso = elNegocioGuarda(AvisosDePrueba.avisoPendienteConAcciones(
                AvisosDePrueba.editarPrecio(Map.of("costo-que-no-existe", 1))));
    }

    // --- cuando ---

    private void cuandoLaPersonaListaTodosLosAvisos() {
        listado = listarAvisos.execute(new ListarAvisosRequest(USUARIO_ID, null));
    }

    private void cuandoLaPersonaListaLosAvisosPendientes() {
        listado = listarAvisos.execute(new ListarAvisosRequest(USUARIO_ID, EstadoAviso.PENDIENTE));
    }

    private void cuandoLaPersonaCuentaSusAvisos() {
        contador = contarAvisos.execute(USUARIO_ID);
    }

    private void cuandoLaPersonaAplicaElAviso() {
        resultado = aplicarAviso.execute(new ResolverAvisoRequest(USUARIO_ID, AVISO_ID));
    }

    private void cuandoLaPersonaIntentaAplicarElAviso() {
        error = catchThrowable(this::cuandoLaPersonaAplicaElAviso);
    }

    private void cuandoLaPersonaDescartaElAviso() {
        resultado = descartarAviso.execute(new ResolverAvisoRequest(USUARIO_ID, AVISO_ID));
    }

    private void cuandoLaPersonaIntentaDescartarElAviso() {
        error = catchThrowable(this::cuandoLaPersonaDescartaElAviso);
    }

    // --- entonces ---

    private void entoncesLosAvisosSeVenEnOrden(String... titulos) {
        assertThat(listado).extracting(AvisoItem::titulo).containsExactly(titulos);
    }

    private void entoncesSoloVeElAvisoPendienteSinConsultarLosDemas() {
        assertThat(listado).extracting(AvisoItem::id).containsExactly(aviso.getId());
        verify(avisoRepository, never()).findByNegocioId(any());
    }

    private void entoncesHayTresPendientesYDosUrgentes() {
        assertThat(contador).isEqualTo(new ContadorAvisos(3, 2));
    }

    private void entoncesElAvisoQuedaAplicadoYGuardadoConLaHoraDelReloj() {
        entoncesElAvisoQuedaResueltoYGuardadoConLaHoraDelReloj(EstadoAviso.APLICADO);
    }

    private void entoncesElAvisoQuedaDescartadoYGuardadoConLaHoraDelReloj() {
        entoncesElAvisoQuedaResueltoYGuardadoConLaHoraDelReloj(EstadoAviso.DESCARTADO);
    }

    private void entoncesElAvisoQuedaResueltoYGuardadoConLaHoraDelReloj(EstadoAviso estado) {
        assertThat(resultado.id()).isEqualTo(AVISO_ID);
        assertThat(aviso.getEstado()).isEqualTo(estado);
        assertThat(aviso.getResueltoEn()).isEqualTo(AHORA);
        verify(avisoRepository).save(aviso);
    }

    private void entoncesSeAnotaLaDecision(TipoDecision decision) {
        verify(decisionesObjetivoService).registrarAviso(aviso, decision, AHORA);
    }

    private void entoncesSeRechazaPorque(Class<? extends Throwable> tipo, String mensaje) {
        assertThat(error).isInstanceOf(tipo).hasMessage(mensaje);
    }

    private void entoncesSeRechazaPorque(Class<? extends Throwable> tipo) {
        assertThat(error).isInstanceOf(tipo);
    }

    private void entoncesElAvisoNoExisteYNoSeGuardaNada() {
        entoncesSeRechazaPorque(ResourceNotFoundException.class, "El aviso no existe");
        verify(avisoRepository, never()).save(any());
    }

    private void entoncesSeRechazaSinEjecutarLaAccionNiGuardarNada() {
        entoncesSeRechazaPorque(BusinessRuleException.class);
        verifyNoInteractions(registroDatosService, decisionesObjetivoService);
        verify(avisoRepository, never()).save(any());
    }

    private void entoncesSeRechazaSinAnotarNingunaDecision() {
        entoncesSeRechazaPorque(BusinessRuleException.class);
        verifyNoInteractions(decisionesObjetivoService);
    }

    private void entoncesElPrecioDeLaYerbaSeCambiaAntesDeGuardarElAviso(int precio) {
        InOrder orden = inOrder(registroDatosService, avisoRepository);
        orden.verify(registroDatosService).editar(registro, campos, Map.of(CAMPO_PRECIO_ID.toString(), precio), AHORA);
        orden.verify(avisoRepository).save(aviso);
    }

    private void entoncesSeCreaLaFilaDelComboConOrigenDeAvisoAntesDeGuardarElAviso() {
        ArgumentCaptor<Registro> captor = ArgumentCaptor.forClass(Registro.class);
        InOrder orden = inOrder(controlPlanService, registroRepository, avisoRepository);
        orden.verify(controlPlanService).validarAltaRegistros(NEGOCIO_ID, 1, AHORA);
        orden.verify(registroRepository).save(captor.capture());
        orden.verify(avisoRepository).save(aviso);
        laFilaCreadaEsElCombo(captor.getValue());
        verify(registroDatosService).validarConexiones(any(), any(), any());
    }

    private void laFilaCreadaEsElCombo(Registro creado) {
        assertThat(creado.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(creado.getBloqueId()).isEqualTo(AvisoTestData.INVENTARIO_ID);
        assertThat(creado.getOrigen()).isEqualTo(Registro.ORIGEN_AVISO);
        assertThat(creado.getValores())
                .containsEntry(AvisoTestData.CAMPO_NOMBRE_ID.toString(), "Combo A + B")
                .containsEntry(AvisoTestData.CAMPO_CANTIDAD_ID.toString(), 0)
                .containsEntry(CAMPO_PRECIO_ID.toString(), 1710);
    }

    private void entoncesSeRechazaPorqueLaTablaPerdioSusCamposYNoSeGuardaNingunRegistro() {
        entoncesSeRechazaPorque(BusinessRuleException.class, SIN_CAMPOS);
        verify(registroRepository, never()).save(any());
    }

    private void entoncesSeRechazaPorqueLaTablaPerdioSusCamposYNoSeEditaNingunRegistro() {
        entoncesSeRechazaPorque(BusinessRuleException.class, SIN_CAMPOS);
        verify(registroDatosService, never()).editar(any(), any(), any(), any());
    }

    private void entoncesNoSeCambiaNingunDato() {
        verifyNoInteractions(registroDatosService, campoRepository);
    }

    // --- apoyo del escenario ---

    private Aviso elNegocioGuarda(Aviso unAviso) {
        when(avisoRepository.findByIdAndNegocioId(AVISO_ID, NEGOCIO_ID)).thenReturn(Optional.of(unAviso));
        return unAviso;
    }

    private void avisosPendientesDelNegocio(Aviso... avisos) {
        when(avisoRepository.findByNegocioIdAndEstado(NEGOCIO_ID, EstadoAviso.PENDIENTE)).thenReturn(List.of(avisos));
    }

    private void laTablaDelInventarioTiene(Registro fila, List<Campo> camposDeLaTabla) {
        when(registroRepository.obtener(REGISTRO_ID, NEGOCIO_ID)).thenReturn(fila);
        when(campoRepository.findByBloqueIdAndNegocioId(AvisoTestData.INVENTARIO_ID, NEGOCIO_ID))
                .thenReturn(camposDeLaTabla);
    }

    private void laTablaDelInventarioTieneUnaFilaDeYerbaConPrecio() {
        laTablaDelInventarioTiene(AvisoTestData.registro(REGISTRO_ID.valor(), "Yerba", 2, 5),
                List.of(AvisoTestData.campo(CAMPO_PRECIO_ID.valor(), "price")));
    }

    private static AccionDeAviso cambiarPrecio(int precio) {
        return AvisosDePrueba.editarPrecio(Map.of("price", precio));
    }

    private static AccionDeAviso crearCombo(Map<String, Object> valores) {
        return AvisosDePrueba.crearEnElInventario(valores);
    }

    private static Aviso avisoDe(String titulo, int prioridad, EstadoAviso estado, int hora) {
        return AvisosDePrueba.avisoDelNegocio(titulo, prioridad, estado, hora);
    }
}
