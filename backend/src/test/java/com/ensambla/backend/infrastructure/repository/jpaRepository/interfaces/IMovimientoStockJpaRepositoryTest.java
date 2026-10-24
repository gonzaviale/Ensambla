package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.inventario.TipoMovimiento;
import com.ensambla.backend.infrastructure.repository.entity.MovimientoStockEntity;
import com.ensambla.backend.support.PostgresDatosTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IMovimientoStockJpaRepositoryTest extends PostgresDatosTestSupport {

    @Autowired
    private IMovimientoStockJpaRepository movimientos;

    private UUID negocioA;
    private UUID negocioB;
    private UUID bloqueA;
    private UUID harina;
    private UUID azucar;

    @BeforeEach
    void setUp() {
        negocioA = givenNegocio("a@example.com").getId();
        negocioB = givenNegocio("b@example.com").getId();
        bloqueA = givenBloque(negocioA, "Productos");
        givenCampo(negocioA, bloqueA, "Producto", TipoCampo.TEXTO, 0);
        harina = givenRegistro(negocioA, bloqueA, Map.of(), AHORA);
        azucar = givenRegistro(negocioA, bloqueA, Map.of(), AHORA);
    }

    @Test
    @DisplayName("El stock es la suma de entradas menos salidas de cada producto")
    void stockDeShouldSumEntriesMinusExits() {
        givenMovimiento(negocioA, harina, TipoMovimiento.ENTRADA, "10", 3);
        givenMovimiento(negocioA, harina, TipoMovimiento.SALIDA, "2.5", 2);
        givenMovimiento(negocioA, harina, TipoMovimiento.ENTRADA, "1", 1);
        givenMovimiento(negocioA, azucar, TipoMovimiento.ENTRADA, "4", 1);

        List<IMovimientoStockJpaRepository.StockPorRegistro> result =
                movimientos.stockDe(negocioA, List.of(harina, azucar));

        assertThat(result).hasSize(2);
        assertThat(stockEn(result, harina)).isEqualByComparingTo("8.5");
        assertThat(stockEn(result, azucar)).isEqualByComparingTo("4");
    }

    @Test
    @DisplayName("Un producto sin movimientos no aparece y un negocio ajeno no suma")
    void stockDeShouldSkipProductsWithoutMovementsAndOtherNegocios() {
        givenMovimiento(negocioA, harina, TipoMovimiento.ENTRADA, "10", 1);

        assertThat(movimientos.stockDe(negocioA, List.of(azucar))).isEmpty();
        assertThat(movimientos.stockDe(negocioB, List.of(harina))).isEmpty();
    }

    @Test
    @DisplayName("Los movimientos recientes salen del bloque, los más nuevos primero, y se pueden acotar a un producto")
    void findRecientesShouldListNewestFirstScopedToBloqueAndProduct() {
        MovimientoStockEntity viejo = givenMovimiento(negocioA, harina, TipoMovimiento.ENTRADA, "1", 3);
        MovimientoStockEntity medio = givenMovimiento(negocioA, azucar, TipoMovimiento.ENTRADA, "1", 2);
        MovimientoStockEntity nuevo = givenMovimiento(negocioA, harina, TipoMovimiento.SALIDA, "1", 1);
        UUID otroBloque = givenBloque(negocioA, "Clientes");
        UUID otroProducto = givenRegistro(negocioA, otroBloque, Map.of(), AHORA);
        givenMovimiento(negocioA, otroProducto, TipoMovimiento.ENTRADA, "1", 0);

        List<MovimientoStockEntity> todos = movimientos.findRecientes(negocioA, bloqueA, null, 100);
        List<MovimientoStockEntity> deHarina = movimientos.findRecientes(negocioA, bloqueA, harina.toString(), 100);
        List<MovimientoStockEntity> limitados = movimientos.findRecientes(negocioA, bloqueA, null, 1);

        assertThat(todos).extracting(MovimientoStockEntity::getId)
                .containsExactly(nuevo.getId(), medio.getId(), viejo.getId());
        assertThat(deHarina).extracting(MovimientoStockEntity::getId).containsExactly(nuevo.getId(), viejo.getId());
        assertThat(limitados).extracting(MovimientoStockEntity::getId).containsExactly(nuevo.getId());
    }

    @Test
    @DisplayName("Un negocio ajeno no ve los movimientos del bloque")
    void findRecientesShouldNotLeakToOtherNegocios() {
        givenMovimiento(negocioA, harina, TipoMovimiento.ENTRADA, "1", 1);

        assertThat(movimientos.findRecientes(negocioB, bloqueA, null, 100)).isEmpty();
    }

    @Test
    @DisplayName("No se guarda un movimiento de un producto de otro negocio")
    void saveShouldRejectProductOfOtherNegocio() {
        MovimientoStockEntity ajeno = movimiento(negocioB, harina, TipoMovimiento.ENTRADA, "1", 0);

        assertThatThrownBy(() -> movimientos.saveAndFlush(ajeno)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("No se guarda una cantidad en cero")
    void saveShouldRejectZeroQuantity() {
        MovimientoStockEntity vacio = movimiento(negocioA, harina, TipoMovimiento.ENTRADA, "0", 0);

        assertThatThrownBy(() -> movimientos.saveAndFlush(vacio)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Al borrar el producto se borran sus movimientos")
    void deletingTheProductShouldDeleteItsMovements() {
        givenMovimiento(negocioA, harina, TipoMovimiento.ENTRADA, "1", 0);

        registros.deleteById(harina);
        registros.flush();
        movimientos.flush();

        assertThat(movimientos.findRecientes(negocioA, bloqueA, null, 100)).isEmpty();
    }

    private static BigDecimal stockEn(List<IMovimientoStockJpaRepository.StockPorRegistro> filas, UUID registroId) {
        return filas.stream()
                .filter(fila -> fila.getRegistro().equals(registroId))
                .findFirst()
                .orElseThrow()
                .getStock();
    }

    private MovimientoStockEntity givenMovimiento(UUID negocioId, UUID registroId, TipoMovimiento tipo,
                                                  String cantidad, int minutosAtras) {
        return movimientos.saveAndFlush(movimiento(negocioId, registroId, tipo, cantidad, minutosAtras));
    }

    private static MovimientoStockEntity movimiento(UUID negocioId, UUID registroId, TipoMovimiento tipo,
                                                    String cantidad, int minutosAtras) {
        return MovimientoStockEntity.builder()
                .negocioId(negocioId)
                .registroId(registroId)
                .tipo(tipo)
                .cantidad(new BigDecimal(cantidad))
                .fecha(LocalDate.parse("2026-10-12"))
                .creadoEn(OffsetDateTime.parse("2026-10-12T12:00:00Z").minusMinutes(minutosAtras))
                .build();
    }
}
