package com.ensambla.backend.domain.useCase.objetivo.listarDecisiones;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivo;
import com.ensambla.backend.domain.model.objetivo.EstadoEfecto;
import com.ensambla.backend.domain.model.objetivo.TipoDecision;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.DecisionObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.DecisionTestData.AVISO_ID;
import static com.ensambla.backend.support.DecisionTestData.aviso;
import static com.ensambla.backend.support.DecisionTestData.decision;
import static com.ensambla.backend.support.DecisionTestData.impacto;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ListarDecisionesTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class);
    private final DecisionObjetivoRepository decisionObjetivoRepository = mock(DecisionObjetivoRepository.class);
    private final AvisoRepository avisoRepository = mock(AvisoRepository.class);
    private ListarDecisiones listarDecisiones;

    private ListarDecisionesResponse respuesta;
    private Throwable rechazo;

    @BeforeEach
    void setUp() {
        listarDecisiones = new ListarDecisiones(
                negocioRepository, objetivoRepository, decisionObjetivoRepository, avisoRepository);
        dadoUnObjetivoDelNegocio();
    }

    @Test
    @DisplayName("Trae las decisiones del objetivo con el efecto de los avisos, leyendo los avisos de una vez")
    void traeLasDecisionesDelObjetivoConElEfectoDeLosAvisosLeyendoLosAvisosDeUnaVez() {
        dadoUnAvisoAplicadoYMedidoConSuDecision();

        cuandoSeListan();

        entoncesLaDecisionTraeElEfectoMedidoDelAviso();
    }

    @Test
    @DisplayName("Sin decisiones de avisos no consulta los avisos")
    void sinDecisionesDeAvisosNoConsultaLosAvisos() {
        dadoUnaDecisionQueNoEsDeUnAviso();

        cuandoSeListan();

        entoncesHayUnaDecisionSinConsultarLosAvisos();
    }

    @Test
    @DisplayName("Sin decisiones devuelve una lista vacía")
    void sinDecisionesDevuelveUnaListaVacia() {
        dadoUnObjetivoSinDecisiones();

        cuandoSeListan();

        entoncesNoHaySemanas();
    }

    @Test
    @DisplayName("Un aviso borrado deja su decisión sin medición en lugar de fallar")
    void unAvisoBorradoDejaSuDecisionSinMedicionEnLugarDeFallar() {
        dadoUnaDecisionDeUnAvisoQueYaNoExiste();

        cuandoSeListan();

        entoncesLaDecisionQuedaSinMedicion();
    }

    @Test
    @DisplayName("Un objetivo de otro negocio responde como inexistente y no consulta sus decisiones")
    void unObjetivoDeOtroNegocioRespondeComoInexistenteYNoConsultaSusDecisiones() {
        dadoUnObjetivoDeOtroNegocio();

        cuandoSeIntentaListar();

        entoncesSeRechazaComoInexistenteSinConsultarDecisionesNiAvisos();
    }

    // --- dado ---

    private void dadoUnObjetivoDelNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID)).thenReturn(objetivo());
    }

    private void dadoUnAvisoAplicadoYMedidoConSuDecision() {
        Aviso aplicado = aviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO);
        aplicado.registrarImpacto(impacto(), AHORA);
        dadoLasDecisiones(decision(TipoDecision.AVISO_APLICADO, AVISO_ID));
        when(avisoRepository.findAllByIdsAndNegocioId(List.of(AVISO_ID), NEGOCIO_ID)).thenReturn(List.of(aplicado));
    }

    private void dadoUnaDecisionQueNoEsDeUnAviso() {
        dadoLasDecisiones(decision(TipoDecision.PASO_ACEPTADO, null));
    }

    private void dadoUnObjetivoSinDecisiones() {
        dadoLasDecisiones();
    }

    private void dadoUnaDecisionDeUnAvisoQueYaNoExiste() {
        dadoLasDecisiones(decision(TipoDecision.AVISO_APLICADO, AVISO_ID));
        when(avisoRepository.findAllByIdsAndNegocioId(List.of(AVISO_ID), NEGOCIO_ID)).thenReturn(List.of());
    }

    private void dadoUnObjetivoDeOtroNegocio() {
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID))
                .thenThrow(new ResourceNotFoundException("Objetivo no encontrado"));
    }

    private void dadoLasDecisiones(DecisionObjetivo... decisiones) {
        when(decisionObjetivoRepository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(List.of(decisiones));
    }

    // --- cuando ---

    private void cuandoSeListan() {
        respuesta = listarDecisiones.execute(new ListarDecisionesRequest(USUARIO_ID, OBJETIVO_ID));
    }

    private void cuandoSeIntentaListar() {
        rechazo = catchThrowable(this::cuandoSeListan);
    }

    // --- entonces ---

    private void entoncesLaDecisionTraeElEfectoMedidoDelAviso() {
        assertThat(respuesta.semanas()).singleElement().satisfies(semana -> assertThat(semana.decisiones())
                .singleElement().satisfies(decision -> {
                    assertThat(decision.efecto()).isEqualTo(EstadoEfecto.MEDIDO);
                    assertThat(decision.deltaIngresoSemanal()).isEqualByComparingTo("350.00");
                }));
    }

    private void entoncesHayUnaDecisionSinConsultarLosAvisos() {
        assertThat(respuesta.semanas()).singleElement()
                .satisfies(semana -> assertThat(semana.decisiones()).hasSize(1));
        verifyNoInteractions(avisoRepository);
    }

    private void entoncesNoHaySemanas() {
        assertThat(respuesta.semanas()).isEmpty();
    }

    private void entoncesLaDecisionQuedaSinMedicion() {
        assertThat(respuesta.semanas().getFirst().decisiones().getFirst().efecto())
                .isEqualTo(EstadoEfecto.SIN_MEDICION);
        verify(avisoRepository).findAllByIdsAndNegocioId(List.of(AVISO_ID), NEGOCIO_ID);
    }

    private void entoncesSeRechazaComoInexistenteSinConsultarDecisionesNiAvisos() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class).hasMessage("Objetivo no encontrado");
        verifyNoInteractions(decisionObjetivoRepository, avisoRepository);
    }
}
