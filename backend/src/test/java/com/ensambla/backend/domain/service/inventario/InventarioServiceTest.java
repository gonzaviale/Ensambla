package com.ensambla.backend.domain.service.inventario;

import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.model.inventario.TipoMovimiento;
import com.ensambla.backend.domain.model.inventario.MovimientoStock;
import com.ensambla.backend.domain.repository.inventario.MovimientoStockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID PRODUCTO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID OTRO_PRODUCTO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000002");

    @Mock
    private MovimientoStockRepository movimientoStockRepository;

    @InjectMocks
    private InventarioService inventarioService;

    @Test
    @DisplayName("Una entrada se guarda sin consultar el stock")
    void registrarShouldSaveEntradaWithoutCheckingStock() {
        MovimientoStock entrada = givenMovimiento(TipoMovimiento.ENTRADA, "5");
        givenSaveReturnsArgument();

        MovimientoStock result = inventarioService.registrar(entrada);

        assertThat(result).isSameAs(entrada);
        verify(movimientoStockRepository, never()).stockDe(any(), any());
    }

    @Test
    @DisplayName("Una salida menor o igual al stock se guarda")
    void registrarShouldSaveSalidaWhenStockIsEnough() {
        givenStock(PRODUCTO_ID, "5");
        MovimientoStock salida = givenMovimiento(TipoMovimiento.SALIDA, "5");
        givenSaveReturnsArgument();

        MovimientoStock result = inventarioService.registrar(salida);

        assertThat(result).isSameAs(salida);
    }

    @Test
    @DisplayName("Una salida mayor al stock se rechaza y avisa cuánto queda")
    void registrarShouldRejectSalidaThatLeavesNegativeStock() {
        givenStock(PRODUCTO_ID, "2.500");
        MovimientoStock salida = givenMovimiento(TipoMovimiento.SALIDA, "3");

        assertThatThrownBy(() -> inventarioService.registrar(salida))
                .isInstanceOfSatisfying(RegistroInvalidoException.class, e -> assertThat(e.getErrores())
                        .containsEntry("cantidad", "No hay stock suficiente: quedan 2.5"));
        verify(movimientoStockRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una salida de un producto sin movimientos se rechaza: no hay stock")
    void registrarShouldRejectSalidaOfProductWithoutMovements() {
        when(movimientoStockRepository.stockDe(List.of(PRODUCTO_ID), NEGOCIO_ID)).thenReturn(Map.of());
        MovimientoStock salida = givenMovimiento(TipoMovimiento.SALIDA, "1");

        assertThatThrownBy(() -> inventarioService.registrar(salida))
                .isInstanceOfSatisfying(RegistroInvalidoException.class, e -> assertThat(e.getErrores())
                        .containsEntry("cantidad", "No hay stock suficiente: quedan 0"));
    }

    @Test
    @DisplayName("El stock de cada producto pedido es cero cuando no tiene movimientos, en el orden pedido")
    void stockDeShouldDefaultToZeroInRequestedOrder() {
        List<UUID> ids = List.of(OTRO_PRODUCTO_ID, PRODUCTO_ID);
        when(movimientoStockRepository.stockDe(ids, NEGOCIO_ID)).thenReturn(Map.of(PRODUCTO_ID, new BigDecimal("7")));

        Map<UUID, BigDecimal> result = inventarioService.stockDe(ids, NEGOCIO_ID);

        assertThat(result).containsExactly(
                Map.entry(OTRO_PRODUCTO_ID, BigDecimal.ZERO), Map.entry(PRODUCTO_ID, new BigDecimal("7")));
    }

    @Test
    @DisplayName("Lista los últimos 100 movimientos del bloque")
    void listarRecientesShouldAskForTheLatestHundred() {
        List<MovimientoStock> movimientos = List.of(givenMovimiento(TipoMovimiento.ENTRADA, "1"));
        when(movimientoStockRepository.findRecientes(BLOQUE_ID, NEGOCIO_ID, PRODUCTO_ID, 100))
                .thenReturn(movimientos);

        List<MovimientoStock> result = inventarioService.listarRecientes(BLOQUE_ID, NEGOCIO_ID, PRODUCTO_ID);

        assertThat(result).isSameAs(movimientos);
    }

    private void givenStock(UUID productoId, String stock) {
        when(movimientoStockRepository.stockDe(List.of(productoId), NEGOCIO_ID))
                .thenReturn(Map.of(productoId, new BigDecimal(stock)));
    }

    private void givenSaveReturnsArgument() {
        when(movimientoStockRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private MovimientoStock givenMovimiento(TipoMovimiento tipo, String cantidad) {
        return MovimientoStock.builder()
                .negocioId(NEGOCIO_ID)
                .registroId(PRODUCTO_ID)
                .tipo(tipo)
                .cantidad(new BigDecimal(cantidad))
                .build();
    }
}
