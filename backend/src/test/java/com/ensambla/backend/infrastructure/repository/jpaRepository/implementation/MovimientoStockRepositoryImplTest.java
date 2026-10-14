package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.enums.TipoMovimiento;
import com.ensambla.backend.domain.model.inventario.MovimientoStock;
import com.ensambla.backend.infrastructure.repository.entity.MovimientoStockEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IMovimientoStockJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.MovimientoStockRepositoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovimientoStockRepositoryImplTest {

    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID PRODUCTO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID MOVIMIENTO_ID = UUID.fromString("e0000000-0000-4000-8000-000000000001");

    @Mock
    private IMovimientoStockJpaRepository jpa;

    private MovimientoStockRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new MovimientoStockRepositoryImpl(jpa, new MovimientoStockRepositoryMapper());
    }

    @Test
    @DisplayName("Guarda el movimiento y devuelve el modelo persistido")
    void saveShouldPersistAndReturnDomain() {
        when(jpa.save(any(MovimientoStockEntity.class))).thenReturn(entity());

        MovimientoStock result = repository.save(MovimientoStock.builder()
                .negocioId(NEGOCIO_ID)
                .registroId(PRODUCTO_ID)
                .tipo(TipoMovimiento.ENTRADA)
                .cantidad(BigDecimal.ONE)
                .build());

        assertThat(result.getId()).isEqualTo(MOVIMIENTO_ID);
        assertThat(result.getTipo()).isEqualTo(TipoMovimiento.ENTRADA);
        assertThat(result.getNota()).isEqualTo("Compra");
    }

    @Test
    @DisplayName("El stock sale del repositorio JPA indexado por producto")
    void stockDeShouldIndexTheSumsByProduct() {
        IMovimientoStockJpaRepository.StockPorRegistro fila = new IMovimientoStockJpaRepository.StockPorRegistro() {
            @Override
            public UUID getRegistro() {
                return PRODUCTO_ID;
            }

            @Override
            public BigDecimal getStock() {
                return new BigDecimal("6");
            }
        };
        when(jpa.stockDe(NEGOCIO_ID, List.of(PRODUCTO_ID))).thenReturn(List.of(fila));

        Map<UUID, BigDecimal> result = repository.stockDe(List.of(PRODUCTO_ID), NEGOCIO_ID);

        assertThat(result).containsOnlyKeys(PRODUCTO_ID);
        assertThat(result.get(PRODUCTO_ID)).isEqualByComparingTo("6");
    }

    @Test
    @DisplayName("Sin productos no consulta el stock")
    void stockDeShouldNotQueryWithoutProducts() {
        Map<UUID, BigDecimal> result = repository.stockDe(List.of(), NEGOCIO_ID);

        assertThat(result).isEmpty();
        verifyNoInteractions(jpa);
    }

    @Test
    @DisplayName("Los movimientos recientes pasan el producto como texto y el límite")
    void findRecientesShouldPassProductAsText() {
        when(jpa.findRecientes(NEGOCIO_ID, BLOQUE_ID, PRODUCTO_ID.toString(), 100)).thenReturn(List.of(entity()));

        List<MovimientoStock> result = repository.findRecientes(BLOQUE_ID, NEGOCIO_ID, PRODUCTO_ID, 100);

        assertThat(result).singleElement().satisfies(movimiento -> {
            assertThat(movimiento.getRegistroId()).isEqualTo(PRODUCTO_ID);
            assertThat(movimiento.getFecha()).isEqualTo(LocalDate.parse("2026-10-12"));
        });
    }

    @Test
    @DisplayName("Sin producto, los movimientos recientes son los de todo el bloque")
    void findRecientesShouldNotFilterWithoutProduct() {
        when(jpa.findRecientes(NEGOCIO_ID, BLOQUE_ID, null, 100)).thenReturn(List.of());

        List<MovimientoStock> result = repository.findRecientes(BLOQUE_ID, NEGOCIO_ID, null, 100);

        assertThat(result).isEmpty();
        verify(jpa).findRecientes(NEGOCIO_ID, BLOQUE_ID, null, 100);
    }

    private static MovimientoStockEntity entity() {
        return MovimientoStockEntity.builder()
                .id(MOVIMIENTO_ID)
                .negocioId(NEGOCIO_ID)
                .registroId(PRODUCTO_ID)
                .tipo(TipoMovimiento.ENTRADA)
                .cantidad(BigDecimal.ONE)
                .fecha(LocalDate.parse("2026-10-12"))
                .nota("Compra")
                .build();
    }
}
