package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.precio.CambioPrecio;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.infrastructure.repository.entity.HistorialPrecioEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class HistorialPrecioRepositoryMapperTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-19T12:00:00Z");

    private final HistorialPrecioRepositoryMapper mapper = new HistorialPrecioRepositoryMapper();

    @Test
    @DisplayName("Pasa el cambio de precio de la entidad al dominio sin perder datos")
    void toDomainShouldMapEveryField() {
        CambioPrecio result = mapper.toDomain(HistorialPrecioEntity.builder()
                .negocioId(NEGOCIO_ID.valor()).registroId(REGISTRO_ID.valor()).precioAnterior(new BigDecimal("1000"))
                .precioNuevo(new BigDecimal("1200")).cambiadoEn(AHORA).build());

        assertThat(result).isEqualTo(new CambioPrecio(NEGOCIO_ID, REGISTRO_ID, new BigDecimal("1000"),
                new BigDecimal("1200"), AHORA));
    }

    @Test
    @DisplayName("Pasa el cambio de precio del dominio a la entidad sin perder datos")
    void toEntityShouldMapEveryField() {
        HistorialPrecioEntity result = mapper.toEntity(new CambioPrecio(NEGOCIO_ID, REGISTRO_ID, null,
                new BigDecimal("1200"), AHORA));

        assertThat(result.getId()).isNull();
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID.valor());
        assertThat(result.getRegistroId()).isEqualTo(REGISTRO_ID.valor());
        assertThat(result.getPrecioAnterior()).isNull();
        assertThat(result.getPrecioNuevo()).isEqualByComparingTo("1200");
        assertThat(result.getCambiadoEn()).isEqualTo(AHORA);
    }
}
