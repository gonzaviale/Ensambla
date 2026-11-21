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
import static org.assertj.core.api.Assertions.catchThrowable;

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
    void dadoDosNegociosConUnObjetivoCadaUno() {
        negocioA = givenNegocio("a@example.com").getId();
        negocioB = givenNegocio("b@example.com").getId();
        objetivoA = unObjetivo(negocioA);
        objetivoB = unObjetivo(negocioB);
    }

    private UUID mini;
    private UUID aviso;
    private UUID producto;
    private UUID decisionId;
    private UUID vieja;
    private UUID nueva;
    private UUID deA;
    private DecisionObjetivoEntity releida;
    private List<DecisionObjetivoEntity> listadas;
    private Throwable rechazo;

    @Test
    @DisplayName("Guarda y relee la decisión con todos sus datos")
    void guardaYReleeLaDecisionConTodosSusDatos() {
        dadoUnaDecisionDeAvisoConMiniObjetivoYProducto();

        cuandoSeReleeLaDecision();

        entoncesSeConservanTodosLosDatosDeLaDecision();
    }

    @Test
    @DisplayName("Lista las decisiones del objetivo de la más nueva a la más vieja")
    void listaLasDecisionesDelObjetivoDeLaMasNuevaALaMasVieja() {
        dadoUnaDecisionVieja();
        dadoUnaDecisionNueva();

        cuandoSeListanLasDecisionesDelObjetivoA();

        entoncesLaNuevaVieneAntesQueLaVieja();
    }

    @Test
    @DisplayName("No lista decisiones de otro objetivo ni de otro negocio")
    void noListaDecisionesDeOtroObjetivoNiDeOtroNegocio() {
        dadoUnaDecisionDelObjetivoAYOtraDelB();

        cuandoSeListanLasDecisionesDelObjetivoA();

        entoncesSoloSeListaLaDecisionDelObjetivoA();
    }

    @Test
    @DisplayName("Un aviso se registra una sola vez por objetivo, pero puede estar en dos objetivos")
    void unAvisoSeRegistraUnaSolaVezPorObjetivoPeroPuedeEstarEnDosObjetivos() {
        dadoUnAvisoRegistradoEnDosObjetivos();

        cuandoSeIntentaRegistrarElMismoAvisoOtraVezEnElObjetivoA();

        entoncesLaBaseRechazaPorIntegridad();
    }

    @Test
    @DisplayName("Las decisiones de pasos, sin aviso, pueden repetirse")
    void lasDecisionesDePasosSinAvisoPuedenRepetirse() {
        dadoDosDecisionesDePasoAceptado();

        cuandoSeListanLasDecisionesDelObjetivoA();

        entoncesSeListanDosDecisiones();
    }

    @Test
    @DisplayName("Al borrar el aviso la decisión queda sin aviso en lugar de borrarse")
    void alBorrarElAvisoLaDecisionQuedaSinAvisoEnLugarDeBorrarse() {
        dadoUnaDecisionDeAvisoRegistrada();

        cuandoSeBorraElAviso();

        entoncesLaDecisionQuedaSinAviso();
    }

    @Test
    @DisplayName("Al borrar el mini-objetivo la decisión queda sin mini-objetivo")
    void alBorrarElMiniObjetivoLaDecisionQuedaSinMiniObjetivo() {
        dadoUnaDecisionDeUnMiniObjetivo();

        cuandoSeBorraElMiniObjetivo();

        entoncesLaDecisionQuedaSinMiniObjetivo();
    }

    @Test
    @DisplayName("Al borrar el objetivo se borran sus decisiones")
    void alBorrarElObjetivoSeBorranSusDecisiones() {
        dadoUnaDecisionDePasoAceptado();

        cuandoSeBorraElObjetivo();

        entoncesNoQuedanDecisionesDelObjetivo();
    }

    @Test
    @DisplayName("La base rechaza una descripción en blanco")
    void laBaseRechazaUnaDescripcionEnBlanco() {
        cuandoSeIntentaGuardarUnaDecisionConDescripcionEnBlanco();

        entoncesLaBaseRechazaPorIntegridad();
    }

    // --- dado ---
    private void dadoUnaDecisionDeAvisoConMiniObjetivoYProducto() {
        mini = unMiniObjetivo(objetivoA, negocioA);
        aviso = unAviso(negocioA, "Yerba");
        producto = UUID.randomUUID();
        DecisionObjetivoEntity entity = unaDecision(objetivoA, negocioA, TipoDecision.AVISO_APLICADO, AHORA);
        entity.setMiniObjetivoId(mini);
        entity.setAvisoId(aviso);
        entity.setRegistroId(producto);
        decisionId = decisiones.saveAndFlush(entity).getId();
    }

    private void dadoUnaDecisionVieja() {
        vieja = unaDecisionGuardada(objetivoA, negocioA, TipoDecision.PASO_ACEPTADO, AHORA).getId();
    }

    private void dadoUnaDecisionNueva() {
        nueva = unaDecisionGuardada(objetivoA, negocioA, TipoDecision.PASO_IGNORADO, AHORA.plusDays(1)).getId();
    }

    private void dadoUnaDecisionDelObjetivoAYOtraDelB() {
        deA = unaDecisionGuardada(objetivoA, negocioA, TipoDecision.PASO_ACEPTADO, AHORA).getId();
        unaDecisionGuardada(objetivoB, negocioB, TipoDecision.PASO_ACEPTADO, AHORA);
    }

    private void dadoUnAvisoRegistradoEnDosObjetivos() {
        aviso = unAviso(negocioA, "Yerba");
        UUID otroObjetivo = unObjetivo(negocioA);
        unaDecisionDeAviso(objetivoA, negocioA, aviso);
        unaDecisionDeAviso(otroObjetivo, negocioA, aviso);
    }

    private void dadoDosDecisionesDePasoAceptado() {
        unaDecisionGuardada(objetivoA, negocioA, TipoDecision.PASO_ACEPTADO, AHORA);
        unaDecisionGuardada(objetivoA, negocioA, TipoDecision.PASO_ACEPTADO, AHORA.plusHours(1));
    }

    private void dadoUnaDecisionDeAvisoRegistrada() {
        aviso = unAviso(negocioA, "Yerba");
        decisionId = unaDecisionDeAviso(objetivoA, negocioA, aviso).getId();
    }

    private void dadoUnaDecisionDeUnMiniObjetivo() {
        mini = unMiniObjetivo(objetivoA, negocioA);
        DecisionObjetivoEntity entity = unaDecision(objetivoA, negocioA, TipoDecision.PASO_ACEPTADO, AHORA);
        entity.setMiniObjetivoId(mini);
        decisionId = decisiones.saveAndFlush(entity).getId();
    }

    private void dadoUnaDecisionDePasoAceptado() {
        unaDecisionGuardada(objetivoA, negocioA, TipoDecision.PASO_ACEPTADO, AHORA);
    }

    /** Lee la columna directo de la base: la entidad ya cargada no se entera de lo que hace ON DELETE SET NULL. */
    private UUID columnaDe(UUID decision, String columna) {
        return jdbcTemplate.queryForObject(
                "SELECT " + columna + " FROM decision_objetivo WHERE id = ?", UUID.class, decision);
    }

    private UUID unObjetivo(UUID negocioId) {
        return objetivos.saveAndFlush(ObjetivoEntity.builder()
                .negocioId(negocioId)
                .texto("Ganar más")
                .meta(new BigDecimal("400000.00"))
                .creadoEn(AHORA)
                .build()).getId();
    }

    private UUID unMiniObjetivo(UUID objetivoId, UUID negocioId) {
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

    private UUID unAviso(UUID negocioId, String nombre) {
        return avisos.saveAndFlush(AvisoEntity.builder()
                .negocioId(negocioId).tipo(TipoAviso.ESTANCADO).grupo(GrupoAviso.VENTAS).prioridad((short) 2)
                .entidad("registro:" + nombre).huella("ESTANCADO:" + nombre + ":" + UUID.randomUUID())
                .titulo("Aviso " + nombre).detalle("detalle").acciones(List.of()).estado(EstadoAviso.APLICADO)
                .resueltoEn(AHORA).creadoEn(AHORA).actualizadoEn(AHORA)
                .build()).getId();
    }

    private DecisionObjetivoEntity unaDecisionGuardada(
            UUID objetivoId, UUID negocioId, TipoDecision tipo, OffsetDateTime tomadaEn) {
        return decisiones.saveAndFlush(unaDecision(objetivoId, negocioId, tipo, tomadaEn));
    }

    private DecisionObjetivoEntity unaDecisionDeAviso(UUID objetivoId, UUID negocioId, UUID avisoId) {
        DecisionObjetivoEntity entity = unaDecision(objetivoId, negocioId, TipoDecision.AVISO_APLICADO, AHORA);
        entity.setAvisoId(avisoId);
        return decisiones.saveAndFlush(entity);
    }

    private static DecisionObjetivoEntity unaDecision(
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

    // --- cuando ---
    private void cuandoSeReleeLaDecision() {
        releida = decisiones.findById(decisionId).orElseThrow();
    }

    private void cuandoSeListanLasDecisionesDelObjetivoA() {
        listadas = decisiones.findAllByObjetivoIdAndNegocioIdOrderByTomadaEnDescIdDesc(objetivoA, negocioA);
    }

    private void cuandoSeIntentaRegistrarElMismoAvisoOtraVezEnElObjetivoA() {
        rechazo = catchThrowable(() -> unaDecisionDeAviso(objetivoA, negocioA, aviso));
    }

    private void cuandoSeBorraElAviso() {
        avisos.deleteById(aviso);
        avisos.flush();
    }

    private void cuandoSeBorraElMiniObjetivo() {
        minis.deleteById(mini);
        minis.flush();
    }

    private void cuandoSeBorraElObjetivo() {
        objetivos.deleteById(objetivoA);
        objetivos.flush();
    }

    private void cuandoSeIntentaGuardarUnaDecisionConDescripcionEnBlanco() {
        DecisionObjetivoEntity entity = unaDecision(objetivoA, negocioA, TipoDecision.PASO_ACEPTADO, AHORA);
        entity.setDescripcion("   ");
        rechazo = catchThrowable(() -> decisiones.saveAndFlush(entity));
    }

    // --- entonces ---
    private void entoncesSeConservanTodosLosDatosDeLaDecision() {
        assertThat(releida.getNegocioId()).isEqualTo(negocioA);
        assertThat(releida.getObjetivoId()).isEqualTo(objetivoA);
        assertThat(releida.getMiniObjetivoId()).isEqualTo(mini);
        assertThat(releida.getTipo()).isEqualTo(TipoDecision.AVISO_APLICADO);
        assertThat(releida.getAvisoId()).isEqualTo(aviso);
        assertThat(releida.getRegistroId()).isEqualTo(producto);
        assertThat(releida.getDescripcion()).isEqualTo("Bajá el precio");
        assertThat(releida.getTomadaEn()).isEqualTo(AHORA);
        assertThat(releida.getSemanaInicio()).isEqualTo(SEMANA);
    }

    private void entoncesLaNuevaVieneAntesQueLaVieja() {
        assertThat(listadas).extracting(DecisionObjetivoEntity::getId).containsExactly(nueva, vieja);
    }

    private void entoncesSoloSeListaLaDecisionDelObjetivoA() {
        assertThat(listadas).extracting(DecisionObjetivoEntity::getId).containsExactly(deA);
        assertThat(decisiones.findAllByObjetivoIdAndNegocioIdOrderByTomadaEnDescIdDesc(objetivoA, negocioB))
                .isEmpty();
    }

    private void entoncesLaBaseRechazaPorIntegridad() {
        assertThat(rechazo).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void entoncesSeListanDosDecisiones() {
        assertThat(listadas).hasSize(2);
    }

    private void entoncesLaDecisionQuedaSinAviso() {
        assertThat(columnaDe(decisionId, "aviso_id")).isNull();
    }

    private void entoncesLaDecisionQuedaSinMiniObjetivo() {
        assertThat(columnaDe(decisionId, "mini_objetivo_id")).isNull();
    }

    private void entoncesNoQuedanDecisionesDelObjetivo() {
        assertThat(decisiones.findAll()).noneMatch(decision -> objetivoA.equals(decision.getObjetivoId()));
    }
}
