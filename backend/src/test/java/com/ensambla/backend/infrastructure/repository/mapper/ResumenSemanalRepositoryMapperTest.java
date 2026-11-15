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
    void toEntityShouldMapEveryFieldAndTheMinisAsJson() {
        ResumenSemanalEntity result = mapper.toEntity(resumen());

        assertThat(result.getId()).isEqualTo(RESUMEN_ID.valor());
        assertThat(result.getObjetivoId()).isEqualTo(OBJETIVO_ID.valor());
        assertThat(result.getSemanaInicio()).isEqualTo(SEMANA);
        assertThat(result.getResultado()).isEqualTo(RESULTADO);
        assertThat(result.getAvance()).isEqualTo(AVANCE);
        assertThat(result.getDecisionesAplicadas()).isEqualTo(1);
        assertThat(result.getDecisionesDescartadas()).isEqualTo(2);
        assertThat(result.getDecisionesAceptadas()).isEqualTo(3);
        assertThat(result.getDecisionesIgnoradas()).isEqualTo(4);
        assertThat(result.getMinis()).containsExactly(
                Map.of("miniObjetivoId", MINI_ID.toString(), "valor", "12600.50"));
        assertThat(result.getQueFunciono()).isEqualTo("Bien");
        assertThat(result.isTextoConIa()).isFalse();
        assertThat(result.getCerradoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Lee el resumen de la entidad con sus mini-objetivos y sus reajustes")
    void toDomainShouldMapEveryFieldWithTheMinisAndReajustes() {
        ResumenSemanalEntity entity = mapper.toEntity(resumen());

        ResumenSemanal result = mapper.toDomain(entity, List.of(reajusteEntity(OTRO_MINI_ID.valor())));

        assertThat(result).usingRecursiveComparison().ignoringFields("reajustes").isEqualTo(resumen());
        assertThat(result.valorDe(MINI_ID)).contains(VALOR);
        assertThat(result.reajustes()).singleElement()
                .satisfies(reajuste -> assertThat(reajuste.miniNuevoId()).isEqualTo(OTRO_MINI_ID));
    }

    @Test
    @DisplayName("Pasa el reajuste a la entidad colgado de su resumen")
    void toEntityShouldMapTheReajusteWithItsResumen() {
        ReajusteMiniObjetivoEntity result = mapper.toEntity(RESUMEN_ID, reajuste());

        assertThat(result).usingRecursiveComparison().isEqualTo(reajusteEntity(OTRO_MINI_ID.valor()));
    }

    @Test
    @DisplayName("Un reajuste sin mini-objetivo nuevo se guarda y se lee con el nuevo vacío")
    void reajusteShouldKeepTheNewMiniEmptyWhenThereIsNone() {
        ReajusteMiniObjetivo sinNuevo = new ReajusteMiniObjetivo(REAJUSTE_ID, MINI_ID, null, TipoReajuste.REEMPLAZAR,
                "Sin reemplazo", "Subile", BigDecimal.TEN, null, null, AHORA);

        ReajusteMiniObjetivoEntity entity = mapper.toEntity(RESUMEN_ID, sinNuevo);

        assertThat(entity.getMiniNuevoId()).isNull();
        assertThat(mapper.toDomain(entity)).isEqualTo(sinNuevo);
    }

    @Test
    @DisplayName("Lee el reajuste de la entidad con todos sus campos")
    void toDomainShouldMapEveryFieldOfTheReajuste() {
        ReajusteMiniObjetivo result = mapper.toDomain(reajusteEntity(OTRO_MINI_ID.valor()));

        assertThat(result).isEqualTo(reajuste());
    }

    // --- helpers ---
    private static ResumenSemanal resumen() {
        return new ResumenSemanal(RESUMEN_ID, OBJETIVO_ID, SEMANA, RESULTADO, AVANCE,
                new ConteoDecisiones(1, 2, 3, 4), List.of(new CierreMini(MINI_ID, VALOR)), "Bien", false, AHORA,
                List.of(reajuste()));
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
}
