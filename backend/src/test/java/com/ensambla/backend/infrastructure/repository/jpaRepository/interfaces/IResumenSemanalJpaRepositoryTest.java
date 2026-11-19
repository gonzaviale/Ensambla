package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.ObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.entity.ResumenSemanalEntity;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IResumenSemanalJpaRepositoryTest extends PostgresDatosTestSupport {

    private static final LocalDate SEMANA = LocalDate.of(2026, 11, 2);

    @Autowired
    private IObjetivoJpaRepository objetivos;

    @Autowired
    private IResumenSemanalJpaRepository resumenes;

    private UUID objetivo;
    private UUID otroObjetivo;

    @BeforeEach
    void setUp() {
        objetivo = givenObjetivo(givenNegocio("a@example.com").getId());
        otroObjetivo = givenObjetivo(givenNegocio("b@example.com").getId());
    }

    @Test
    @DisplayName("Guarda el resumen y lo relee con los mini-objetivos en JSON")
    void saveShouldRoundTripTheResumenWithTheMinisAsJson() {
        UUID mini = UUID.randomUUID();
        resumenes.saveAndFlush(resumen(objetivo, SEMANA, List.of(Map.of("miniObjetivoId", mini.toString(),
                "valor", "12600.50"))));

        ResumenSemanalEntity result = resumenes.findAllByObjetivoIdOrderBySemanaInicioDesc(objetivo).getFirst();

        assertThat(result.getSemanaInicio()).isEqualTo(SEMANA);
        assertThat(result.getResultado()).isEqualByComparingTo("120000.00");
        assertThat(result.getAvance()).isEqualByComparingTo("0.3");
        assertThat(result.getDecisionesAplicadas()).isEqualTo(1);
        assertThat(result.getDecisionesIgnoradas()).isEqualTo(4);
        assertThat(result.getMinis()).containsExactly(Map.of("miniObjetivoId", mini.toString(), "valor", "12600.50"));
        assertThat(result.getQueFunciono()).isEqualTo("Bien");
        assertThat(result.getCerradoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Lista las semanas del objetivo de la más nueva a la más vieja y no las de otro objetivo")
    void findAllByObjetivoIdShouldOrderFromNewestAndExcludeOtherObjetivos() {
        resumenes.saveAndFlush(resumen(objetivo, SEMANA, List.of()));
        resumenes.saveAndFlush(resumen(objetivo, SEMANA.plusWeeks(1), List.of()));
        resumenes.saveAndFlush(resumen(otroObjetivo, SEMANA, List.of()));

        List<ResumenSemanalEntity> result = resumenes.findAllByObjetivoIdOrderBySemanaInicioDesc(objetivo);

        assertThat(result).extracting(ResumenSemanalEntity::getSemanaInicio)
                .containsExactly(SEMANA.plusWeeks(1), SEMANA);
    }

    @Test
    @DisplayName("No deja cerrar dos veces la misma semana del mismo objetivo")
    void saveShouldRejectASecondResumenForTheSameWeek() {
        resumenes.saveAndFlush(resumen(objetivo, SEMANA, List.of()));

        assertThatThrownBy(() -> resumenes.saveAndFlush(resumen(objetivo, SEMANA, List.of())))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ux_resumen_semana");
    }

    @Test
    @DisplayName("Al borrar el objetivo se borran sus resúmenes")
    void deleteObjetivoShouldCascadeToItsResumenes() {
        resumenes.saveAndFlush(resumen(objetivo, SEMANA, List.of()));

        objetivos.deleteById(objetivo);
        objetivos.flush();

        assertThat(resumenes.findAllByObjetivoIdOrderBySemanaInicioDesc(objetivo)).isEmpty();
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

    static ResumenSemanalEntity resumen(UUID objetivoId, LocalDate semana, List<Object> minis) {
        return ResumenSemanalEntity.builder()
                .id(UUID.randomUUID())
                .objetivoId(objetivoId)
                .semanaInicio(semana)
                .resultado(new BigDecimal("120000.00"))
                .avance(new BigDecimal("0.3000"))
                .decisionesAplicadas(1)
                .decisionesDescartadas(2)
                .decisionesAceptadas(3)
                .decisionesIgnoradas(4)
                .minis(minis)
                .queFunciono("Bien")
                .textoConIa(false)
                .cerradoEn(AHORA)
                .build();
    }
}
