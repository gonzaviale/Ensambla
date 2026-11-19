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
import static org.assertj.core.api.Assertions.catchThrowable;

class IObjetivoJpaRepositoryTest extends PostgresDatosTestSupport {

    private static final BigDecimal META = new BigDecimal("400000.00");

    @Autowired
    private IObjetivoJpaRepository objetivos;

    private UUID negocioA;
    private UUID negocioB;

    @BeforeEach
    void dadoDosNegocios() {
        negocioA = givenNegocio("a@example.com").getId();
        negocioB = givenNegocio("b@example.com").getId();
    }

    private UUID id;
    private UUID nuevo;
    private UUID viejo;
    private UUID deA;
    private UUID sinObjetivos;
    private ObjetivoEntity guardado;
    private ObjetivoEntity releido;
    private List<ObjetivoEntity> listados;
    private Optional<ObjetivoEntity> encontrado;
    private List<UUID> negocioIds;
    private List<UUID> primeraPagina;
    private List<UUID> segundaPagina;
    private Throwable rechazo;

    @Test
    @DisplayName("Guarda y relee el objetivo con todos sus datos")
    void guardaYReleeElObjetivoConTodosSusDatos() {
        dadoUnObjetivoGuardado("Ganar más");

        cuandoSeReleeElObjetivo();

        entoncesSeConservanTodosLosDatosDelObjetivo();
    }

    @Test
    @DisplayName("Asigna un id cuando la entidad no trae uno")
    void asignaUnIdCuandoLaEntidadNoTraeUno() {
        cuandoSeGuardaUnObjetivoSinId();

        entoncesElObjetivoGuardadoTieneId();
    }

    @Test
    @DisplayName("Conserva el id cuando la entidad ya trae uno")
    void conservaElIdCuandoLaEntidadYaTraeUno() {
        cuandoSeGuardaUnObjetivoConUnIdPropio();

        entoncesElObjetivoGuardadoConservaSuId();
    }

    @Test
    @DisplayName("Lista solo los objetivos del negocio, del más viejo al más nuevo")
    void listaSoloLosObjetivosDelNegocioDelMasViejoAlMasNuevo() {
        dadoUnObjetivoNuevoUnoViejoYUnoAjeno();

        cuandoSeListanLosObjetivosDelNegocioA();

        entoncesSeListanElViejoYElNuevoEnOrden();
    }

    @Test
    @DisplayName("Un negocio sin objetivos recibe una lista vacía")
    void unNegocioSinObjetivosRecibeUnaListaVacia() {
        dadoUnObjetivoPropio();

        cuandoSeListanLosObjetivosDelNegocioB();

        entoncesNoSeListaNingunObjetivo();
    }

    @Test
    @DisplayName("No devuelve un objetivo de otro negocio")
    void noDevuelveUnObjetivoDeOtroNegocio() {
        dadoUnObjetivoDeA();

        cuandoSeBuscaElObjetivoDeAConElNegocioB();

        entoncesNoSeEncuentraNingunObjetivo();
    }

    @Test
    @DisplayName("Borra el objetivo del negocio")
    void borraElObjetivoDelNegocio() {
        dadoUnObjetivoParaBorrar();

        cuandoSeBorraElObjetivoConSuNegocio();

        entoncesElObjetivoYaNoExiste();
    }

    @Test
    @DisplayName("No borra un objetivo de otro negocio")
    void noBorraUnObjetivoDeOtroNegocio() {
        dadoUnObjetivoDeA();

        cuandoSeBorraElObjetivoDeAConElNegocioB();

        entoncesElObjetivoDeASigueExistiendo();
    }

    @Test
    @DisplayName("Al borrar el negocio se borran sus objetivos")
    void alBorrarElNegocioSeBorranSusObjetivos() {
        dadoUnObjetivoDeA();

        cuandoSeBorraElNegocioA();

        entoncesNoQuedanObjetivosDelNegocioA();
    }

    @Test
    @DisplayName("La base rechaza una meta que no es positiva")
    void laBaseRechazaUnaMetaQueNoEsPositiva() {
        cuandoSeIntentaGuardarUnObjetivoConMetaCero();

        entoncesLaBaseRechazaPorIntegridad();
    }

    @Test
    @DisplayName("Lista cada negocio con objetivos una sola vez y no incluye a los que no tienen")
    void listaCadaNegocioConObjetivosUnaSolaVezYNoIncluyeALosQueNoTienen() {
        dadoDosObjetivosEnAUnoEnBYUnNegocioSinObjetivos();

        cuandoSeListanLosNegociosConObjetivos();

        entoncesCadaNegocioConObjetivosApareceUnaSolaVez();
    }

    @Test
    @DisplayName("Pagina los negocios con objetivos sin repetirlos entre páginas")
    void paginaLosNegociosConObjetivosSinRepetirlosEntrePaginas() {
        dadoUnObjetivoEnAYUnoEnB();

        cuandoSePidenDosPaginasDeUnNegocio();

        entoncesLasPaginasNoRepitenNegocios();
    }

    @Test
    @DisplayName("La base rechaza un texto en blanco")
    void laBaseRechazaUnTextoEnBlanco() {
        cuandoSeIntentaGuardarUnObjetivoConTextoEnBlanco();

        entoncesLaBaseRechazaPorIntegridad();
    }

    // --- dado ---
    private void dadoUnObjetivoGuardado(String texto) {
        id = unObjetivoGuardado(negocioA, texto, AHORA).getId();
    }

    private void dadoUnObjetivoNuevoUnoViejoYUnoAjeno() {
        nuevo = unObjetivoGuardado(negocioA, "Nuevo", AHORA.plusDays(1)).getId();
        viejo = unObjetivoGuardado(negocioA, "Viejo", AHORA).getId();
        unObjetivoGuardado(negocioB, "Ajeno", AHORA);
    }

    private void dadoUnObjetivoPropio() {
        unObjetivoGuardado(negocioA, "Propio", AHORA);
    }

    private void dadoUnObjetivoDeA() {
        deA = unObjetivoGuardado(negocioA, "De A", AHORA).getId();
    }

    private void dadoUnObjetivoParaBorrar() {
        id = unObjetivoGuardado(negocioA, "Para borrar", AHORA).getId();
    }

    private void dadoDosObjetivosEnAUnoEnBYUnNegocioSinObjetivos() {
        sinObjetivos = givenNegocio("c@example.com").getId();
        unObjetivoGuardado(negocioA, "Uno", AHORA);
        unObjetivoGuardado(negocioA, "Dos", AHORA.plusDays(1));
        unObjetivoGuardado(negocioB, "Tres", AHORA);
    }

    private void dadoUnObjetivoEnAYUnoEnB() {
        unObjetivoGuardado(negocioA, "Uno", AHORA);
        unObjetivoGuardado(negocioB, "Dos", AHORA);
    }

    private ObjetivoEntity unObjetivoGuardado(UUID negocioId, String texto, OffsetDateTime creadoEn) {
        return objetivos.saveAndFlush(unObjetivo(null, negocioId, texto, creadoEn));
    }

    private ObjetivoEntity unObjetivo(UUID idDado, UUID negocioId, String texto, OffsetDateTime creadoEn) {
        return ObjetivoEntity.builder()
                .id(idDado)
                .negocioId(negocioId)
                .texto(texto)
                .meta(META)
                .creadoEn(creadoEn)
                .build();
    }

    // --- cuando ---
    private void cuandoSeReleeElObjetivo() {
        releido = objetivos.findByIdAndNegocioId(id, negocioA).orElseThrow();
    }

    private void cuandoSeGuardaUnObjetivoSinId() {
        guardado = unObjetivoGuardado(negocioA, "Sin id", AHORA);
    }

    private void cuandoSeGuardaUnObjetivoConUnIdPropio() {
        id = UUID.fromString("00000000-0000-4000-8000-0000000000a1");
        guardado = objetivos.saveAndFlush(unObjetivo(id, negocioA, "Con id", AHORA));
    }

    private void cuandoSeListanLosObjetivosDelNegocioA() {
        listados = objetivos.findAllByNegocioIdOrderByCreadoEnAscIdAsc(negocioA);
    }

    private void cuandoSeListanLosObjetivosDelNegocioB() {
        listados = objetivos.findAllByNegocioIdOrderByCreadoEnAscIdAsc(negocioB);
    }

    private void cuandoSeBuscaElObjetivoDeAConElNegocioB() {
        encontrado = objetivos.findByIdAndNegocioId(deA, negocioB);
    }

    private void cuandoSeBorraElObjetivoConSuNegocio() {
        objetivos.deleteByIdAndNegocioId(id, negocioA);
    }

    private void cuandoSeBorraElObjetivoDeAConElNegocioB() {
        objetivos.deleteByIdAndNegocioId(deA, negocioB);
    }

    private void cuandoSeBorraElNegocioA() {
        negocios.deleteById(negocioA);
        negocios.flush();
        objetivos.flush();
    }

    private void cuandoSeIntentaGuardarUnObjetivoConMetaCero() {
        ObjetivoEntity sinMeta = unObjetivo(null, negocioA, "Sin meta", AHORA);
        sinMeta.setMeta(BigDecimal.ZERO);
        rechazo = catchThrowable(() -> objetivos.saveAndFlush(sinMeta));
    }

    private void cuandoSeIntentaGuardarUnObjetivoConTextoEnBlanco() {
        rechazo = catchThrowable(() -> unObjetivoGuardado(negocioA, "   ", AHORA));
    }

    private void cuandoSeListanLosNegociosConObjetivos() {
        negocioIds = objetivos.findNegocioIdsConObjetivos(PageRequest.of(0, 1000));
    }

    private void cuandoSePidenDosPaginasDeUnNegocio() {
        primeraPagina = objetivos.findNegocioIdsConObjetivos(PageRequest.of(0, 1));
        segundaPagina = objetivos.findNegocioIdsConObjetivos(PageRequest.of(1, 1));
    }

    // --- entonces ---
    private void entoncesSeConservanTodosLosDatosDelObjetivo() {
        assertThat(releido.getNegocioId()).isEqualTo(negocioA);
        assertThat(releido.getTexto()).isEqualTo("Ganar más");
        assertThat(releido.getMeta()).isEqualByComparingTo(META);
        assertThat(releido.getCreadoEn()).isEqualTo(AHORA);
    }

    private void entoncesElObjetivoGuardadoTieneId() {
        assertThat(guardado.getId()).isNotNull();
    }

    private void entoncesElObjetivoGuardadoConservaSuId() {
        assertThat(guardado.getId()).isEqualTo(id);
    }

    private void entoncesSeListanElViejoYElNuevoEnOrden() {
        assertThat(listados).extracting(ObjetivoEntity::getId).containsExactly(viejo, nuevo);
    }

    private void entoncesNoSeListaNingunObjetivo() {
        assertThat(listados).isEmpty();
    }

    private void entoncesNoSeEncuentraNingunObjetivo() {
        assertThat(encontrado).isEmpty();
    }

    private void entoncesElObjetivoYaNoExiste() {
        assertThat(objetivos.findByIdAndNegocioId(id, negocioA)).isEmpty();
    }

    private void entoncesElObjetivoDeASigueExistiendo() {
        assertThat(objetivos.findByIdAndNegocioId(deA, negocioA)).isPresent();
    }

    private void entoncesNoQuedanObjetivosDelNegocioA() {
        assertThat(objetivos.findAllByNegocioIdOrderByCreadoEnAscIdAsc(negocioA)).isEmpty();
    }

    private void entoncesLaBaseRechazaPorIntegridad() {
        assertThat(rechazo).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void entoncesCadaNegocioConObjetivosApareceUnaSolaVez() {
        assertThat(negocioIds).doesNotHaveDuplicates().contains(negocioA, negocioB).doesNotContain(sinObjetivos);
    }

    private void entoncesLasPaginasNoRepitenNegocios() {
        assertThat(primeraPagina).hasSize(1);
        assertThat(segundaPagina).hasSize(1).doesNotContainAnyElementsOf(primeraPagina);
    }
}
