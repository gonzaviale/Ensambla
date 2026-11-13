package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.ObjetivoEntity;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IObjetivoJpaRepositoryTest extends PostgresDatosTestSupport {

    private static final BigDecimal META = new BigDecimal("400000.00");

    @Autowired
    private IObjetivoJpaRepository objetivos;

    private UUID negocioA;
    private UUID negocioB;

    @BeforeEach
    void setUp() {
        negocioA = givenNegocio("a@example.com").getId();
        negocioB = givenNegocio("b@example.com").getId();
    }

    @Test
    @DisplayName("Guarda y relee el objetivo con todos sus datos")
    void saveShouldRoundTripEveryField() {
        UUID id = givenObjetivo(negocioA, "Ganar más", AHORA).getId();

        ObjetivoEntity result = objetivos.findByIdAndNegocioId(id, negocioA).orElseThrow();

        assertThat(result.getNegocioId()).isEqualTo(negocioA);
        assertThat(result.getTexto()).isEqualTo("Ganar más");
        assertThat(result.getMeta()).isEqualByComparingTo(META);
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Asigna un id cuando la entidad no trae uno")
    void saveShouldGenerateIdWhenMissing() {
        ObjetivoEntity result = givenObjetivo(negocioA, "Sin id", AHORA);

        assertThat(result.getId()).isNotNull();
    }

    @Test
    @DisplayName("Conserva el id cuando la entidad ya trae uno")
    void saveShouldKeepProvidedId() {
        UUID id = UUID.fromString("00000000-0000-4000-8000-0000000000a1");

        ObjetivoEntity result = objetivos.saveAndFlush(objetivo(id, negocioA, "Con id", AHORA));

        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("Lista solo los objetivos del negocio, del más viejo al más nuevo")
    void findAllShouldFilterByNegocioAndOrderByCreation() {
        UUID nuevo = givenObjetivo(negocioA, "Nuevo", AHORA.plusDays(1)).getId();
        UUID viejo = givenObjetivo(negocioA, "Viejo", AHORA).getId();
        givenObjetivo(negocioB, "Ajeno", AHORA);

        List<ObjetivoEntity> result = objetivos.findAllByNegocioIdOrderByCreadoEnAscIdAsc(negocioA);

        assertThat(result).extracting(ObjetivoEntity::getId).containsExactly(viejo, nuevo);
    }

    @Test
    @DisplayName("Un negocio sin objetivos recibe una lista vacía")
    void findAllShouldReturnEmptyForNegocioWithoutObjetivos() {
        givenObjetivo(negocioA, "Propio", AHORA);

        List<ObjetivoEntity> result = objetivos.findAllByNegocioIdOrderByCreadoEnAscIdAsc(negocioB);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("No devuelve un objetivo de otro negocio")
    void findByIdAndNegocioIdShouldReturnEmptyWhenObjetivoBelongsToOtherNegocio() {
        UUID deA = givenObjetivo(negocioA, "De A", AHORA).getId();

        Optional<ObjetivoEntity> result = objetivos.findByIdAndNegocioId(deA, negocioB);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Borra el objetivo del negocio")
    void deleteShouldRemoveObjetivoOfNegocio() {
        UUID id = givenObjetivo(negocioA, "Para borrar", AHORA).getId();

        objetivos.deleteByIdAndNegocioId(id, negocioA);

        assertThat(objetivos.findByIdAndNegocioId(id, negocioA)).isEmpty();
    }

    @Test
    @DisplayName("No borra un objetivo de otro negocio")
    void deleteShouldNotRemoveObjetivoOfOtherNegocio() {
        UUID deA = givenObjetivo(negocioA, "De A", AHORA).getId();

        objetivos.deleteByIdAndNegocioId(deA, negocioB);

        assertThat(objetivos.findByIdAndNegocioId(deA, negocioA)).isPresent();
    }

    @Test
    @DisplayName("Al borrar el negocio se borran sus objetivos")
    void deletingNegocioShouldDeleteItsObjetivos() {
        givenObjetivo(negocioA, "De A", AHORA);

        negocios.deleteById(negocioA);
        negocios.flush();
        objetivos.flush();

        assertThat(objetivos.findAllByNegocioIdOrderByCreadoEnAscIdAsc(negocioA)).isEmpty();
    }

    @Test
    @DisplayName("La base rechaza una meta que no es positiva")
    void shouldRejectNonPositiveMeta() {
        ObjetivoEntity sinMeta = objetivo(null, negocioA, "Sin meta", AHORA);
        sinMeta.setMeta(BigDecimal.ZERO);

        assertThatThrownBy(() -> objetivos.saveAndFlush(sinMeta))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Lista cada negocio con objetivos una sola vez y no incluye a los que no tienen")
    void findNegocioIdsConObjetivosShouldListEachBusinessOnce() {
        UUID sinObjetivos = givenNegocio("c@example.com").getId();
        givenObjetivo(negocioA, "Uno", AHORA);
        givenObjetivo(negocioA, "Dos", AHORA.plusDays(1));
        givenObjetivo(negocioB, "Tres", AHORA);

        List<UUID> result = objetivos.findNegocioIdsConObjetivos(PageRequest.of(0, 1000));

        assertThat(result).doesNotHaveDuplicates().contains(negocioA, negocioB).doesNotContain(sinObjetivos);
    }

    @Test
    @DisplayName("Pagina los negocios con objetivos sin repetirlos entre páginas")
    void findNegocioIdsConObjetivosShouldPageWithoutRepeating() {
        givenObjetivo(negocioA, "Uno", AHORA);
        givenObjetivo(negocioB, "Dos", AHORA);

        List<UUID> primera = objetivos.findNegocioIdsConObjetivos(PageRequest.of(0, 1));
        List<UUID> segunda = objetivos.findNegocioIdsConObjetivos(PageRequest.of(1, 1));

        assertThat(primera).hasSize(1);
        assertThat(segunda).hasSize(1).doesNotContainAnyElementsOf(primera);
    }

    @Test
    @DisplayName("La base rechaza un texto en blanco")
    void shouldRejectBlankText() {
        assertThatThrownBy(() -> givenObjetivo(negocioA, "   ", AHORA))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // --- helpers ---
    private ObjetivoEntity givenObjetivo(UUID negocioId, String texto, OffsetDateTime creadoEn) {
        return objetivos.saveAndFlush(objetivo(null, negocioId, texto, creadoEn));
    }

    private ObjetivoEntity objetivo(UUID id, UUID negocioId, String texto, OffsetDateTime creadoEn) {
        return ObjetivoEntity.builder()
                .id(id)
                .negocioId(negocioId)
                .texto(texto)
                .meta(META)
                .creadoEn(creadoEn)
                .build();
    }
}
