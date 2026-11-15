package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.infrastructure.repository.entity.MiniObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.entity.ObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.entity.ProgresoObjetivoEntity;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IProgresoObjetivoJpaRepositoryTest extends PostgresDatosTestSupport {

    private static final LocalDate DIA = LocalDate.of(2026, 10, 12);

    @Autowired
    private IObjetivoJpaRepository objetivos;

    @Autowired
    private IMiniObjetivoJpaRepository minis;

    @Autowired
    private IProgresoObjetivoJpaRepository progresos;

    private UUID negocio;
    private UUID objetivo;
    private UUID otroObjetivo;
    private UUID mini;

    @BeforeEach
    void setUp() {
        negocio = givenNegocio("a@example.com").getId();
        objetivo = givenObjetivo(negocio);
        otroObjetivo = givenObjetivo(givenNegocio("b@example.com").getId());
        mini = givenMini(objetivo, negocio);
    }

    @Test
    @DisplayName("Registra la foto de un mini-objetivo y la relee con todos sus datos")
    void registrarShouldRoundTripTheMiniObjetivoPhoto() {
        UUID id = UUID.randomUUID();

        progresos.registrar(id, objetivo, mini, DIA, new BigDecimal("12600.00"), AHORA);

        ProgresoObjetivoEntity result = progresos.findAllByObjetivoIdOrderByDiaAscMedidoEnAsc(objetivo).getFirst();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getObjetivoId()).isEqualTo(objetivo);
        assertThat(result.getMiniObjetivoId()).isEqualTo(mini);
        assertThat(result.getDia()).isEqualTo(DIA);
        assertThat(result.getValor()).isEqualByComparingTo("12600.00");
        assertThat(result.getMedidoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Registra la foto del objetivo mayor sin mini-objetivo")
    void registrarShouldSaveTheObjetivoPhotoWithoutMiniObjetivo() {
        progresos.registrar(UUID.randomUUID(), objetivo, null, DIA, new BigDecimal("-27000.00"), AHORA);

        List<ProgresoObjetivoEntity> result = progresos.findAllByObjetivoIdOrderByDiaAscMedidoEnAsc(objetivo);

        assertThat(result).singleElement().satisfies(foto -> {
            assertThat(foto.getMiniObjetivoId()).isNull();
            assertThat(foto.getValor()).isEqualByComparingTo("-27000.00");
        });
    }

    @Test
    @DisplayName("Una segunda foto del mismo mini-objetivo y día pisa a la anterior")
    void registrarShouldOverwriteThePhotoOfTheSameMiniObjetivoAndDay() {
        progresos.registrar(UUID.randomUUID(), objetivo, mini, DIA, new BigDecimal("12600.00"), AHORA);

        progresos.registrar(UUID.randomUUID(), objetivo, mini, DIA, new BigDecimal("13000.00"), AHORA.plusHours(1));

        assertThat(progresos.findAllByObjetivoIdOrderByDiaAscMedidoEnAsc(objetivo)).singleElement()
                .satisfies(foto -> {
                    assertThat(foto.getValor()).isEqualByComparingTo("13000.00");
                    assertThat(foto.getMedidoEn()).isEqualTo(AHORA.plusHours(1));
                });
    }

    @Test
    @DisplayName("Una segunda foto del objetivo mayor en el mismo día también pisa a la anterior")
    void registrarShouldOverwriteThePhotoOfTheObjetivoOnTheSameDay() {
        progresos.registrar(UUID.randomUUID(), objetivo, null, DIA, new BigDecimal("-27000.00"), AHORA);

        progresos.registrar(UUID.randomUUID(), objetivo, null, DIA, new BigDecimal("-20000.00"), AHORA.plusHours(1));

        assertThat(progresos.findAllByObjetivoIdOrderByDiaAscMedidoEnAsc(objetivo)).singleElement()
                .satisfies(foto -> assertThat(foto.getValor()).isEqualByComparingTo("-20000.00"));
    }

    @Test
    @DisplayName("El objetivo y su mini-objetivo guardan una foto cada uno el mismo día")
    void registrarShouldKeepOnePhotoPerSubjectAndDay() {
        progresos.registrar(UUID.randomUUID(), objetivo, null, DIA, BigDecimal.ONE, AHORA);
        progresos.registrar(UUID.randomUUID(), objetivo, mini, DIA, BigDecimal.TEN, AHORA);

        assertThat(progresos.findAllByObjetivoIdOrderByDiaAscMedidoEnAsc(objetivo)).hasSize(2);
    }

    @Test
    @DisplayName("Lista las fotos del objetivo de la más vieja a la más nueva y no las de otro objetivo")
    void findAllByObjetivoIdShouldOrderByDayAndExcludeOtherObjetivos() {
        progresos.registrar(UUID.randomUUID(), objetivo, null, DIA.plusDays(1), BigDecimal.TEN, AHORA);
        progresos.registrar(UUID.randomUUID(), objetivo, null, DIA, BigDecimal.ONE, AHORA);
        progresos.registrar(UUID.randomUUID(), otroObjetivo, null, DIA, BigDecimal.ZERO, AHORA);

        List<ProgresoObjetivoEntity> result = progresos.findAllByObjetivoIdOrderByDiaAscMedidoEnAsc(objetivo);

        assertThat(result).extracting(ProgresoObjetivoEntity::getDia).containsExactly(DIA, DIA.plusDays(1));
    }

    @Test
    @DisplayName("Al borrar el mini-objetivo se borran sus fotos y quedan las del objetivo")
    void deleteMiniObjetivoShouldCascadeToItsPhotos() {
        progresos.registrar(UUID.randomUUID(), objetivo, mini, DIA, BigDecimal.TEN, AHORA);
        progresos.registrar(UUID.randomUUID(), objetivo, null, DIA, BigDecimal.ONE, AHORA);

        minis.deleteById(mini);
        minis.flush();

        assertThat(progresos.findAllByObjetivoIdOrderByDiaAscMedidoEnAsc(objetivo))
                .singleElement()
                .satisfies(foto -> assertThat(foto.getMiniObjetivoId()).isNull());
    }

    @Test
    @DisplayName("No registra la foto de un objetivo que no existe")
    void registrarShouldRejectAnUnknownObjetivo() {
        assertThatThrownBy(() -> progresos.registrar(
                UUID.randomUUID(), UUID.randomUUID(), null, DIA, BigDecimal.ONE, AHORA))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // --- helpers ---
    private UUID givenObjetivo(UUID negocioId) {
        return objetivos.saveAndFlush(ObjetivoEntity.builder()
                .negocioId(negocioId)
                .texto("Ganar más")
                .meta(new BigDecimal("400000.00"))
                .creadoEn(AHORA)
                .build()).getId();
    }

    private UUID givenMini(UUID objetivoId, UUID negocioId) {
        return minis.saveAndFlush(MiniObjetivoEntity.builder()
                .id(UUID.randomUUID())
                .objetivoId(objetivoId)
                .negocioId(negocioId)
                .posicion((short) 1)
                .tipo(TipoMiniObjetivo.SUBIR_PRECIO)
                .meta(new BigDecimal("14183.00"))
                .valorInicial(new BigDecimal("12600.00"))
                .valorActual(new BigDecimal("12600.00"))
                .aporteEstimado(new BigDecimal("6786.00"))
                .estado(EstadoMiniObjetivo.EN_CURSO)
                .semanaInicio(LocalDate.of(2026, 10, 12))
                .titulo("Subile $250")
                .justificacion("Porque sí")
                .creadoEn(OffsetDateTime.parse("2026-10-10T12:00:00Z"))
                .actualizadoEn(OffsetDateTime.parse("2026-10-10T12:00:00Z"))
                .build()).getId();
    }
}
