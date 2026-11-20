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
import static org.assertj.core.api.Assertions.catchThrowable;

class IResumenSemanalJpaRepositoryTest extends PostgresDatosTestSupport {

    private static final LocalDate SEMANA = LocalDate.of(2026, 11, 2);

    @Autowired
    private IObjetivoJpaRepository objetivos;

    @Autowired
    private IResumenSemanalJpaRepository resumenes;

    private UUID objetivo;
    private UUID otroObjetivo;

    @BeforeEach
    void dadoDosObjetivosDeDosNegocios() {
        objetivo = unObjetivo(givenNegocio("a@example.com").getId());
        otroObjetivo = unObjetivo(givenNegocio("b@example.com").getId());
    }

    private UUID mini;
    private ResumenSemanalEntity releido;
    private List<ResumenSemanalEntity> semanas;
    private Throwable rechazo;

    @Test
    @DisplayName("Guarda el resumen y lo relee con los mini-objetivos en JSON")
    void guardaElResumenYLoReleeConLosMiniObjetivosEnJson() {
        dadoUnResumenConUnMiniObjetivo();

        cuandoSeReleeElUltimoResumenDelObjetivo();

        entoncesSeConservanTodosLosDatosDelResumen();
    }

    @Test
    @DisplayName("Lista las semanas del objetivo de la más nueva a la más vieja y no las de otro objetivo")
    void listaLasSemanasDelObjetivoDeLaMasNuevaALaMasViejaYNoLasDeOtroObjetivo() {
        dadoDosSemanasDelObjetivoYUnaDeOtroObjetivo();

        cuandoSeListanLasSemanasDelObjetivo();

        entoncesVienenDeLaMasNuevaALaMasViejaSinLasDeOtroObjetivo();
    }

    @Test
    @DisplayName("No deja cerrar dos veces la misma semana del mismo objetivo")
    void noDejaCerrarDosVecesLaMismaSemanaDelMismoObjetivo() {
        dadoUnaSemanaCerrada();

        cuandoSeIntentaCerrarLaMismaSemanaOtraVez();

        entoncesLaBaseRechazaLaSemanaRepetida();
    }

    @Test
    @DisplayName("Al borrar el objetivo se borran sus resúmenes")
    void alBorrarElObjetivoSeBorranSusResumenes() {
        dadoUnaSemanaCerrada();

        cuandoSeBorraElObjetivo();

        entoncesNoQuedanResumenesDelObjetivo();
    }

    // --- dado ---
    private void dadoUnResumenConUnMiniObjetivo() {
        mini = UUID.randomUUID();
        resumenes.saveAndFlush(resumen(objetivo, SEMANA, List.of(Map.of("miniObjetivoId", mini.toString(),
                "valor", "12600.50"))));
    }

    private void dadoDosSemanasDelObjetivoYUnaDeOtroObjetivo() {
        resumenes.saveAndFlush(resumen(objetivo, SEMANA, List.of()));
        resumenes.saveAndFlush(resumen(objetivo, SEMANA.plusWeeks(1), List.of()));
        resumenes.saveAndFlush(resumen(otroObjetivo, SEMANA, List.of()));
    }

    private void dadoUnaSemanaCerrada() {
        resumenes.saveAndFlush(resumen(objetivo, SEMANA, List.of()));
    }

    private UUID unObjetivo(UUID negocioId) {
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

    // --- cuando ---
    private void cuandoSeReleeElUltimoResumenDelObjetivo() {
        releido = resumenes.findAllByObjetivoIdOrderBySemanaInicioDesc(objetivo).getFirst();
    }

    private void cuandoSeListanLasSemanasDelObjetivo() {
        semanas = resumenes.findAllByObjetivoIdOrderBySemanaInicioDesc(objetivo);
    }

    private void cuandoSeIntentaCerrarLaMismaSemanaOtraVez() {
        rechazo = catchThrowable(() -> resumenes.saveAndFlush(resumen(objetivo, SEMANA, List.of())));
    }

    private void cuandoSeBorraElObjetivo() {
        objetivos.deleteById(objetivo);
        objetivos.flush();
    }

    // --- entonces ---
    private void entoncesSeConservanTodosLosDatosDelResumen() {
        assertThat(releido.getSemanaInicio()).isEqualTo(SEMANA);
        assertThat(releido.getResultado()).isEqualByComparingTo("120000.00");
        assertThat(releido.getAvance()).isEqualByComparingTo("0.3");
        assertThat(releido.getDecisionesAplicadas()).isEqualTo(1);
        assertThat(releido.getDecisionesIgnoradas()).isEqualTo(4);
        assertThat(releido.getMinis()).containsExactly(Map.of("miniObjetivoId", mini.toString(), "valor", "12600.50"));
        assertThat(releido.getQueFunciono()).isEqualTo("Bien");
        assertThat(releido.getCerradoEn()).isEqualTo(AHORA);
    }

    private void entoncesVienenDeLaMasNuevaALaMasViejaSinLasDeOtroObjetivo() {
        assertThat(semanas).extracting(ResumenSemanalEntity::getSemanaInicio)
                .containsExactly(SEMANA.plusWeeks(1), SEMANA);
    }

    private void entoncesLaBaseRechazaLaSemanaRepetida() {
        assertThat(rechazo).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ux_resumen_semana");
    }

    private void entoncesNoQuedanResumenesDelObjetivo() {
        assertThat(resumenes.findAllByObjetivoIdOrderBySemanaInicioDesc(objetivo)).isEmpty();
    }
}
