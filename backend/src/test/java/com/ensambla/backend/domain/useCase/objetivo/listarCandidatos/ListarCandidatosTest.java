package com.ensambla.backend.domain.useCase.objetivo.listarCandidatos;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.objetivo.CandidatoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.CandidatosDelNegocio;
import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.service.objetivo.PropuestaMiniObjetivosService;
import com.ensambla.backend.domain.useCase.objetivo.listarCandidatos.ListarCandidatosResponse.CandidatoItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidato;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidatoDelNegocio;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ListarCandidatosTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 10);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class);
    private final MiniObjetivoRepository miniObjetivoRepository = mock(MiniObjetivoRepository.class);
    private final PropuestaMiniObjetivosService propuestaMiniObjetivosService =
            mock(PropuestaMiniObjetivosService.class);
    private ListarCandidatos listarCandidatos;

    @BeforeEach
    void setUp() {
        listarCandidatos = new ListarCandidatos(
                negocioRepository,
                objetivoRepository,
                miniObjetivoRepository,
                propuestaMiniObjetivosService,
                Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID)).thenReturn(objetivo());
        when(miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, MINI_ID)).thenReturn(miniObjetivo());
    }

    @Test
    @DisplayName("Ofrece los candidatos de hoy menos el mismo mini-objetivo")
    void executeShouldOfferTheCandidatesOfTodayExceptTheCurrentOne() {
        givenMiniObjetivosEnCurso(miniObjetivo());
        givenCandidatos(candidato(), candidatoDelNegocio(TipoMiniObjetivo.MEJORAR_MARGEN, "6180"));

        ListarCandidatosResponse result = whenListar();

        assertThat(result.content()).extracting(CandidatoItem::tipo).containsExactly(TipoMiniObjetivo.MEJORAR_MARGEN);
    }

    @Test
    @DisplayName("No ofrece el tipo de los otros mini-objetivos en curso")
    void executeShouldNotOfferTheTypeOfTheOtherMiniObjetivosInProgress() {
        MiniObjetivo otro = miniObjetivo();
        otro.setId(MiniObjetivoId.nuevo());
        otro.setTipo(TipoMiniObjetivo.MEJORAR_MARGEN);
        otro.setRegistroId(null);
        givenMiniObjetivosEnCurso(miniObjetivo(), otro);
        givenCandidatos(candidatoDelNegocio(TipoMiniObjetivo.MEJORAR_MARGEN, "6180"),
                candidatoDelNegocio(TipoMiniObjetivo.RECORTAR_GASTO, "4000"));

        ListarCandidatosResponse result = whenListar();

        assertThat(result.content()).extracting(CandidatoItem::tipo).containsExactly(TipoMiniObjetivo.RECORTAR_GASTO);
    }

    @Test
    @DisplayName("Un mini-objetivo logrado antes no ocupa lugar: sus tipos siguen disponibles")
    void executeShouldIgnoreMiniObjetivosThatAreNotInProgress() {
        MiniObjetivo logrado = miniObjetivo();
        logrado.setId(MiniObjetivoId.nuevo());
        logrado.setTipo(TipoMiniObjetivo.MEJORAR_MARGEN);
        logrado.setEstado(EstadoMiniObjetivo.LOGRADO);
        when(miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(List.of(miniObjetivo(), logrado));
        givenCandidatos(candidatoDelNegocio(TipoMiniObjetivo.MEJORAR_MARGEN, "6180"));

        ListarCandidatosResponse result = whenListar();

        assertThat(result.content()).extracting(CandidatoItem::tipo).containsExactly(TipoMiniObjetivo.MEJORAR_MARGEN);
    }

    @Test
    @DisplayName("Sin candidatos devuelve una lista vacía")
    void executeShouldReturnAnEmptyListWhenThereAreNoCandidates() {
        givenMiniObjetivosEnCurso(miniObjetivo());
        givenCandidatos();

        assertThat(whenListar().content()).isEmpty();
    }

    @Test
    @DisplayName("Un mini-objetivo que ya no está en curso no tiene alternativas y no se calcula nada")
    void executeShouldFailWhenTheMiniObjetivoIsNotInProgress() {
        MiniObjetivo logrado = miniObjetivo();
        logrado.setEstado(EstadoMiniObjetivo.LOGRADO);
        when(miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, MINI_ID)).thenReturn(logrado);

        assertThatThrownBy(this::whenListar).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(propuestaMiniObjetivosService);
    }

    @Test
    @DisplayName("Un objetivo de otro negocio responde como inexistente")
    void executeShouldFailWhenTheObjetivoBelongsToAnotherBusiness() {
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID))
                .thenThrow(new ResourceNotFoundException("Objetivo no encontrado"));

        assertThatThrownBy(this::whenListar).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(miniObjetivoRepository, propuestaMiniObjetivosService);
    }

    @Test
    @DisplayName("Un mini-objetivo de otro objetivo responde como inexistente")
    void executeShouldFailWhenTheMiniObjetivoBelongsToAnotherObjetivo() {
        when(miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, MINI_ID))
                .thenThrow(new ResourceNotFoundException("Mini-objetivo no encontrado"));

        assertThatThrownBy(this::whenListar).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(propuestaMiniObjetivosService);
    }

    // --- arrange ---
    private void givenMiniObjetivosEnCurso(MiniObjetivo... enCurso) {
        when(miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(List.of(enCurso));
    }

    private void givenCandidatos(CandidatoMiniObjetivo... candidatos) {
        when(propuestaMiniObjetivosService.candidatos(NEGOCIO_ID, HOY))
                .thenReturn(new CandidatosDelNegocio(BigDecimal.ZERO, List.of(candidatos)));
    }

    // --- act ---
    private ListarCandidatosResponse whenListar() {
        return listarCandidatos.execute(new ListarCandidatosRequest(USUARIO_ID, OBJETIVO_ID, MINI_ID));
    }
}
