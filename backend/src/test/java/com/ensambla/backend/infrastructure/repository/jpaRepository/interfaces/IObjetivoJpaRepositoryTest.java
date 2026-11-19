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
    void guardaYReleeElObjetivoConTodosSusDatos() {
        dadoUnObjetivoGuardado(negocioA, "Ganar más", AHORA);

        cuandoSeLeeElObjetivoGuardado();

        entoncesSeRelenTodosLosDatos();
    }

    @Test
    @DisplayName("Asigna un id cuando la entidad no trae uno")
    void asignaUnIdCuandoLaEntidadNoTraeUno() {
        dadoUnObjetivoGuardado(negocioA, "Sin id", AHORA);

        entoncesElObjetivoTieneId();
    }

    @Test
    @DisplayName("Conserva el id cuando la entidad ya trae uno")
    void conservaElIdCuandoLaEntidadYaTraeUno() {
        cuandoSeGuardaConId(ID_PROPIO);

        entoncesElIdEs(ID_PROPIO);
    }

    @Test
    @DisplayName("Lista solo los objetivos del negocio, del más viejo al más nuevo")
    void listaSoloLosObjetivosDelNegocioDelMasViejoAlMasNuevo() {
        dadoDosObjetivosDeAYUnoDeB();

        cuandoSeListanLosObjetivosDe(negocioA);

        entoncesSeListanDelMasViejoAlMasNuevo();
    }

    @Test
    @DisplayName("Un negocio sin objetivos recibe una lista vacía")
    void unNegocioSinObjetivosRecibeUnaListaVacia() {
        dadoUnObjetivoGuardado(negocioA, "Propio", AHORA);

        cuandoSeListanLosObjetivosDe(negocioB);

        entoncesNoSeListaNada();
    }

    @Test
    @DisplayName("No devuelve un objetivo de otro negocio")
    void noDevuelveUnObjetivoDeOtroNegocio() {
        dadoUnObjetivoGuardado(negocioA, "De A", AHORA);

        cuandoSeBuscaElObjetivoGuardadoEn(negocioB);

        entoncesNoSeEncuentra();
    }

    @Test
    @DisplayName("Borra el objetivo del negocio")
    void borraElObjetivoDelNegocio() {
        dadoUnObjetivoGuardado(negocioA, "Para borrar", AHORA);

        cuandoSeBorraElObjetivoEn(negocioA);

        entoncesElObjetivoYaNoEstaEn(negocioA);
    }

    @Test
    @DisplayName("No borra un objetivo de otro negocio")
    void noBorraUnObjetivoDeOtroNegocio() {
        dadoUnObjetivoGuardado(negocioA, "De A", AHORA);

        cuandoSeBorraElObjetivoEn(negocioB);

        entoncesElObjetivoSigueEn(negocioA);
    }

    @Test
    @DisplayName("Al borrar el negocio se borran sus objetivos")
    void alBorrarElNegocioSeBorranSusObjetivos() {
        dadoUnObjetivoGuardado(negocioA, "De A", AHORA);

        cuandoSeBorraElNegocio(negocioA);

        entoncesElNegocioNoTieneObjetivos(negocioA);
    }

    @Test
    @DisplayName("La base rechaza una meta que no es positiva")
    void shouldRejectNonPositiveMeta() {
        cuandoSeGuardaConMetaEnCero();

        entoncesLaBaseLoRechaza();
    }

    @Test
    @DisplayName("Lista cada negocio con objetivos una sola vez y no incluye a los que no tienen")
    void listaCadaNegocioConObjetivosUnaSolaVezYNoIncluyeALosQueNoTienen() {
        dadoTresObjetivosEnDosNegociosYOtroNegocioSinObjetivos();

        cuandoSeListanLosNegociosConObjetivos();

        entoncesCadaNegocioConObjetivosApareceUnaVez();
    }

    @Test
    @DisplayName("Pagina los negocios con objetivos sin repetirlos entre páginas")
    void paginaLosNegociosConObjetivosSinRepetirlosEntrePaginas() {
        dadoUnObjetivoGuardado(negocioA, "Uno", AHORA);
        dadoUnObjetivoGuardado(negocioB, "Dos", AHORA);

        cuandoSePidenDosPaginasDeUnNegocio();

        entoncesLasPaginasNoSeRepiten();
    }

    @Test
    @DisplayName("La base rechaza un texto en blanco")
    void shouldRejectBlankText() {
        cuandoSeGuardaConError(negocioA, "   ");

        entoncesLaBaseLoRechaza();
    }

    @Test
    @DisplayName("Guarda y relee un objetivo armado con Sami, sin meta en pesos")
    void guardaYReleeUnObjetivoArmadoConSamiSinMetaEnPesos() {
        cuandoSeGuardaUnObjetivoSinMeta(TipoObjetivo.HIBRIDO);

        entoncesSeReleeSinMetaYArmadoConSami();
    }

    @Test
    @DisplayName("Un objetivo guardado sin tipo es monetario y no lo armó Sami")
    void unObjetivoGuardadoSinTipoEsMonetarioYNoLoArmoSami() {
        dadoUnObjetivoGuardado(negocioA, "Clásico", AHORA);

        cuandoSeLeeElObjetivoGuardado();

        entoncesEsMonetarioYNoLoArmoSami();
    }

    @Test
    @DisplayName("La base rechaza un objetivo monetario sin meta")
    void laBaseRechazaUnObjetivoMonetarioSinMeta() {
        cuandoSeGuardaConError(objetivoSinMeta(negocioA, TipoObjetivo.MONETARIO));

        entoncesLaBaseLoRechaza();
    }

    @Test
    @DisplayName("La base rechaza un objetivo no monetario con meta")
    void laBaseRechazaUnObjetivoNoMonetarioConMeta() {
        cuandoSeGuardaConError(objetivoConMetaYSinDatos());

        entoncesLaBaseLoRechaza();
    }

    // --- arrange ---
    private void dadoUnObjetivoGuardado(UUID negocioId, String texto, OffsetDateTime creadoEn) {
        guardado = objetivos.saveAndFlush(objetivo(null, negocioId, texto, creadoEn));
        idGuardado = guardado.getId();
    }

    private void dadoDosObjetivosDeAYUnoDeB() {
        idNuevo = objetivos.saveAndFlush(objetivo(null, negocioA, "Nuevo", AHORA.plusDays(1))).getId();
        idViejo = objetivos.saveAndFlush(objetivo(null, negocioA, "Viejo", AHORA)).getId();
        objetivos.saveAndFlush(objetivo(null, negocioB, "Ajeno", AHORA));
    }

    private void dadoTresObjetivosEnDosNegociosYOtroNegocioSinObjetivos() {
        negocioSinObjetivos = givenNegocio("c@example.com").getId();
        dadoUnObjetivoGuardado(negocioA, "Uno", AHORA);
        dadoUnObjetivoGuardado(negocioA, "Dos", AHORA.plusDays(1));
        dadoUnObjetivoGuardado(negocioB, "Tres", AHORA);
    }

    // --- act ---
    private void cuandoSeLeeElObjetivoGuardado() {
        leido = objetivos.findByIdAndNegocioId(idGuardado, negocioA).orElseThrow();
    }

    private void cuandoSeGuardaConId(UUID id) {
        guardado = objetivos.saveAndFlush(objetivo(id, negocioA, "Con id", AHORA));
    }

    private void cuandoSeListanLosObjetivosDe(UUID negocioId) {
        listados = objetivos.findAllByNegocioIdOrderByCreadoEnAscIdAsc(negocioId);
    }

    private void cuandoSeBuscaElObjetivoGuardadoEn(UUID negocioId) {
        buscado = objetivos.findByIdAndNegocioId(idGuardado, negocioId);
    }

    private void cuandoSeBorraElObjetivoEn(UUID negocioId) {
        objetivos.deleteByIdAndNegocioId(idGuardado, negocioId);
    }

    private void cuandoSeBorraElNegocio(UUID negocioId) {
        negocios.deleteById(negocioId);
        negocios.flush();
        objetivos.flush();
    }

    private void cuandoSeGuardaConMetaEnCero() {
        ObjetivoEntity sinMeta = objetivo(null, negocioA, "Sin meta", AHORA);
        sinMeta.setMeta(BigDecimal.ZERO);
        cuandoSeGuardaConError(sinMeta);
    }

    private void cuandoSeGuardaConError(UUID negocioId, String texto) {
        error = catchThrowable(() -> dadoUnObjetivoGuardado(negocioId, texto, AHORA));
    }

    private void cuandoSeGuardaConError(ObjetivoEntity objetivo) {
        error = catchThrowable(() -> objetivos.saveAndFlush(objetivo));
    }

    private void cuandoSeListanLosNegociosConObjetivos() {
        negociosConObjetivos = objetivos.findNegocioIdsConObjetivos(PageRequest.of(0, 1000));
    }

    private void cuandoSePidenDosPaginasDeUnNegocio() {
        primeraPagina = objetivos.findNegocioIdsConObjetivos(PageRequest.of(0, 1));
        segundaPagina = objetivos.findNegocioIdsConObjetivos(PageRequest.of(1, 1));
    }

    private void cuandoSeGuardaUnObjetivoSinMeta(TipoObjetivo tipo) {
        idGuardado = objetivos.saveAndFlush(objetivoSinMeta(negocioA, tipo)).getId();
        cuandoSeLeeElObjetivoGuardado();
    }

    // --- assert ---
    private void entoncesSeRelenTodosLosDatos() {
        assertThat(leido.getNegocioId()).isEqualTo(negocioA);
        assertThat(leido.getTexto()).isEqualTo("Ganar más");
        assertThat(leido.getMeta()).isEqualByComparingTo(META);
        assertThat(leido.getCreadoEn()).isEqualTo(AHORA);
    }

    private void entoncesElObjetivoTieneId() {
        assertThat(guardado.getId()).isNotNull();
    }

    private void entoncesElIdEs(UUID esperado) {
        assertThat(guardado.getId()).isEqualTo(esperado);
    }

    private void entoncesSeListanDelMasViejoAlMasNuevo() {
        assertThat(listados).extracting(ObjetivoEntity::getId).containsExactly(idViejo, idNuevo);
    }

    private void entoncesNoSeListaNada() {
        assertThat(listados).isEmpty();
    }

    private void entoncesNoSeEncuentra() {
        assertThat(buscado).isEmpty();
    }

    private void entoncesElObjetivoYaNoEstaEn(UUID negocioId) {
        assertThat(objetivos.findByIdAndNegocioId(idGuardado, negocioId)).isEmpty();
    }

    private void entoncesElObjetivoSigueEn(UUID negocioId) {
        assertThat(objetivos.findByIdAndNegocioId(idGuardado, negocioId)).isPresent();
    }

    private void entoncesElNegocioNoTieneObjetivos(UUID negocioId) {
        assertThat(objetivos.findAllByNegocioIdOrderByCreadoEnAscIdAsc(negocioId)).isEmpty();
    }

    private void entoncesLaBaseLoRechaza() {
        assertThat(error).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void entoncesCadaNegocioConObjetivosApareceUnaVez() {
        assertThat(negociosConObjetivos).doesNotHaveDuplicates().contains(negocioA, negocioB)
                .doesNotContain(negocioSinObjetivos);
    }

    private void entoncesLasPaginasNoSeRepiten() {
        assertThat(primeraPagina).hasSize(1);
        assertThat(segundaPagina).hasSize(1).doesNotContainAnyElementsOf(primeraPagina);
    }

    private void entoncesSeReleeSinMetaYArmadoConSami() {
        assertThat(leido.getTipo()).isEqualTo(TipoObjetivo.HIBRIDO);
        assertThat(leido.getMeta()).isNull();
        assertThat(leido.isArmadoConSami()).isTrue();
    }

    private void entoncesEsMonetarioYNoLoArmoSami() {
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
