package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.objetivo.ProgresoObjetivo;
import com.ensambla.backend.infrastructure.repository.entity.ProgresoObjetivoEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class ProgresoObjetivoRepositoryMapperTest {

    private static final UUID ID = UUID.fromString("00000000-0000-4000-8000-0000000000e1");
    private static final LocalDate DIA = LocalDate.of(2026, 11, 10);
    private static final BigDecimal VALOR = new BigDecimal("12600.00");

    private final ProgresoObjetivoRepositoryMapper mapper = new ProgresoObjetivoRepositoryMapper();

    @Test
    @DisplayName("Mapea la foto de un mini-objetivo con todos sus campos")
    void toDomainShouldMapEveryField() {
        ProgresoObjetivo result = mapper.toDomain(entity(MINI_ID.valor()));

        assertThat(result.id().valor()).isEqualTo(ID);
        assertThat(result.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(result.miniObjetivoId()).isEqualTo(MINI_ID);
        assertThat(result.dia()).isEqualTo(DIA);
        assertThat(result.valor()).isEqualTo(VALOR);
        assertThat(result.medidoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("La foto del objetivo mayor se mapea sin mini-objetivo")
    void toDomainShouldKeepTheMiniObjetivoEmptyForTheObjetivoPhoto() {
        ProgresoObjetivo result = mapper.toDomain(entity(null));

        assertThat(result.miniObjetivoId()).isNull();
        assertThat(result.esDelObjetivo()).isTrue();
    }

    // --- helpers ---
    private static ProgresoObjetivoEntity entity(UUID miniObjetivoId) {
        return ProgresoObjetivoEntity.builder()
                .id(ID)
                .objetivoId(OBJETIVO_ID.valor())
                .miniObjetivoId(miniObjetivoId)
                .dia(DIA)
                .valor(VALOR)
                .medidoEn(AHORA)
                .build();
    }
}
