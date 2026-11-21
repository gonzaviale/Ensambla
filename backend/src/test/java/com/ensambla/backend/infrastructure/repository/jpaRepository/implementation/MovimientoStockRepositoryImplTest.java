package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.inventario.MovimientoStock;
import com.ensambla.backend.domain.model.inventario.MovimientoStockId;
import com.ensambla.backend.domain.model.inventario.TipoMovimiento;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.RegistroId;
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

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final RegistroId PRODUCTO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final MovimientoStockId MOVIMIENTO_ID = MovimientoStockId.de("e0000000-0000-4000-8000-000000000001");

    @Mock
    private IMovimientoStockJpaRepository jpa;

    private MovimientoStockRepositoryImpl repository;

    @BeforeEach
    void dadoUnRepositorioDeMovimientos() {
        repository = new MovimientoStockRepositoryImpl(jpa, new MovimientoStockRepositoryMapper());
    }

    private MovimientoStock guardado;
    private Map<RegistroId, BigDecimal> stock;
    private List<MovimientoStock> recientes;

    @Test
    @DisplayName("Guarda el movimiento y devuelve el modelo persistido")
    void guardaElMovimientoYDevuelveElModeloPersistido() {
        dadoQueGuardarDevuelveElMovimiento();

        cuandoSeGuardaUnaEntradaDeStock();

        entoncesSeDevuelveLaEntradaDeCompraPersistida();
    }

    @Test
    @DisplayName("El stock sale del repositorio JPA indexado por producto")
    void elStockSaleDelRepositorioJpaIndexadoPorProducto() {
        dadoQueElProductoTieneSeisUnidades();

        cuandoSeConsultaElStockDelProducto();

        entoncesElStockEstaIndexadoPorProductoYVaSeis();
    }

    @Test
    @DisplayName("Sin productos no consulta el stock")
    void sinProductosNoConsultaElStock() {
        cuandoSeConsultaElStockSinProductos();

        entoncesNoHayStockNiSeConsultoElRepositorio();
    }

    @Test
    @DisplayName("Los movimientos recientes pasan el producto como texto y el límite")
    void losMovimientosRecientesPasanElProductoComoTextoYElLimite() {
        dadoQueElProductoTieneUnMovimientoReciente();

        cuandoSeListanLosMovimientosRecientesDelProducto();

        entoncesSeListaElMovimientoDelProducto();
    }

    @Test
    @DisplayName("Sin producto, los movimientos recientes son los de todo el bloque")
    void sinProductoLosMovimientosRecientesSonLosDeTodoElBloque() {
        dadoQueElBloqueNoTieneMovimientosRecientes();

        cuandoSeListanLosMovimientosRecientesDelBloque();

        entoncesNoHayMovimientosYSeConsultoSinFiltroDeProducto();
    }

    // --- dado ---
    private void dadoQueGuardarDevuelveElMovimiento() {
        when(jpa.save(any(MovimientoStockEntity.class))).thenReturn(unaEntidad());
    }

    private void dadoQueElProductoTieneSeisUnidades() {
        IMovimientoStockJpaRepository.StockPorRegistro fila = new IMovimientoStockJpaRepository.StockPorRegistro() {
            @Override
            public UUID getRegistro() {
                return PRODUCTO_ID.valor();
            }

            @Override
            public BigDecimal getStock() {
                return new BigDecimal("6");
            }
        };
        when(jpa.stockDe(NEGOCIO_ID.valor(), List.of(PRODUCTO_ID.valor()))).thenReturn(List.of(fila));
    }

    private void dadoQueElProductoTieneUnMovimientoReciente() {
        when(jpa.findRecientes(NEGOCIO_ID.valor(), BLOQUE_ID.valor(), PRODUCTO_ID.toString(), 100))
                .thenReturn(List.of(unaEntidad()));
    }

    private void dadoQueElBloqueNoTieneMovimientosRecientes() {
        when(jpa.findRecientes(NEGOCIO_ID.valor(), BLOQUE_ID.valor(), null, 100)).thenReturn(List.of());
    }

    private static MovimientoStockEntity unaEntidad() {
        return MovimientoStockEntity.builder()
                .id(MOVIMIENTO_ID.valor())
                .negocioId(NEGOCIO_ID.valor())
                .registroId(PRODUCTO_ID.valor())
                .tipo(TipoMovimiento.ENTRADA)
                .cantidad(BigDecimal.ONE)
                .fecha(LocalDate.parse("2026-10-12"))
                .nota("Compra")
                .build();
    }

    // --- cuando ---
    private void cuandoSeGuardaUnaEntradaDeStock() {
        guardado = repository.save(MovimientoStock.builder()
                .negocioId(NEGOCIO_ID)
                .registroId(PRODUCTO_ID)
                .tipo(TipoMovimiento.ENTRADA)
                .cantidad(BigDecimal.ONE)
                .build());
    }

    private void cuandoSeConsultaElStockDelProducto() {
        stock = repository.stockDe(List.of(PRODUCTO_ID), NEGOCIO_ID);
    }

    private void cuandoSeConsultaElStockSinProductos() {
        stock = repository.stockDe(List.of(), NEGOCIO_ID);
    }

    private void cuandoSeListanLosMovimientosRecientesDelProducto() {
        recientes = repository.findRecientes(BLOQUE_ID, NEGOCIO_ID, PRODUCTO_ID, 100);
    }

    private void cuandoSeListanLosMovimientosRecientesDelBloque() {
        recientes = repository.findRecientes(BLOQUE_ID, NEGOCIO_ID, null, 100);
    }

    // --- entonces ---
    private void entoncesSeDevuelveLaEntradaDeCompraPersistida() {
        assertThat(guardado.getId()).isEqualTo(MOVIMIENTO_ID);
        assertThat(guardado.getTipo()).isEqualTo(TipoMovimiento.ENTRADA);
        assertThat(guardado.getNota()).isEqualTo("Compra");
    }

    private void entoncesElStockEstaIndexadoPorProductoYVaSeis() {
        assertThat(stock).containsOnlyKeys(PRODUCTO_ID);
        assertThat(stock.get(PRODUCTO_ID)).isEqualByComparingTo("6");
    }

    private void entoncesNoHayStockNiSeConsultoElRepositorio() {
        assertThat(stock).isEmpty();
        verifyNoInteractions(jpa);
    }

    private void entoncesSeListaElMovimientoDelProducto() {
        assertThat(recientes).singleElement().satisfies(movimiento -> {
            assertThat(movimiento.getRegistroId()).isEqualTo(PRODUCTO_ID);
            assertThat(movimiento.getFecha()).isEqualTo(LocalDate.parse("2026-10-12"));
        });
    }

    private void entoncesNoHayMovimientosYSeConsultoSinFiltroDeProducto() {
        assertThat(recientes).isEmpty();
        verify(jpa).findRecientes(NEGOCIO_ID.valor(), BLOQUE_ID.valor(), null, 100);
    }
}
