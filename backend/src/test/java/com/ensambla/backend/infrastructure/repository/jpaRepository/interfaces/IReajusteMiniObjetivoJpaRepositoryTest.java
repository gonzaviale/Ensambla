package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoReajuste;
import com.ensambla.backend.infrastructure.repository.entity.MiniObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.entity.ObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.entity.ReajusteMiniObjetivoEntity;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IReajusteMiniObjetivoJpaRepositoryTest extends PostgresDatosTestSupport {

    private static final LocalDate SEMANA = LocalDate.of(2026, 11, 2);

    @Autowired
    private IObjetivoJpaRepository objetivos;

    @Autowired
    private IMiniObjetivoJpaRepository minis;

    @Autowired
    private IResumenSemanalJpaRepository resumenes;

    @Autowired
    private IReajusteMiniObjetivoJpaRepository reajustes;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID resumen;
    private UUID otroResumen;
    private UUID anterior;
    private UUID nuevo;

    @BeforeEach
    void dadoUnObjetivoConDosMiniObjetivosYDosResumenes() {
        UUID negocio = givenNegocio("a@example.com").getId();
        UUID objetivo = unObjetivo(negocio);
        anterior = unMini(objetivo, negocio, (short) 1);
        nuevo = unMini(objetivo, negocio, (short) 2);
        resumen = resumenes.saveAndFlush(IResumenSemanalJpaRepositoryTest.resumen(objetivo, SEMANA, List.of()))
                .getId();
        otroResumen = resumenes.saveAndFlush(
                IResumenSemanalJpaRepositoryTest.resumen(objetivo, SEMANA.plusWeeks(1), List.of())).getId();
    }

    private List<ReajusteMiniObjetivoEntity> reajustesDelResumen;
    private List<ReajusteMiniObjetivoEntity> reajustesDeVarios;

    @Test
    @DisplayName("Guarda el reajuste y lo relee con todos sus datos")
    void guardaElReajusteYLoReleeConTodosSusDatos() {
        cuandoSeGuardaUnReajusteConMiniNuevo();

        entoncesSeConservanTodosLosDatosDelReajuste();
    }

    @Test
    @DisplayName("Busca los reajustes de varios resúmenes juntos, del más viejo al más nuevo")
    void buscaLosReajustesDeVariosResumenesJuntosDelMasViejoAlMasNuevo() {
        dadoUnReajusteDeCadaResumenElDelOtroMasNuevo();

        cuandoSeBuscanLosReajustesDeLosDosResumenes();

        entoncesVienenOrdenadosPorCreacionYSePuedeAcotarAUnResumen();
    }

    @Test
    @DisplayName("Al borrar el mini-objetivo nuevo el reajuste queda sin él")
    void alBorrarElMiniObjetivoNuevoElReajusteQuedaSinEl() {
        dadoUnReajusteConMiniNuevo();

        cuandoSeBorraElMiniObjetivoNuevo();

        entoncesElReajusteQuedaSinMiniNuevo();
    }

    @Test
    @DisplayName("Al borrar el resumen se borran sus reajustes")
    void alBorrarElResumenSeBorranSusReajustes() {
        dadoUnReajusteSinMiniNuevo();

        cuandoSeBorraElResumen();

        entoncesNoQuedanReajustesDelResumen();
    }

    // --- dado ---
    private void dadoUnReajusteConMiniNuevo() {
        reajustes.saveAndFlush(unReajuste(resumen, nuevo, AHORA.toString()));
    }

    private void dadoUnReajusteSinMiniNuevo() {
        reajustes.saveAndFlush(unReajusteSinMiniNuevo(resumen, AHORA.toString()));
    }

    private void dadoUnReajusteDeCadaResumenElDelOtroMasNuevo() {
        reajustes.saveAndFlush(unReajuste(otroResumen, nuevo, AHORA.plusHours(1).toString()));
        reajustes.saveAndFlush(unReajuste(resumen, nuevo, AHORA.toString()));
    }

    private ReajusteMiniObjetivoEntity unReajuste(UUID resumenId, UUID miniNuevoId, String creadoEn) {
        return unReajusteBase(resumenId, miniNuevoId, creadoEn)
                .tituloNuevo("Después")
                .metaNueva(new BigDecimal("20"))
                .build();
    }

    private ReajusteMiniObjetivoEntity unReajusteSinMiniNuevo(UUID resumenId, String creadoEn) {
        return unReajusteBase(resumenId, null, creadoEn).build();
    }

    private ReajusteMiniObjetivoEntity.ReajusteMiniObjetivoEntityBuilder unReajusteBase(
            UUID resumenId, UUID miniNuevoId, String creadoEn) {
        return ReajusteMiniObjetivoEntity.builder()
                .id(UUID.randomUUID())
                .resumenId(resumenId)
                .miniAnteriorId(anterior)
                .miniNuevoId(miniNuevoId)
                .tipo(TipoReajuste.REEMPLAZAR)
                .motivo("Cambio")
                .tituloAnterior("Antes")
                .metaAnterior(BigDecimal.TEN)
                .creadoEn(OffsetDateTime.parse(creadoEn));
    }

    private UUID unObjetivo(UUID negocioId) {
        return objetivos.saveAndFlush(ObjetivoEntity.builder()
                .negocioId(negocioId)
                .texto("Ganar más")
                .meta(new BigDecimal("400000.00"))
                .creadoEn(AHORA)
                .build()).getId();
    }

    private UUID unMini(UUID objetivoId, UUID negocioId, short posicion) {
        return minis.saveAndFlush(MiniObjetivoEntity.builder()
                .id(UUID.randomUUID())
                .objetivoId(objetivoId)
                .negocioId(negocioId)
                .posicion(posicion)
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

    // --- cuando ---
    private void cuandoSeGuardaUnReajusteConMiniNuevo() {
        dadoUnReajusteConMiniNuevo();
    }

    private void cuandoSeBuscanLosReajustesDeLosDosResumenes() {
        reajustesDeVarios = reajustes.findAllByResumenIdInOrderByCreadoEnAscIdAsc(List.of(resumen, otroResumen));
    }

    private void cuandoSeBorraElMiniObjetivoNuevo() {
        minis.deleteById(nuevo);
        minis.flush();
    }

    private void cuandoSeBorraElResumen() {
        resumenes.deleteById(resumen);
        resumenes.flush();
    }

    // --- entonces ---
    private void entoncesSeConservanTodosLosDatosDelReajuste() {
        ReajusteMiniObjetivoEntity result =
                reajustes.findAllByResumenIdInOrderByCreadoEnAscIdAsc(List.of(resumen)).getFirst();
        assertThat(result.getMiniAnteriorId()).isEqualTo(anterior);
        assertThat(result.getMiniNuevoId()).isEqualTo(nuevo);
        assertThat(result.getTipo()).isEqualTo(TipoReajuste.REEMPLAZAR);
        assertThat(result.getMotivo()).isEqualTo("Cambio");
        assertThat(result.getMetaAnterior()).isEqualByComparingTo("10");
        assertThat(result.getMetaNueva()).isEqualByComparingTo("20");
    }

    private void entoncesVienenOrdenadosPorCreacionYSePuedeAcotarAUnResumen() {
        assertThat(reajustesDeVarios).extracting(ReajusteMiniObjetivoEntity::getResumenId)
                .containsExactly(resumen, otroResumen);
        assertThat(reajustes.findAllByResumenIdInOrderByCreadoEnAscIdAsc(List.of(resumen))).hasSize(1);
    }

    private void entoncesElReajusteQuedaSinMiniNuevo() {
        assertThat(jdbcTemplate.queryForList(
                "SELECT mini_nuevo_id FROM reajuste_mini_objetivo WHERE resumen_id = ?", UUID.class, resumen))
                .singleElement().isNull();
    }

    private void entoncesNoQuedanReajustesDelResumen() {
        assertThat(reajustes.findAllByResumenIdInOrderByCreadoEnAscIdAsc(List.of(resumen))).isEmpty();
    }
}
