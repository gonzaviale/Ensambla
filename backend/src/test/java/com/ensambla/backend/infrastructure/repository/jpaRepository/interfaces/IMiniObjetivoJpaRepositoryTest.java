package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.infrastructure.repository.entity.MiniObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.entity.ObjetivoEntity;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IMiniObjetivoJpaRepositoryTest extends PostgresDatosTestSupport {

    private static final BigDecimal META = new BigDecimal("400000.00");
    private static final LocalDate SEMANA = LocalDate.of(2026, 10, 5);

    @Autowired
    private IObjetivoJpaRepository objetivos;

    @Autowired
    private IMiniObjetivoJpaRepository minis;

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
    @DisplayName("Guarda y relee el mini-objetivo con todos sus datos")
    void saveShouldRoundTripEveryField() {
        UUID producto = UUID.randomUUID();
        UUID reemplazo = givenMini(objetivoA, negocioA, 2, EstadoMiniObjetivo.EN_CURSO, AHORA).getId();
        MiniObjetivoEntity entity = mini(objetivoA, negocioA, 1, EstadoMiniObjetivo.REEMPLAZADO, AHORA);
        entity.setRegistroId(producto);
        entity.setReferencia("Yerba 1 kg");
        entity.setTextoConIa(true);
        entity.setReemplazadoPor(reemplazo);
        UUID id = minis.saveAndFlush(entity).getId();

        MiniObjetivoEntity result = minis.findByIdAndObjetivoIdAndNegocioId(id, objetivoA, negocioA).orElseThrow();

        assertThat(result.getObjetivoId()).isEqualTo(objetivoA);
        assertThat(result.getNegocioId()).isEqualTo(negocioA);
        assertThat(result.getPosicion()).isEqualTo((short) 1);
        assertThat(result.getTipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(result.getRegistroId()).isEqualTo(producto);
        assertThat(result.getReferencia()).isEqualTo("Yerba 1 kg");
        assertThat(result.getMeta()).isEqualByComparingTo("14183.00");
        assertThat(result.getValorInicial()).isEqualByComparingTo("12600.00");
        assertThat(result.getValorActual()).isEqualByComparingTo("12600.00");
        assertThat(result.getAporteEstimado()).isEqualByComparingTo("6786.00");
        assertThat(result.getEstado()).isEqualTo(EstadoMiniObjetivo.REEMPLAZADO);
        assertThat(result.getSemanaInicio()).isEqualTo(SEMANA);
        assertThat(result.getTitulo()).isEqualTo("Subile $250");
        assertThat(result.getJustificacion()).isEqualTo("Porque sí");
        assertThat(result.isTextoConIa()).isTrue();
        assertThat(result.getReemplazadoPor()).isEqualTo(reemplazo);
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.getActualizadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Lista sólo los del objetivo del negocio, del más viejo al más nuevo y, a igualdad, por posición")
    void findAllShouldFilterAndOrder() {
        UUID nuevo = givenMini(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO, AHORA.plusDays(1)).getId();
        UUID segundo = givenMini(objetivoA, negocioA, 2, EstadoMiniObjetivo.EN_CURSO, AHORA).getId();
        UUID primero = givenMini(objetivoA, negocioA, 1, EstadoMiniObjetivo.LOGRADO, AHORA).getId();
        givenMini(objetivoB, negocioB, 1, EstadoMiniObjetivo.EN_CURSO, AHORA);

        List<MiniObjetivoEntity> result =
                minis.findAllByObjetivoIdAndNegocioIdOrderByCreadoEnAscPosicionAsc(objetivoA, negocioA);

        assertThat(result).extracting(MiniObjetivoEntity::getId).containsExactly(primero, segundo, nuevo);
    }

    @Test
    @DisplayName("No devuelve un mini-objetivo de otro negocio ni de otro objetivo")
    void findByIdShouldBeScopedToObjetivoAndNegocio() {
        UUID deA = givenMini(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO, AHORA).getId();

        Optional<MiniObjetivoEntity> otroNegocio = minis.findByIdAndObjetivoIdAndNegocioId(deA, objetivoA, negocioB);
        Optional<MiniObjetivoEntity> otroObjetivo = minis.findByIdAndObjetivoIdAndNegocioId(deA, objetivoB, negocioA);

        assertThat(otroNegocio).isEmpty();
        assertThat(otroObjetivo).isEmpty();
    }

    @Test
    @DisplayName("Un lugar lo ocupa uno solo en curso: el segundo en la misma posición se rechaza")
    void shouldRejectTwoInProgressInTheSamePlace() {
        givenMini(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO, AHORA);

        assertThatThrownBy(() -> givenMini(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO, AHORA))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Los que ya no están en curso no ocupan lugar: pueden compartir la posición con uno en curso")
    void shouldAllowHistoryInTheSamePlaceAsOneInProgress() {
        givenMini(objetivoA, negocioA, 1, EstadoMiniObjetivo.REEMPLAZADO, AHORA);
        givenMini(objetivoA, negocioA, 1, EstadoMiniObjetivo.LOGRADO, AHORA);
        givenMini(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO, AHORA);

        assertThat(minis.findAllByObjetivoIdAndNegocioIdOrderByCreadoEnAscPosicionAsc(objetivoA, negocioA))
                .hasSize(3);
    }

    @Test
    @DisplayName("Otro objetivo puede usar la misma posición")
    void shouldAllowTheSamePlaceInAnotherObjetivo() {
        givenMini(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO, AHORA);

        givenMini(objetivoB, negocioB, 1, EstadoMiniObjetivo.EN_CURSO, AHORA);

        assertThat(minis.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("La base rechaza una posición fuera de 1 a 3")
    void shouldRejectAPositionOutOfRange() {
        assertThatThrownBy(() -> givenMini(objetivoA, negocioA, 4, EstadoMiniObjetivo.EN_CURSO, AHORA))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("La base rechaza un texto en blanco")
    void shouldRejectABlankTitle() {
        MiniObjetivoEntity sinTitulo = mini(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO, AHORA);
        sinTitulo.setTitulo("  ");

        assertThatThrownBy(() -> minis.saveAndFlush(sinTitulo)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Al borrar el objetivo se borran sus mini-objetivos")
    void deletingObjetivoShouldDeleteItsMiniObjetivos() {
        givenMini(objetivoA, negocioA, 1, EstadoMiniObjetivo.EN_CURSO, AHORA);

        objetivos.deleteByIdAndNegocioId(objetivoA, negocioA);

        assertThat(minis.findAllByObjetivoIdAndNegocioIdOrderByCreadoEnAscPosicionAsc(objetivoA, negocioA)).isEmpty();
    }

    // --- helpers ---
    private UUID givenObjetivo(UUID negocioId) {
        return objetivos.saveAndFlush(ObjetivoEntity.builder()
                .negocioId(negocioId)
                .texto("Ganar más")
                .meta(META)
                .creadoEn(AHORA)
                .build()).getId();
    }

    private MiniObjetivoEntity givenMini(
            UUID objetivoId, UUID negocioId, int posicion, EstadoMiniObjetivo estado, OffsetDateTime creadoEn) {
        return minis.saveAndFlush(mini(objetivoId, negocioId, posicion, estado, creadoEn));
    }

    private static MiniObjetivoEntity mini(
            UUID objetivoId, UUID negocioId, int posicion, EstadoMiniObjetivo estado, OffsetDateTime creadoEn) {
        return MiniObjetivoEntity.builder()
                .id(UUID.randomUUID())
                .objetivoId(objetivoId)
                .negocioId(negocioId)
                .posicion((short) posicion)
                .tipo(TipoMiniObjetivo.SUBIR_PRECIO)
                .meta(new BigDecimal("14183.00"))
                .valorInicial(new BigDecimal("12600.00"))
                .valorActual(new BigDecimal("12600.00"))
                .aporteEstimado(new BigDecimal("6786.00"))
                .estado(estado)
                .semanaInicio(SEMANA)
                .titulo("Subile $250")
                .justificacion("Porque sí")
                .creadoEn(creadoEn)
                .actualizadoEn(creadoEn)
                .build();
    }
}
