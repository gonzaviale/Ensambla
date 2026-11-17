package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.objetivo.TipoObjetivo;
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
import static org.assertj.core.api.Assertions.catchThrowable;

class IObjetivoJpaRepositoryTest extends PostgresDatosTestSupport {

    private static final BigDecimal META = new BigDecimal("400000.00");
    private static final UUID ID_PROPIO = UUID.fromString("00000000-0000-4000-8000-0000000000a1");

    @Autowired
    private IObjetivoJpaRepository objetivos;

    private UUID negocioA;
    private UUID negocioB;
    private UUID negocioSinObjetivos;
    private UUID idGuardado;
    private UUID idViejo;
    private UUID idNuevo;
    private ObjetivoEntity guardado;
    private ObjetivoEntity leido;
    private Optional<ObjetivoEntity> buscado;
    private List<ObjetivoEntity> listados;
    private List<UUID> negociosConObjetivos;
    private List<UUID> primeraPagina;
    private List<UUID> segundaPagina;
    private Throwable error;

    @BeforeEach
    void setUp() {
        negocioA = givenNegocio("a@example.com").getId();
        negocioB = givenNegocio("b@example.com").getId();
    }

    @Test
    @DisplayName("Guarda y relee el objetivo con todos sus datos")
    void saveShouldRoundTripEveryField() {
        givenUnObjetivoGuardado(negocioA, "Ganar más", AHORA);

        whenSeLeeElObjetivoGuardado();

        thenSeRelenTodosLosDatos();
    }

    @Test
    @DisplayName("Asigna un id cuando la entidad no trae uno")
    void saveShouldGenerateIdWhenMissing() {
        givenUnObjetivoGuardado(negocioA, "Sin id", AHORA);

        thenElObjetivoTieneId();
    }

    @Test
    @DisplayName("Conserva el id cuando la entidad ya trae uno")
    void saveShouldKeepProvidedId() {
        whenSeGuardaConId(ID_PROPIO);

        thenElIdEs(ID_PROPIO);
    }

    @Test
    @DisplayName("Lista solo los objetivos del negocio, del más viejo al más nuevo")
    void findAllShouldFilterByNegocioAndOrderByCreation() {
        givenDosObjetivosDeAYUnoDeB();

        whenSeListanLosObjetivosDe(negocioA);

        thenSeListanDelMasViejoAlMasNuevo();
    }

    @Test
    @DisplayName("Un negocio sin objetivos recibe una lista vacía")
    void findAllShouldReturnEmptyForNegocioWithoutObjetivos() {
        givenUnObjetivoGuardado(negocioA, "Propio", AHORA);

        whenSeListanLosObjetivosDe(negocioB);

        thenNoSeListaNada();
    }

    @Test
    @DisplayName("No devuelve un objetivo de otro negocio")
    void findByIdAndNegocioIdShouldReturnEmptyWhenObjetivoBelongsToOtherNegocio() {
        givenUnObjetivoGuardado(negocioA, "De A", AHORA);

        whenSeBuscaElObjetivoGuardadoEn(negocioB);

        thenNoSeEncuentra();
    }

    @Test
    @DisplayName("Borra el objetivo del negocio")
    void deleteShouldRemoveObjetivoOfNegocio() {
        givenUnObjetivoGuardado(negocioA, "Para borrar", AHORA);

        whenSeBorraElObjetivoEn(negocioA);

        thenElObjetivoYaNoEstaEn(negocioA);
    }

    @Test
    @DisplayName("No borra un objetivo de otro negocio")
    void deleteShouldNotRemoveObjetivoOfOtherNegocio() {
        givenUnObjetivoGuardado(negocioA, "De A", AHORA);

        whenSeBorraElObjetivoEn(negocioB);

        thenElObjetivoSigueEn(negocioA);
    }

    @Test
    @DisplayName("Al borrar el negocio se borran sus objetivos")
    void deletingNegocioShouldDeleteItsObjetivos() {
        givenUnObjetivoGuardado(negocioA, "De A", AHORA);

        whenSeBorraElNegocio(negocioA);

        thenElNegocioNoTieneObjetivos(negocioA);
    }

    @Test
    @DisplayName("La base rechaza una meta que no es positiva")
    void shouldRejectNonPositiveMeta() {
        whenSeGuardaConMetaEnCero();

        thenLaBaseLoRechaza();
    }

    @Test
    @DisplayName("Lista cada negocio con objetivos una sola vez y no incluye a los que no tienen")
    void findNegocioIdsConObjetivosShouldListEachBusinessOnce() {
        givenTresObjetivosEnDosNegociosYOtroNegocioSinObjetivos();

        whenSeListanLosNegociosConObjetivos();

        thenCadaNegocioConObjetivosApareceUnaVez();
    }

    @Test
    @DisplayName("Pagina los negocios con objetivos sin repetirlos entre páginas")
    void findNegocioIdsConObjetivosShouldPageWithoutRepeating() {
        givenUnObjetivoGuardado(negocioA, "Uno", AHORA);
        givenUnObjetivoGuardado(negocioB, "Dos", AHORA);

        whenSePidenDosPaginasDeUnNegocio();

        thenLasPaginasNoSeRepiten();
    }

    @Test
    @DisplayName("La base rechaza un texto en blanco")
    void shouldRejectBlankText() {
        whenSeGuardaConError(negocioA, "   ");

        thenLaBaseLoRechaza();
    }

    @Test
    @DisplayName("Guarda y relee un objetivo armado con Sami, sin meta en pesos")
    void saveShouldRoundTripAnObjetivoWithoutGoal() {
        whenSeGuardaUnObjetivoSinMeta(TipoObjetivo.HIBRIDO);

        thenSeReleeSinMetaYArmadoConSami();
    }

    @Test
    @DisplayName("Un objetivo guardado sin tipo es monetario y no lo armó Sami")
    void saveShouldDefaultToMonetario() {
        givenUnObjetivoGuardado(negocioA, "Clásico", AHORA);

        whenSeLeeElObjetivoGuardado();

        thenEsMonetarioYNoLoArmoSami();
    }

    @Test
    @DisplayName("La base rechaza un objetivo monetario sin meta")
    void saveShouldRejectAMonetarioWithoutGoal() {
        whenSeGuardaConError(objetivoSinMeta(negocioA, TipoObjetivo.MONETARIO));

        thenLaBaseLoRechaza();
    }

    @Test
    @DisplayName("La base rechaza un objetivo no monetario con meta")
    void saveShouldRejectANonMonetarioWithGoal() {
        whenSeGuardaConError(objetivoConMetaYSinDatos());

        thenLaBaseLoRechaza();
    }

    // --- arrange ---
    private void givenUnObjetivoGuardado(UUID negocioId, String texto, OffsetDateTime creadoEn) {
        guardado = objetivos.saveAndFlush(objetivo(null, negocioId, texto, creadoEn));
        idGuardado = guardado.getId();
    }

    private void givenDosObjetivosDeAYUnoDeB() {
        idNuevo = objetivos.saveAndFlush(objetivo(null, negocioA, "Nuevo", AHORA.plusDays(1))).getId();
        idViejo = objetivos.saveAndFlush(objetivo(null, negocioA, "Viejo", AHORA)).getId();
        objetivos.saveAndFlush(objetivo(null, negocioB, "Ajeno", AHORA));
    }

    private void givenTresObjetivosEnDosNegociosYOtroNegocioSinObjetivos() {
        negocioSinObjetivos = givenNegocio("c@example.com").getId();
        givenUnObjetivoGuardado(negocioA, "Uno", AHORA);
        givenUnObjetivoGuardado(negocioA, "Dos", AHORA.plusDays(1));
        givenUnObjetivoGuardado(negocioB, "Tres", AHORA);
    }

    // --- act ---
    private void whenSeLeeElObjetivoGuardado() {
        leido = objetivos.findByIdAndNegocioId(idGuardado, negocioA).orElseThrow();
    }

    private void whenSeGuardaConId(UUID id) {
        guardado = objetivos.saveAndFlush(objetivo(id, negocioA, "Con id", AHORA));
    }

    private void whenSeListanLosObjetivosDe(UUID negocioId) {
        listados = objetivos.findAllByNegocioIdOrderByCreadoEnAscIdAsc(negocioId);
    }

    private void whenSeBuscaElObjetivoGuardadoEn(UUID negocioId) {
        buscado = objetivos.findByIdAndNegocioId(idGuardado, negocioId);
    }

    private void whenSeBorraElObjetivoEn(UUID negocioId) {
        objetivos.deleteByIdAndNegocioId(idGuardado, negocioId);
    }

    private void whenSeBorraElNegocio(UUID negocioId) {
        negocios.deleteById(negocioId);
        negocios.flush();
        objetivos.flush();
    }

    private void whenSeGuardaConMetaEnCero() {
        ObjetivoEntity sinMeta = objetivo(null, negocioA, "Sin meta", AHORA);
        sinMeta.setMeta(BigDecimal.ZERO);
        whenSeGuardaConError(sinMeta);
    }

    private void whenSeGuardaConError(UUID negocioId, String texto) {
        error = catchThrowable(() -> givenUnObjetivoGuardado(negocioId, texto, AHORA));
    }

    private void whenSeGuardaConError(ObjetivoEntity objetivo) {
        error = catchThrowable(() -> objetivos.saveAndFlush(objetivo));
    }

    private void whenSeListanLosNegociosConObjetivos() {
        negociosConObjetivos = objetivos.findNegocioIdsConObjetivos(PageRequest.of(0, 1000));
    }

    private void whenSePidenDosPaginasDeUnNegocio() {
        primeraPagina = objetivos.findNegocioIdsConObjetivos(PageRequest.of(0, 1));
        segundaPagina = objetivos.findNegocioIdsConObjetivos(PageRequest.of(1, 1));
    }

    private void whenSeGuardaUnObjetivoSinMeta(TipoObjetivo tipo) {
        idGuardado = objetivos.saveAndFlush(objetivoSinMeta(negocioA, tipo)).getId();
        whenSeLeeElObjetivoGuardado();
    }

    // --- assert ---
    private void thenSeRelenTodosLosDatos() {
        assertThat(leido.getNegocioId()).isEqualTo(negocioA);
        assertThat(leido.getTexto()).isEqualTo("Ganar más");
        assertThat(leido.getMeta()).isEqualByComparingTo(META);
        assertThat(leido.getCreadoEn()).isEqualTo(AHORA);
    }

    private void thenElObjetivoTieneId() {
        assertThat(guardado.getId()).isNotNull();
    }

    private void thenElIdEs(UUID esperado) {
        assertThat(guardado.getId()).isEqualTo(esperado);
    }

    private void thenSeListanDelMasViejoAlMasNuevo() {
        assertThat(listados).extracting(ObjetivoEntity::getId).containsExactly(idViejo, idNuevo);
    }

    private void thenNoSeListaNada() {
        assertThat(listados).isEmpty();
    }

    private void thenNoSeEncuentra() {
        assertThat(buscado).isEmpty();
    }

    private void thenElObjetivoYaNoEstaEn(UUID negocioId) {
        assertThat(objetivos.findByIdAndNegocioId(idGuardado, negocioId)).isEmpty();
    }

    private void thenElObjetivoSigueEn(UUID negocioId) {
        assertThat(objetivos.findByIdAndNegocioId(idGuardado, negocioId)).isPresent();
    }

    private void thenElNegocioNoTieneObjetivos(UUID negocioId) {
        assertThat(objetivos.findAllByNegocioIdOrderByCreadoEnAscIdAsc(negocioId)).isEmpty();
    }

    private void thenLaBaseLoRechaza() {
        assertThat(error).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void thenCadaNegocioConObjetivosApareceUnaVez() {
        assertThat(negociosConObjetivos).doesNotHaveDuplicates().contains(negocioA, negocioB)
                .doesNotContain(negocioSinObjetivos);
    }

    private void thenLasPaginasNoSeRepiten() {
        assertThat(primeraPagina).hasSize(1);
        assertThat(segundaPagina).hasSize(1).doesNotContainAnyElementsOf(primeraPagina);
    }

    private void thenSeReleeSinMetaYArmadoConSami() {
        assertThat(leido.getTipo()).isEqualTo(TipoObjetivo.HIBRIDO);
        assertThat(leido.getMeta()).isNull();
        assertThat(leido.isArmadoConSami()).isTrue();
    }

    private void thenEsMonetarioYNoLoArmoSami() {
        assertThat(leido.getTipo()).isEqualTo(TipoObjetivo.MONETARIO);
        assertThat(leido.isArmadoConSami()).isFalse();
    }

    // --- helpers ---
    private ObjetivoEntity objetivo(UUID id, UUID negocioId, String texto, OffsetDateTime creadoEn) {
        return ObjetivoEntity.builder()
                .id(id)
                .negocioId(negocioId)
                .texto(texto)
                .meta(META)
                .creadoEn(creadoEn)
                .build();
    }

    private ObjetivoEntity objetivoSinMeta(UUID negocioId, TipoObjetivo tipo) {
        return ObjetivoEntity.builder()
                .negocioId(negocioId)
                .texto("Que me conozcan")
                .tipo(tipo)
                .armadoConSami(true)
                .creadoEn(AHORA)
                .build();
    }

    private ObjetivoEntity objetivoConMetaYSinDatos() {
        ObjetivoEntity conMeta = objetivoSinMeta(negocioA, TipoObjetivo.SIN_DATOS);
        conMeta.setMeta(META);
        return conMeta;
    }
}
