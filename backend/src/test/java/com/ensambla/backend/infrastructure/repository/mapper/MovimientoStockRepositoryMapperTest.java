package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.inventario.TipoMovimiento;
import com.ensambla.backend.domain.model.inventario.MovimientoStock;
import com.ensambla.backend.infrastructure.repository.entity.MovimientoStockEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MovimientoStockRepositoryMapperTest {

    private static final UUID ID = UUID.fromString("e0000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID PRODUCTO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-13T12:00:00Z");

    private final MovimientoStockRepositoryMapper mapper = new MovimientoStockRepositoryMapper();

    @Test
    @DisplayName("Pasa del modelo a la entidad y de vuelta sin perder ningún dato")
    void shouldRoundTripEveryField() {
        MovimientoStock movimiento = MovimientoStock.builder()
                .id(ID)
                .negocioId(NEGOCIO_ID)
                .registroId(PRODUCTO_ID)
                .tipo(TipoMovimiento.SALIDA)
                .cantidad(new BigDecimal("2.5"))
                .fecha(LocalDate.parse("2026-10-12"))
                .nota("Venta")
                .creadoEn(AHORA)
                .build();

        MovimientoStockEntity entity = mapper.toEntity(movimiento);
        MovimientoStock result = mapper.toDomain(entity);

        assertThat(entity.getRegistroId()).isEqualTo(PRODUCTO_ID);
        assertThat(entity.getTipo()).isEqualTo(TipoMovimiento.SALIDA);
        assertThat(result).usingRecursiveComparison().isEqualTo(movimiento);
    }
}
