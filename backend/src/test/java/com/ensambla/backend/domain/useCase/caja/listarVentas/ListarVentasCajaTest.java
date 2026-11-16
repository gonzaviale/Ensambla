package com.ensambla.backend.domain.useCase.caja.listarVentas;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.repository.caja.CobroCajaRepository;
import com.ensambla.backend.domain.repository.caja.VentaCajaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.support.CajaTestData;
import com.ensambla.backend.support.CobroCajaTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListarVentasCajaTest {

    @Mock
    private NegocioRepository negocios;
    @Mock
    private SistemaRepository sistemas;
    @Mock
    private VentaCajaRepository ventas;
    @Mock
    private CobroCajaRepository cobros;
    private ListarVentasCaja listar;
    private final ListarVentasCajaRequest request = new ListarVentasCajaRequest(
            CajaTestData.USUARIO, CajaTestData.PROYECTO, CajaTestData.INVENTARIO);

    @BeforeEach
    void setUp() {
        listar = new ListarVentasCaja(negocios, sistemas, ventas, cobros);
    }

    @Test
    @DisplayName("Lista ventas con su saldo actual y cobros posteriores del inventario del negocio")
    void executeShouldReturnSalesAndCollections() {
        givenUsuario();
        when(ventas.listarTodas(CajaTestData.NEGOCIO, CajaTestData.PROYECTO, CajaTestData.INVENTARIO))
                .thenReturn(List.of(CobroCajaTestData.venta()));
        when(cobros.listarTodos(CajaTestData.NEGOCIO, CajaTestData.PROYECTO, CajaTestData.INVENTARIO))
                .thenReturn(List.of(CobroCajaTestData.cobro()));

        ListarVentasCajaResponse respuesta = listar.execute(request);

        assertThat(respuesta.ventas().getFirst().saldo()).isEqualByComparingTo("1200");
        assertThat(respuesta.cobros().getFirst().importe()).isEqualByComparingTo("600");
    }

    @Test
    @DisplayName("Un inventario sin movimientos devuelve listas vacías")
    void executeShouldReturnEmptyListsWhenNoMovementsExist() {
        givenUsuario();

        ListarVentasCajaResponse respuesta = listar.execute(request);

        assertThat(respuesta.ventas()).isEmpty();
        assertThat(respuesta.cobros()).isEmpty();
    }

    @Test
    @DisplayName("Un inventario ajeno no expone cuentas ni deudas")
    void executeShouldHideForeignInventory() {
        givenUsuario();
        doThrow(new ResourceNotFoundException("Inventario no encontrado")).when(sistemas)
                .obtenerInventarioCaja(any(), any(), any());

        assertThatThrownBy(() -> listar.execute(request)).isInstanceOf(ResourceNotFoundException.class);
    }

    // --- arrange ---
    private void givenUsuario() {
        when(negocios.obtenerIdDelUsuario(CajaTestData.USUARIO)).thenReturn(CajaTestData.NEGOCIO);
    }
}
