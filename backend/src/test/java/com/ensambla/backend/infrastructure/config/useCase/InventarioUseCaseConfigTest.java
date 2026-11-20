package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.inventario.MovimientoStock;
import com.ensambla.backend.domain.model.inventario.TipoMovimiento;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.inventario.MovimientoStockRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.useCase.inventario.MovimientoItem;
import com.ensambla.backend.domain.useCase.inventario.consultarStock.ConsultarStockRequest;
import com.ensambla.backend.domain.useCase.inventario.consultarStock.ConsultarStockResponse;
import com.ensambla.backend.domain.useCase.inventario.listarMovimientos.ListarMovimientosRequest;
import com.ensambla.backend.domain.useCase.inventario.registrarMovimiento.RegistrarMovimientoRequest;
import com.ensambla.backend.domain.useCase.inventario.registrarMovimiento.RegistrarMovimientoResponse;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventarioUseCaseConfigTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final RegistroId PRODUCTO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");

    private final InventarioUseCaseConfig config = new InventarioUseCaseConfig();
    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final BloqueRepository bloqueRepository = mock(BloqueRepository.class);
    private final CampoRepository campoRepository = mock(CampoRepository.class);
    private final RegistroRepository registroRepository = mock(RegistroRepository.class);
    private final MovimientoStockRepository movimientoStockRepository =
            mock(MovimientoStockRepository.class, CALLS_REAL_METHODS);
    private RegistrarMovimientoResponse registrado;
    private List<MovimientoItem> movimientos;
    private ConsultarStockResponse stock;

    @BeforeEach
    void dadoUnBloqueDeProductosSinCampos() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(bloqueRepository.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Bloque.builder().id(BLOQUE_ID).build());
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of());
    }

    @Test
    @DisplayName("El wiring de registrar conecta los servicios y el reloj")
    void elWiringDeRegistrarConectaLosServiciosYElReloj() {
        dadoUnProductoConStock(BigDecimal.TEN);

        cuandoSeRegistraUnaEntrada();

        entoncesSeRegistroLaEntradaConStock("10");
    }

    @Test
    @DisplayName("El wiring de listar conecta los servicios")
    void elWiringDeListarConectaLosServicios() {
        dadaUnaSalidaReciente();

        cuandoSeListanLosMovimientos();

        entoncesElUnicoMovimientoEs("SALIDA");
    }

    @Test
    @DisplayName("El wiring de stock conecta los servicios")
    void elWiringDeStockConectaLosServicios() {
        dadoUnProductoListadoConStock(BigDecimal.TWO);

        cuandoSeConsultaElStock();

        entoncesElUnicoProductoTieneStock("2");
    }

    private void dadoUnProductoConStock(BigDecimal cantidad) {
        when(registroRepository.obtener(PRODUCTO_ID, NEGOCIO_ID)).thenReturn(unProducto());
        when(movimientoStockRepository.save(any())).thenAnswer(guardado -> guardado.getArgument(0));
        dadoQueElProductoTieneStock(cantidad);
    }

    private void dadaUnaSalidaReciente() {
        when(movimientoStockRepository.findRecientes(BLOQUE_ID, NEGOCIO_ID, null, 100)).thenReturn(List.of(
                MovimientoStock.builder().registroId(PRODUCTO_ID).tipo(TipoMovimiento.SALIDA)
                        .cantidad(BigDecimal.ONE).build()));
        when(registroRepository.findAllByIdsAndBloque(List.of(PRODUCTO_ID), BLOQUE_ID, NEGOCIO_ID))
                .thenReturn(List.of(unProducto()));
    }

    private void dadoUnProductoListadoConStock(BigDecimal cantidad) {
        when(registroRepository.buscar(any())).thenReturn(new Pagina<>(List.of(unProducto()), 0, 20, 1));
        dadoQueElProductoTieneStock(cantidad);
    }

    private void dadoQueElProductoTieneStock(BigDecimal cantidad) {
        when(movimientoStockRepository.stockDe(List.of(PRODUCTO_ID), NEGOCIO_ID)).thenReturn(Map.of(PRODUCTO_ID, cantidad));
    }

    private void cuandoSeRegistraUnaEntrada() {
        registrado = config.registrarMovimiento(negocioRepository, bloqueRepository, campoRepository, registroRepository,
                        movimientoStockRepository, new TransaccionDirecta(), Clock.systemUTC())
                .execute(new RegistrarMovimientoRequest(USUARIO_ID, BLOQUE_ID, PRODUCTO_ID, "ENTRADA", BigDecimal.ONE,
                        null, null));
    }

    private void cuandoSeListanLosMovimientos() {
        movimientos = config.listarMovimientos(negocioRepository, bloqueRepository, campoRepository, registroRepository,
                movimientoStockRepository).execute(new ListarMovimientosRequest(USUARIO_ID, BLOQUE_ID, null));
    }

    private void cuandoSeConsultaElStock() {
        stock = config.consultarStock(negocioRepository, bloqueRepository, campoRepository, registroRepository,
                movimientoStockRepository).execute(new ConsultarStockRequest(USUARIO_ID, BLOQUE_ID, null, null, null));
    }

    private void entoncesSeRegistroLaEntradaConStock(String stockActual) {
        assertThat(registrado.movimiento().tipo()).isEqualTo("ENTRADA");
        assertThat(registrado.stockActual()).isEqualByComparingTo(stockActual);
    }

    private void entoncesElUnicoMovimientoEs(String tipo) {
        assertThat(movimientos).singleElement().satisfies(movimiento -> assertThat(movimiento.tipo()).isEqualTo(tipo));
    }

    private void entoncesElUnicoProductoTieneStock(String cantidad) {
        assertThat(stock.contenido()).singleElement()
                .satisfies(producto -> assertThat(producto.stock()).isEqualByComparingTo(cantidad));
    }

    private static Registro unProducto() {
        return Registro.builder().id(PRODUCTO_ID).bloqueId(BLOQUE_ID).valores(Map.of()).build();
    }
}