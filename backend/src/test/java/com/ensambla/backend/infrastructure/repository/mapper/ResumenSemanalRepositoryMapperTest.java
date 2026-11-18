package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.objetivo.CierreMini;
import com.ensambla.backend.domain.model.objetivo.ConteoDecisiones;
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

    private static final ResumenSemanalId RESUMEN_ID = ResumenSemanalId.de("00000000-0000-4000-8000-0000000000f1");
    private static final ReajusteMiniObjetivoId REAJUSTE_ID =
            ReajusteMiniObjetivoId.de("00000000-0000-4000-8000-0000000000f2");
    private static final BigDecimal RESULTADO = new BigDecimal("120000.00");
    private static final BigDecimal AVANCE = new BigDecimal("0.3000");
    private static final BigDecimal VALOR = new BigDecimal("12600.50");

    private final ResumenSemanalRepositoryMapper mapper = new ResumenSemanalRepositoryMapper();

    @Test
    @DisplayName("Pasa el resumen a la entidad con los mini-objetivos como JSON y el valor como texto")
    void unResumenSeGuardaConLosMinisComoJsonYElValorComoTexto() {
        ResumenSemanal resumen = dadoUnResumenSemanal();

        ResumenSemanalEntity entidad = cuandoSeGuardaElResumen(resumen);

        entoncesLaEntidadTieneTodosLosDatosDelResumen(entidad);
        entoncesLosMinisQuedanComoJsonConElValorComoTexto(entidad);
    }

    @Test
    @DisplayName("Lee el resumen de la entidad con sus mini-objetivos y sus reajustes")
    void unResumenSeLeeConSusMinisYSusReajustes() {
        ResumenSemanalEntity entidad = dadoUnaEntidadDeResumen();

        ResumenSemanal resumen = cuandoSeLeeElResumenConUnReajuste(entidad);

        entoncesElResumenTieneTodosLosDatosDeLaEntidad(resumen);
        entoncesElResumenRecuerdaElValorDelMini(resumen);
        entoncesElResumenTieneElReajusteLeido(resumen);
    }

    @Test
    @DisplayName("Pasa el reajuste a la entidad colgado de su resumen")
    void unReajusteSeGuardaColgadoDeSuResumen() {
        ReajusteMiniObjetivo reajuste = dadoUnReajusteQueSubeLaMeta();

        ReajusteMiniObjetivoEntity entidad = cuandoSeGuardaElReajusteDelResumen(reajuste);

        entoncesLaEntidadDelReajusteTieneTodosLosDatos(entidad);
    }

    @Test
    @DisplayName("Un reajuste sin mini-objetivo nuevo se guarda y se lee con el nuevo vacío")
    void unReajusteSinMiniNuevoSeGuardaYSeLeeConElNuevoVacio() {
        ReajusteMiniObjetivo sinNuevo = dadoUnReajusteSinMiniNuevo();

        ReajusteMiniObjetivoEntity entidad = cuandoSeGuardaElReajusteDelResumen(sinNuevo);

        entoncesLaEntidadNoTieneMiniNuevo(entidad);
        entoncesAlLeerloSeRecuperaElMismoReajuste(entidad, sinNuevo);
    }

    @Test
    @DisplayName("Lee el reajuste de la entidad con todos sus campos")
    void unReajusteSeLeeConTodosSusDatos() {
        ReajusteMiniObjetivoEntity entidad = dadoUnaEntidadDeReajuste();

        ReajusteMiniObjetivo reajuste = cuandoSeLeeElReajuste(entidad);

        entoncesElReajusteEsElQueSubeLaMeta(reajuste);
    }

    // --- dado ---
    private ResumenSemanal dadoUnResumenSemanal() {
        return new ResumenSemanal(RESUMEN_ID, OBJETIVO_ID, SEMANA, RESULTADO, AVANCE,
                new ConteoDecisiones(1, 2, 3, 4), List.of(new CierreMini(MINI_ID, VALOR)), "Bien", false, AHORA,
                List.of(dadoUnReajusteQueSubeLaMeta()));
    }

    private ResumenSemanalEntity dadoUnaEntidadDeResumen() {
        return mapper.toEntity(dadoUnResumenSemanal());
    }

    private ReajusteMiniObjetivo dadoUnReajusteQueSubeLaMeta() {
        return new ReajusteMiniObjetivo(REAJUSTE_ID, MINI_ID, OTRO_MINI_ID, TipoReajuste.SUBIR_META, "Lo lograste",
                "Subile $250", new BigDecimal("14183.00"), "Subile $500", new BigDecimal("15000.00"), AHORA);
    }

    private ReajusteMiniObjetivo dadoUnReajusteSinMiniNuevo() {
        return new ReajusteMiniObjetivo(REAJUSTE_ID, MINI_ID, null, TipoReajuste.REEMPLAZAR, "Sin reemplazo",
                "Subile", BigDecimal.TEN, null, null, AHORA);
    }

    private ReajusteMiniObjetivoEntity dadoUnaEntidadDeReajuste() {
        return unaEntidadDeReajusteHacia(OTRO_MINI_ID.valor());
    }

    private ReajusteMiniObjetivoEntity unaEntidadDeReajusteHacia(UUID miniNuevoId) {
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

    // --- cuando ---
    private ResumenSemanalEntity cuandoSeGuardaElResumen(ResumenSemanal resumen) {
        return mapper.toEntity(resumen);
    }

    private ResumenSemanal cuandoSeLeeElResumenConUnReajuste(ResumenSemanalEntity entidad) {
        return mapper.toDomain(entidad, List.of(unaEntidadDeReajusteHacia(OTRO_MINI_ID.valor())));
    }

    private ReajusteMiniObjetivoEntity cuandoSeGuardaElReajusteDelResumen(ReajusteMiniObjetivo reajuste) {
        return mapper.toEntity(RESUMEN_ID, reajuste);
    }

    private ReajusteMiniObjetivo cuandoSeLeeElReajuste(ReajusteMiniObjetivoEntity entidad) {
        return mapper.toDomain(entidad);
    }

    // --- entonces ---
    private void entoncesLaEntidadTieneTodosLosDatosDelResumen(ResumenSemanalEntity entidad) {
        assertThat(entidad.getId()).isEqualTo(RESUMEN_ID.valor());
        assertThat(entidad.getObjetivoId()).isEqualTo(OBJETIVO_ID.valor());
        assertThat(entidad.getSemanaInicio()).isEqualTo(SEMANA);
        assertThat(entidad.getResultado()).isEqualTo(RESULTADO);
        assertThat(entidad.getAvance()).isEqualTo(AVANCE);
        assertThat(entidad.getDecisionesAplicadas()).isEqualTo(1);
        assertThat(entidad.getDecisionesDescartadas()).isEqualTo(2);
        assertThat(entidad.getDecisionesAceptadas()).isEqualTo(3);
        assertThat(entidad.getDecisionesIgnoradas()).isEqualTo(4);
        assertThat(entidad.getQueFunciono()).isEqualTo("Bien");
        assertThat(entidad.isTextoConIa()).isFalse();
        assertThat(entidad.getCerradoEn()).isEqualTo(AHORA);
    }

    private void entoncesLosMinisQuedanComoJsonConElValorComoTexto(ResumenSemanalEntity entidad) {
        assertThat(entidad.getMinis()).containsExactly(
                Map.of("miniObjetivoId", MINI_ID.toString(), "valor", "12600.50"));
    }

    private void entoncesElResumenTieneTodosLosDatosDeLaEntidad(ResumenSemanal resumen) {
        assertThat(resumen).usingRecursiveComparison().ignoringFields("reajustes").isEqualTo(dadoUnResumenSemanal());
    }

    private void entoncesElResumenRecuerdaElValorDelMini(ResumenSemanal resumen) {
        assertThat(resumen.valorDe(MINI_ID)).contains(VALOR);
    }

    private void entoncesElResumenTieneElReajusteLeido(ResumenSemanal resumen) {
        assertThat(resumen.reajustes()).singleElement()
                .satisfies(reajuste -> assertThat(reajuste.miniNuevoId()).isEqualTo(OTRO_MINI_ID));
    }

    private void entoncesLaEntidadDelReajusteTieneTodosLosDatos(ReajusteMiniObjetivoEntity entidad) {
        assertThat(entidad).usingRecursiveComparison().isEqualTo(unaEntidadDeReajusteHacia(OTRO_MINI_ID.valor()));
    }

    private void entoncesLaEntidadNoTieneMiniNuevo(ReajusteMiniObjetivoEntity entidad) {
        assertThat(entidad.getMiniNuevoId()).isNull();
    }

    private void entoncesAlLeerloSeRecuperaElMismoReajuste(ReajusteMiniObjetivoEntity entidad,
                                                          ReajusteMiniObjetivo original) {
        assertThat(mapper.toDomain(entidad)).isEqualTo(original);
    }

    private void entoncesElReajusteEsElQueSubeLaMeta(ReajusteMiniObjetivo reajuste) {
        assertThat(reajuste).isEqualTo(dadoUnReajusteQueSubeLaMeta());
    }
}
