package com.ensambla.backend.infrastructure.repository.mapper;

import com.ensambla.backend.domain.model.inventario.MovimientoStock;
import com.ensambla.backend.domain.model.inventario.MovimientoStockId;
import com.ensambla.backend.domain.model.inventario.TipoMovimiento;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.infrastructure.repository.entity.MovimientoStockEntity;
import com.ensambla.backend.support.IdsDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MovimientoStockRepositoryMapperTest {

    private static final MovimientoStockId ID = MovimientoStockId.de("e0000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final RegistroId PRODUCTO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-13T12:00:00Z");

    private final MovimientoStockRepositoryMapper mapper = new MovimientoStockRepositoryMapper();

    @Test
    @DisplayName("Pasa del modelo a la entidad y de vuelta sin perder ningún dato")
    void unMovimientoDeStockVuelveIgualDespuesDeIdaYVuelta() {
        MovimientoStock movimiento = dadoUnaVentaDeDosYMedioKilos();

        MovimientoStockEntity entidad = cuandoSePasaALaEntidad(movimiento);

        entoncesAlVolverAlDominioEsElMismoMovimiento(entidad, movimiento);
    }

    // --- dado ---
    private MovimientoStock dadoUnaVentaDeDosYMedioKilos() {
        return MovimientoStock.builder()
                .id(ID)
                .negocioId(NEGOCIO_ID)
                .registroId(PRODUCTO_ID)
                .tipo(TipoMovimiento.SALIDA)
                .cantidad(new BigDecimal("2.5"))
                .fecha(LocalDate.parse("2026-10-12"))
                .nota("Venta")
                .creadoEn(AHORA)
                .build();
    }

    // --- cuando ---
    private MovimientoStockEntity cuandoSePasaALaEntidad(MovimientoStock movimiento) {
        return mapper.toEntity(movimiento);
    }

    // --- entonces ---
    private void entoncesAlVolverAlDominioEsElMismoMovimiento(MovimientoStockEntity entidad,
                                                              MovimientoStock original) {
        assertThat(entidad.getRegistroId()).isEqualTo(PRODUCTO_ID.valor());
        assertThat(entidad.getTipo()).isEqualTo(TipoMovimiento.SALIDA);
        assertThat(mapper.toDomain(entidad)).usingRecursiveComparison()
                .withEqualsForFieldsMatchingRegexes(IdsDePrueba::mismoValor, IdsDePrueba.CAMPOS_DE_ID)
                .isEqualTo(original);
    }
}
