package com.ensambla.backend.domain.repository.inventario;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovimientoStockRepositoryTest {

    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final RegistroId PRODUCTO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final RegistroId OTRO_PRODUCTO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private MovimientoStockRepository movimientoStockRepository;

    @Test
    @DisplayName("El stock de cada producto pedido es cero cuando no tiene movimientos, en el orden pedido")
    void stockDeShouldDefaultToZeroInRequestedOrder() {
        List<RegistroId> ids = List.of(OTRO_PRODUCTO_ID, PRODUCTO_ID);
        when(movimientoStockRepository.stockDe(ids, NEGOCIO_ID)).thenReturn(Map.of(PRODUCTO_ID, new BigDecimal("7")));

        Map<RegistroId, BigDecimal> result = movimientoStockRepository.stockCompletoDe(ids, NEGOCIO_ID);

        assertThat(result).containsExactly(
                Map.entry(OTRO_PRODUCTO_ID, BigDecimal.ZERO), Map.entry(PRODUCTO_ID, new BigDecimal("7")));
    }
}
