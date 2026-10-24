package com.ensambla.backend.domain.service.inventario;

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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID PRODUCTO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID OTRO_PRODUCTO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000002");

    @Mock
    private MovimientoStockRepository movimientoStockRepository;

    @InjectMocks
    private InventarioService inventarioService;

    @Test
    @DisplayName("El stock de cada producto pedido es cero cuando no tiene movimientos, en el orden pedido")
    void stockDeShouldDefaultToZeroInRequestedOrder() {
        List<UUID> ids = List.of(OTRO_PRODUCTO_ID, PRODUCTO_ID);
        when(movimientoStockRepository.stockDe(ids, NEGOCIO_ID)).thenReturn(Map.of(PRODUCTO_ID, new BigDecimal("7")));

        Map<UUID, BigDecimal> result = inventarioService.stockDe(ids, NEGOCIO_ID);

        assertThat(result).containsExactly(
                Map.entry(OTRO_PRODUCTO_ID, BigDecimal.ZERO), Map.entry(PRODUCTO_ID, new BigDecimal("7")));
    }
}
