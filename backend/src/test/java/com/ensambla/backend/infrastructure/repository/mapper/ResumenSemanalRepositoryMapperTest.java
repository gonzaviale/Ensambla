package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.objetivo.CierreMini;
import com.ensambla.backend.domain.model.objetivo.ConteoDecisiones;
import com.ensambla.backend.domain.model.objetivo.HizoManual;
import com.ensambla.backend.domain.model.objetivo.ManualCierre;
import com.ensambla.backend.domain.model.objetivo.QuePaso;
import com.ensambla.backend.domain.model.objetivo.ReajusteMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.ReajusteMiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ResumenSemanal;
import com.ensambla.backend.domain.model.objetivo.ResumenSemanalId;
import com.ensambla.backend.domain.model.objetivo.TipoReajuste;
import com.ensambla.backend.infrastructure.repository.entity.ReajusteMiniObjetivoEntity;
import com.ensambla.backend.infrastructure.repository.entity.ResumenSemanalEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.SEMANA;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class ResumenSemanalRepositoryMapperTest {

    private static final List<ManualCierre> MANUALES = List.of(
            new ManualCierre(MINI_ID, "Pedile a 3 clientes", HizoManual.EN_PARTE, QuePaso.NO_TUVE_TIEMPO),
            new ManualCierre(OTRO_MINI_ID, "Un cartel", null, null));

    private static final ResumenSemanalId RESUMEN_ID = ResumenSemanalId.de("00000000-0000-4000-8000-0000000000f1");
    private static final ReajusteMiniObjetivoId REAJUSTE_ID =
            ReajusteMiniObjetivoId.de("00000000-0000-4000-8000-0000000000f2");
    private static final BigDecimal RESULTADO = new BigDecimal("120000.00");
    private static final BigDecimal AVANCE = new BigDecimal("0.3000");
    private static final BigDecimal VALOR = new BigDecimal("12600.50");

    private final ResumenSemanalRepositoryMapper mapper = new ResumenSemanalRepositoryMapper();
    private ResumenSemanalEntity entidadDelResumen;
    private ResumenSemanal resumenLeido;
    private ReajusteMiniObjetivoEntity entidadDelReajuste;
    private ReajusteMiniObjetivo reajusteLeido;
    private ReajusteMiniObjetivo reajusteSinNuevo;

    @Test
    @DisplayName("Pasa el resumen a la entidad con los mini-objetivos como JSON y el valor como texto")
    void pasaElResumenALaEntidadConLosMiniObjetivosComoJsonYElValorComoTexto() {
        cuandoSePasaElResumenALaEntidad();

        entoncesLaEntidadTraeTodosLosCamposYLosMinisComoJson();
    }

    @Test
    @DisplayName("Lee el resumen de la entidad con sus mini-objetivos y sus reajustes")
    void leeElResumenDeLaEntidadConSusMiniObjetivosYSusReajustes() {
        dadoLaEntidadDelResumen();

        cuandoSeLeeElResumenConUnReajuste();

        entoncesSeLeeElResumenConSusMinisYReajustes();
    }

    @Test
    @DisplayName("Pasa el reajuste a la entidad colgado de su resumen")
    void pasaElReajusteALaEntidadColgadoDeSuResumen() {
        cuandoSePasaElReajusteALaEntidad(reajuste());

        entoncesLaEntidadDelReajusteCuelgaDeSuResumen();
    }

    @Test
    @DisplayName("Un reajuste sin mini-objetivo nuevo se guarda con el nuevo vacío")
    void unReajusteSinMiniObjetivoNuevoSeGuardaConElNuevoVacio() {
        dadoUnReajusteSinNuevo();

        cuandoSePasaElReajusteALaEntidad(reajusteSinNuevo);

        entoncesLaEntidadNoTieneMiniNuevo();
    }

    @Test
    @DisplayName("Un reajuste sin mini-objetivo nuevo se lee con el nuevo vacío")
    void unReajusteSinMiniObjetivoNuevoSeLeeConElNuevoVacio() {
        dadoUnReajusteSinNuevo();

        cuandoSeLeeElReajusteGuardado();

        entoncesSeLeeElMismoReajusteSinNuevo();
    }

    @Test
    @DisplayName("Lee el reajuste de la entidad con todos sus campos")
    void leeElReajusteDeLaEntidadConTodosSusCampos() {
        cuandoSeLeeElReajusteDeLaEntidad();

        entoncesSeLeeElReajusteCompleto();
    }

    @Test
    @DisplayName("Pasa lo respondido de los manuales a JSON, con vacío cuando no respondió")
    void pasaLoRespondidoDeLosManualesAJsonConVacioCuandoNoRespondio() {
        cuandoSePasaElResumenALaEntidad();

        entoncesLosManualesVanComoJsonConVacioSiNoRespondio();
    }

    // --- arrange ---
    private void dadoLaEntidadDelResumen() {
        entidadDelResumen = mapper.toEntity(resumen());
    }

    private void dadoUnReajusteSinNuevo() {
        reajusteSinNuevo = new ReajusteMiniObjetivo(REAJUSTE_ID, MINI_ID, null, TipoReajuste.REEMPLAZAR,
                "Sin reemplazo", "Subile", BigDecimal.TEN, null, null, AHORA);
    }

    // --- act ---
    private void cuandoSePasaElResumenALaEntidad() {
        entidadDelResumen = mapper.toEntity(resumen());
    }

    private void cuandoSeLeeElResumenConUnReajuste() {
        resumenLeido = mapper.toDomain(entidadDelResumen, List.of(reajusteEntity(OTRO_MINI_ID.valor())));
    }

    private void cuandoSePasaElReajusteALaEntidad(ReajusteMiniObjetivo reajuste) {
        entidadDelReajuste = mapper.toEntity(RESUMEN_ID, reajuste);
    }

    private void cuandoSeLeeElReajusteGuardado() {
        reajusteLeido = mapper.toDomain(mapper.toEntity(RESUMEN_ID, reajusteSinNuevo));
    }

    private void cuandoSeLeeElReajusteDeLaEntidad() {
        reajusteLeido = mapper.toDomain(reajusteEntity(OTRO_MINI_ID.valor()));
    }

    // --- assert ---
    private void entoncesLaEntidadTraeTodosLosCamposYLosMinisComoJson() {
        assertThat(entidadDelResumen.getId()).isEqualTo(RESUMEN_ID.valor());
        assertThat(entidadDelResumen.getObjetivoId()).isEqualTo(OBJETIVO_ID.valor());
        assertThat(entidadDelResumen.getSemanaInicio()).isEqualTo(SEMANA);
        assertThat(entidadDelResumen.getResultado()).isEqualTo(RESULTADO);
        assertThat(entidadDelResumen.getAvance()).isEqualTo(AVANCE);
        assertThat(entidadDelResumen.getDecisionesAplicadas()).isEqualTo(1);
        assertThat(entidadDelResumen.getDecisionesDescartadas()).isEqualTo(2);
        assertThat(entidadDelResumen.getDecisionesAceptadas()).isEqualTo(3);
        assertThat(entidadDelResumen.getDecisionesIgnoradas()).isEqualTo(4);
        assertThat(entidadDelResumen.getMinis()).containsExactly(
                Map.of("miniObjetivoId", MINI_ID.toString(), "valor", "12600.50"));
        assertThat(entidadDelResumen.getQueFunciono()).isEqualTo("Bien");
        assertThat(entidadDelResumen.isTextoConIa()).isFalse();
        assertThat(entidadDelResumen.getCerradoEn()).isEqualTo(AHORA);
    }

    private void entoncesSeLeeElResumenConSusMinisYReajustes() {
        assertThat(resumenLeido).usingRecursiveComparison().ignoringFields("reajustes").isEqualTo(resumen());
        assertThat(resumenLeido.valorDe(MINI_ID)).contains(VALOR);
        assertThat(resumenLeido.reajustes()).singleElement()
                .satisfies(reajuste -> assertThat(reajuste.miniNuevoId()).isEqualTo(OTRO_MINI_ID));
    }

    private void entoncesLaEntidadDelReajusteCuelgaDeSuResumen() {
        assertThat(entidadDelReajuste).usingRecursiveComparison().isEqualTo(reajusteEntity(OTRO_MINI_ID.valor()));
    }

    private void entoncesLaEntidadNoTieneMiniNuevo() {
        assertThat(entidadDelReajuste.getMiniNuevoId()).isNull();
    }

    private void entoncesSeLeeElMismoReajusteSinNuevo() {
        assertThat(reajusteLeido).isEqualTo(reajusteSinNuevo);
    }

    private void entoncesSeLeeElReajusteCompleto() {
        assertThat(reajusteLeido).isEqualTo(reajuste());
    }

    private void entoncesLosManualesVanComoJsonConVacioSiNoRespondio() {
        assertThat(entidadDelResumen.getManuales()).containsExactly(
                new LinkedHashMap<>(Map.of("miniObjetivoId", MINI_ID.toString(), "titulo", "Pedile a 3 clientes",
                        "hizo", "EN_PARTE", "quePaso", "NO_TUVE_TIEMPO")),
                jsonSinRespuesta());
    }

    // --- helpers ---
    private static ResumenSemanal resumen() {
        return new ResumenSemanal(RESUMEN_ID, OBJETIVO_ID, SEMANA, RESULTADO, AVANCE,
                new ConteoDecisiones(1, 2, 3, 4), List.of(new CierreMini(MINI_ID, VALOR)), "Bien", false, AHORA,
                List.of(reajuste()), MANUALES);
    }

    private static ReajusteMiniObjetivo reajuste() {
        return new ReajusteMiniObjetivo(REAJUSTE_ID, MINI_ID, OTRO_MINI_ID, TipoReajuste.SUBIR_META, "Lo lograste",
                "Subile $250", new BigDecimal("14183.00"), "Subile $500", new BigDecimal("15000.00"), AHORA);
    }

    private static ReajusteMiniObjetivoEntity reajusteEntity(UUID miniNuevoId) {
        return ReajusteMiniObjetivoEntity.builder()
                .id(REAJUSTE_ID.valor())
                .resumenId(RESUMEN_ID.valor())
                .miniAnteriorId(MINI_ID.valor())
                .miniNuevoId(miniNuevoId)
                .tipo(TipoReajuste.SUBIR_META)
                .motivo("Lo lograste")
                .tituloAnterior("Subile $250")
                .metaAnterior(new BigDecimal("14183.00"))
                .tituloNuevo("Subile $500")
                .metaNueva(new BigDecimal("15000.00"))
                .creadoEn(AHORA)
                .build();
    }

    private static Map<String, Object> jsonSinRespuesta() {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("miniObjetivoId", OTRO_MINI_ID.toString());
        json.put("titulo", "Un cartel");
        json.put("hizo", null);
        json.put("quePaso", null);
        return json;
    }
}
