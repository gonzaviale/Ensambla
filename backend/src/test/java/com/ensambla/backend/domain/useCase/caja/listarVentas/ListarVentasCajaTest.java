package com.ensambla.backend.domain.useCase.caja.listarVentas;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.repository.caja.CobroCajaRepository;
import com.ensambla.backend.domain.repository.caja.VentaCajaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.support.CajaTestData;
import com.ensambla.backend.support.CobroCajaTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
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

    private ListarVentasCajaResponse respuesta;
    private Throwable rechazo;

    @BeforeEach
    void setUp() {
        listar = new ListarVentasCaja(negocios, sistemas, ventas, cobros);
    }

    @Test
    @DisplayName("Lista ventas con su saldo actual y cobros posteriores del inventario del negocio")
    void listaVentasConSuSaldoActualYCobrosPosterioresDelInventarioDelNegocio() {
        dadoUnaVentaConUnCobroPosterior();

        cuandoSeListanLasVentas();

        entoncesSeMuestraElSaldoActualYElCobro();
    }

    @Test
    @DisplayName("Un inventario sin movimientos devuelve listas vacías")
    void unInventarioSinMovimientosDevuelveListasVacias() {
        dadoUnUsuarioDelNegocio();

        cuandoSeListanLasVentas();

        entoncesNoHayVentasNiCobros();
    }

    @Test
    @DisplayName("Un inventario ajeno no expone cuentas ni deudas")
    void unInventarioAjenoNoExponeCuentasNiDeudas() {
        dadoUnInventarioQueNoEsDelNegocio();

        cuandoSeIntentanListarLasVentas();

        entoncesElInventarioNoExiste();
    }

    // --- dado ---

    private void dadoUnUsuarioDelNegocio() {
        when(negocios.obtenerIdDelUsuario(CajaTestData.USUARIO)).thenReturn(CajaTestData.NEGOCIO);
    }

    private void dadoUnaVentaConUnCobroPosterior() {
        dadoUnUsuarioDelNegocio();
        when(ventas.listarTodas(CajaTestData.NEGOCIO, CajaTestData.PROYECTO, CajaTestData.INVENTARIO))
                .thenReturn(List.of(CobroCajaTestData.venta()));
        when(cobros.listarTodos(CajaTestData.NEGOCIO, CajaTestData.PROYECTO, CajaTestData.INVENTARIO))
                .thenReturn(List.of(CobroCajaTestData.cobro()));
    }

    private void dadoUnInventarioQueNoEsDelNegocio() {
        dadoUnUsuarioDelNegocio();
        doThrow(new ResourceNotFoundException("Inventario no encontrado")).when(sistemas)
                .obtenerInventarioCaja(any(), any(), any());
    }

    // --- cuando ---

    private void cuandoSeListanLasVentas() {
        respuesta = listar.execute(request);
    }

    private void cuandoSeIntentanListarLasVentas() {
        rechazo = catchThrowable(() -> listar.execute(request));
    }

    // --- entonces ---

    private void entoncesSeMuestraElSaldoActualYElCobro() {
        assertThat(respuesta.ventas().getFirst().saldo()).isEqualByComparingTo("1200");
        assertThat(respuesta.cobros().getFirst().importe()).isEqualByComparingTo("600");
    }

    private void entoncesNoHayVentasNiCobros() {
        assertThat(respuesta.ventas()).isEmpty();
        assertThat(respuesta.cobros()).isEmpty();
    }

    private void entoncesElInventarioNoExiste() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }
}
