package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.GrupoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoDecision;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.infrastructure.repository.entity.AvisoEntity;
import com.ensambla.backend.infrastructure.repository.entity.DecisionObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.entity.MiniObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.entity.ObjetivoEntity;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IDecisionObjetivoJpaRepositoryTest extends PostgresDatosTestSupport {

    private static final LocalDate SEMANA = LocalDate.of(2026, 10, 5);

    @Autowired
    private IObjetivoJpaRepository objetivos;

    @Autowired
    private IMiniObjetivoJpaRepository minis;

    @Autowired
    private IAvisoJpaRepository avisos;

    @Autowired
    private IDecisionObjetivoJpaRepository decisiones;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID negocioA;
    private UUID negocioB;
    private UUID objetivoA;
    private UUID objetivoB;

    @BeforeEach
    void setUp() {
        negocioA = givenNegocio("a@example.com").getId();
        negocioB = givenNegocio("b@example.com").getId();
        objetivoA = givenObjetivo(negocioA);
        objetivoB = givenObjetivo(negocioB);
    }

    @Test
    @DisplayName("Guarda y relee la decisión con todos sus datos")
    void saveShouldRoundTripEveryField() {
        UUID mini = givenMini(objetivoA, negocioA);
        UUID aviso = givenAviso(negocioA, "Yerba");
        UUID producto = UUID.randomUUID();
        DecisionObjetivoEntity entity = decision(objetivoA, negocioA, TipoDecision.AVISO_APLICADO, AHORA);
        entity.setMiniObjetivoId(mini);
        entity.setAvisoId(aviso);
        entity.setRegistroId(producto);
        UUID id = decisiones.saveAndFlush(entity).getId();

        DecisionObjetivoEntity result = decisiones.findById(id).orElseThrow();

        assertThat(result.getNegocioId()).isEqualTo(negocioA);
        assertThat(result.getObjetivoId()).isEqualTo(objetivoA);
        assertThat(result.getMiniObjetivoId()).isEqualTo(mini);
        assertThat(result.getTipo()).isEqualTo(TipoDecision.AVISO_APLICADO);
        assertThat(result.getAvisoId()).isEqualTo(aviso);
        assertThat(result.getRegistroId()).isEqualTo(producto);
        assertThat(result.getDescripcion()).isEqualTo("Bajá el precio");
        assertThat(result.getTomadaEn()).isEqualTo(AHORA);
        assertThat(result.getSemanaInicio()).isEqualTo(SEMANA);
    }

    @Test
    @DisplayName("Lista las decisiones del objetivo de la más nueva a la más vieja")
    void findAllShouldOrderFromNewestToOldest() {
        UUID vieja = givenDecision(objetivoA, negocioA, TipoDecision.PASO_ACEPTADO, AHORA).getId();
        UUID nueva = givenDecision(objetivoA, negocioA, TipoDecision.PASO_IGNORADO, AHORA.plusDays(1)).getId();

        List<DecisionObjetivoEntity> result =
                decisiones.findAllByObjetivoIdAndNegocioIdOrderByTomadaEnDescIdDesc(objetivoA, negocioA);

        assertThat(result).extracting(DecisionObjetivoEntity::getId).containsExactly(nueva, vieja);
    }

    @Test
    @DisplayName("No lista decisiones de otro objetivo ni de otro negocio")
    void findAllShouldNotListOtherObjetivosOrBusinesses() {
        UUID deA = givenDecision(objetivoA, negocioA, TipoDecision.PASO_ACEPTADO, AHORA).getId();
        givenDecision(objetivoB, negocioB, TipoDecision.PASO_ACEPTADO, AHORA);

        assertThat(decisiones.findAllByObjetivoIdAndNegocioIdOrderByTomadaEnDescIdDesc(objetivoA, negocioA))
                .extracting(DecisionObjetivoEntity::getId).containsExactly(deA);
        assertThat(decisiones.findAllByObjetivoIdAndNegocioIdOrderByTomadaEnDescIdDesc(objetivoA, negocioB))
                .isEmpty();
    }

    @Test
    @DisplayName("Un aviso se registra una sola vez por objetivo, pero puede estar en dos objetivos")
    void saveShouldRejectTheSameAvisoTwiceInTheSameObjetivo() {
        UUID aviso = givenAviso(negocioA, "Yerba");
        UUID otroObjetivo = givenObjetivo(negocioA);
        givenDecisionDeAviso(objetivoA, negocioA, aviso);
        givenDecisionDeAviso(otroObjetivo, negocioA, aviso);

        assertThatThrownBy(() -> givenDecisionDeAviso(objetivoA, negocioA, aviso))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Las decisiones de pasos, sin aviso, pueden repetirse")
    void saveShouldAllowRepeatedStepDecisions() {
        givenDecision(objetivoA, negocioA, TipoDecision.PASO_ACEPTADO, AHORA);
        givenDecision(objetivoA, negocioA, TipoDecision.PASO_ACEPTADO, AHORA.plusHours(1));

        assertThat(decisiones.findAllByObjetivoIdAndNegocioIdOrderByTomadaEnDescIdDesc(objetivoA, negocioA))
                .hasSize(2);
    }

    @Test
    @DisplayName("Al borrar el aviso la decisión queda sin aviso en lugar de borrarse")
    void deleteAvisoShouldKeepTheDecisionWithoutAviso() {
        UUID aviso = givenAviso(negocioA, "Yerba");
        UUID id = givenDecisionDeAviso(objetivoA, negocioA, aviso).getId();

        avisos.deleteById(aviso);
        avisos.flush();

        assertThat(columnaDe(id, "aviso_id")).isNull();
    }

    @Test
    @DisplayName("Al borrar el mini-objetivo la decisión queda sin mini-objetivo")
    void deleteMiniObjetivoShouldKeepTheDecisionWithoutMiniObjetivo() {
        UUID mini = givenMini(objetivoA, negocioA);
        DecisionObjetivoEntity entity = decision(objetivoA, negocioA, TipoDecision.PASO_ACEPTADO, AHORA);
        entity.setMiniObjetivoId(mini);
        UUID id = decisiones.saveAndFlush(entity).getId();

        minis.deleteById(mini);
        minis.flush();

        assertThat(columnaDe(id, "mini_objetivo_id")).isNull();
    }

    @Test
    @DisplayName("Al borrar el objetivo se borran sus decisiones")
    void deleteObjetivoShouldCascadeToItsDecisions() {
        givenDecision(objetivoA, negocioA, TipoDecision.PASO_ACEPTADO, AHORA);

        objetivos.deleteById(objetivoA);
        objetivos.flush();

        assertThat(decisiones.findAll()).noneMatch(decision -> objetivoA.equals(decision.getObjetivoId()));
    }

    @Test
    @DisplayName("La base rechaza una descripción en blanco")
    void shouldRejectBlankDescription() {
        DecisionObjetivoEntity entity = decision(objetivoA, negocioA, TipoDecision.PASO_ACEPTADO, AHORA);
        entity.setDescripcion("   ");

        assertThatThrownBy(() -> decisiones.saveAndFlush(entity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // --- helpers ---
    /** Lee la columna directo de la base: la entidad ya cargada no se entera de lo que hace ON DELETE SET NULL. */
    private UUID columnaDe(UUID decisionId, String columna) {
        return jdbcTemplate.queryForObject(
                "SELECT " + columna + " FROM decision_objetivo WHERE id = ?", UUID.class, decisionId);
    }

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
                .semanaInicio(SEMANA)
                .titulo("Subile $250")
                .justificacion("Porque sí")
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID givenAviso(UUID negocioId, String nombre) {
        return avisos.saveAndFlush(AvisoEntity.builder()
                .negocioId(negocioId).tipo(TipoAviso.ESTANCADO).grupo(GrupoAviso.VENTAS).prioridad((short) 2)
                .entidad("registro:" + nombre).huella("ESTANCADO:" + nombre + ":" + UUID.randomUUID())
                .titulo("Aviso " + nombre).detalle("detalle").acciones(List.of()).estado(EstadoAviso.APLICADO)
                .resueltoEn(AHORA).creadoEn(AHORA).actualizadoEn(AHORA)
                .build()).getId();
    }

    private DecisionObjetivoEntity givenDecision(
            UUID objetivoId, UUID negocioId, TipoDecision tipo, OffsetDateTime tomadaEn) {
        return decisiones.saveAndFlush(decision(objetivoId, negocioId, tipo, tomadaEn));
    }

    private DecisionObjetivoEntity givenDecisionDeAviso(UUID objetivoId, UUID negocioId, UUID avisoId) {
        DecisionObjetivoEntity entity = decision(objetivoId, negocioId, TipoDecision.AVISO_APLICADO, AHORA);
        entity.setAvisoId(avisoId);
        return decisiones.saveAndFlush(entity);
    }

    private static DecisionObjetivoEntity decision(
            UUID objetivoId, UUID negocioId, TipoDecision tipo, OffsetDateTime tomadaEn) {
        return DecisionObjetivoEntity.builder()
                .id(UUID.randomUUID())
                .negocioId(negocioId)
                .objetivoId(objetivoId)
                .tipo(tipo)
                .descripcion("Bajá el precio")
                .tomadaEn(tomadaEn)
                .semanaInicio(SEMANA)
                .build();
    }
}
