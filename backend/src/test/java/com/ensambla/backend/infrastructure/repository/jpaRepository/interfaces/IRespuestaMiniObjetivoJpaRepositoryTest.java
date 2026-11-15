package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.HizoManual;
import com.ensambla.backend.domain.model.objetivo.ModoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.QuePaso;
import com.ensambla.backend.domain.model.objetivo.TipoObjetivo;
import com.ensambla.backend.infrastructure.repository.entity.MiniObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.entity.ObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.entity.RespuestaMiniObjetivoEntity;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IRespuestaMiniObjetivoJpaRepositoryTest extends PostgresDatosTestSupport {

    private static final LocalDate SEMANA = LocalDate.of(2026, 11, 9);

    @Autowired
    private IRespuestaMiniObjetivoJpaRepository respuestas;

    @Autowired
    private IMiniObjetivoJpaRepository minis;

    @Autowired
    private IObjetivoJpaRepository objetivos;

    private UUID negocioA;
    private UUID objetivoA;
    private UUID manualA;

    @BeforeEach
    void setUp() {
        negocioA = givenNegocio("a@example.com").getId();
        objetivoA = objetivos.saveAndFlush(ObjetivoEntity.builder()
                .negocioId(negocioA)
                .texto("Que me conozcan")
                .tipo(TipoObjetivo.SIN_DATOS)
                .armadoConSami(true)
                .creadoEn(AHORA)
                .build()).getId();
        manualA = minis.saveAndFlush(manual(objetivoA, negocioA, 1)).getId();
    }

    @Test
    @DisplayName("Guarda y relee la respuesta con todos sus datos")
    void saveShouldRoundTripEveryField() {
        UUID id = respuestas.saveAndFlush(respuesta(UUID.randomUUID(), manualA, HizoManual.NO, QuePaso.OTRA,
                "Llovió")).getId();

        RespuestaMiniObjetivoEntity result = respuestas.findById(id).orElseThrow();

        assertThat(result.getNegocioId()).isEqualTo(negocioA);
        assertThat(result.getObjetivoId()).isEqualTo(objetivoA);
        assertThat(result.getMiniObjetivoId()).isEqualTo(manualA);
        assertThat(result.getHizo()).isEqualTo(HizoManual.NO);
        assertThat(result.getQuePaso()).isEqualTo(QuePaso.OTRA);
        assertThat(result.getComentario()).isEqualTo("Llovió");
        assertThat(result.getSemanaInicio()).isEqualTo(SEMANA);
        assertThat(result.getRespondidaEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Sabe si un mini-objetivo ya tiene respuesta")
    void existsByMiniObjetivoIdShouldTellIfItWasAnswered() {
        assertThat(respuestas.existsByMiniObjetivoId(manualA)).isFalse();

        respuestas.saveAndFlush(respuesta(UUID.randomUUID(), manualA, HizoManual.SI, QuePaso.FUNCIONO, null));

        assertThat(respuestas.existsByMiniObjetivoId(manualA)).isTrue();
    }

    @Test
    @DisplayName("La base no admite una segunda respuesta al mismo mini-objetivo")
    void saveShouldRejectASecondAnswerToTheSameMiniObjetivo() {
        respuestas.saveAndFlush(respuesta(UUID.randomUUID(), manualA, HizoManual.SI, QuePaso.FUNCIONO, null));
        RespuestaMiniObjetivoEntity segunda =
                respuesta(UUID.randomUUID(), manualA, HizoManual.NO, QuePaso.OTRA, null);

        assertThatThrownBy(() -> respuestas.saveAndFlush(segunda)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("La base rechaza «lo hice» con «no tuve tiempo»")
    void saveShouldRejectDoneWithoutTime() {
        RespuestaMiniObjetivoEntity contradictoria =
                respuesta(UUID.randomUUID(), manualA, HizoManual.SI, QuePaso.NO_TUVE_TIEMPO, null);

        assertThatThrownBy(() -> respuestas.saveAndFlush(contradictoria))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("La base rechaza un comentario de más de 300 caracteres")
    void saveShouldRejectATooLongComment() {
        RespuestaMiniObjetivoEntity larga =
                respuesta(UUID.randomUUID(), manualA, HizoManual.NO, QuePaso.OTRA, "a".repeat(301));

        assertThatThrownBy(() -> respuestas.saveAndFlush(larga)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Al borrar el mini-objetivo se borra su respuesta")
    void deletingTheMiniObjetivoShouldDeleteItsAnswer() {
        UUID id = respuestas.saveAndFlush(
                respuesta(UUID.randomUUID(), manualA, HizoManual.SI, QuePaso.FUNCIONO, null)).getId();

        minis.deleteById(manualA);
        minis.flush();
        respuestas.flush();

        assertThat(respuestas.existsById(id)).isFalse();
    }

    // --- helpers ---
    private RespuestaMiniObjetivoEntity respuesta(
            UUID id, UUID miniObjetivoId, HizoManual hizo, QuePaso quePaso, String comentario) {
        return RespuestaMiniObjetivoEntity.builder()
                .id(id)
                .negocioId(negocioA)
                .objetivoId(objetivoA)
                .miniObjetivoId(miniObjetivoId)
                .hizo(hizo)
                .quePaso(quePaso)
                .comentario(comentario)
                .semanaInicio(SEMANA)
                .respondidaEn(AHORA)
                .build();
    }

    private static MiniObjetivoEntity manual(UUID objetivoId, UUID negocioId, int posicion) {
        return MiniObjetivoEntity.builder()
                .id(UUID.randomUUID())
                .objetivoId(objetivoId)
                .negocioId(negocioId)
                .posicion((short) posicion)
                .modo(ModoMiniObjetivo.MANUAL)
                .estado(EstadoMiniObjetivo.EN_CURSO)
                .semanaInicio(SEMANA)
                .titulo("Pedile a 3 clientes")
                .justificacion("Porque sí")
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build();
    }

    @Test
    @DisplayName("Lista las respuestas del objetivo del negocio y nunca las de otro negocio")
    void findAllByObjetivoIdAndNegocioIdShouldNeverListOtherBusinessAnswers() {
        UUID id = respuestas.saveAndFlush(
                respuesta(UUID.randomUUID(), manualA, HizoManual.SI, QuePaso.FUNCIONO, null)).getId();
        UUID negocioB = givenNegocio("b@example.com").getId();

        assertThat(respuestas.findAllByObjetivoIdAndNegocioId(objetivoA, negocioA))
                .extracting(RespuestaMiniObjetivoEntity::getId).containsExactly(id);
        assertThat(respuestas.findAllByObjetivoIdAndNegocioId(objetivoA, negocioB)).isEmpty();
    }
}
