package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.common.Pagina;
import com.ensambla.backend.domain.model.inventario.MovimientoStock;
import com.ensambla.backend.domain.model.inventario.TipoMovimiento;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.repository.inventario.MovimientoStockRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.inventario.InventarioService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import com.ensambla.backend.domain.useCase.inventario.consultarStock.ConsultarStockRequest;
import com.ensambla.backend.domain.useCase.inventario.listarMovimientos.ListarMovimientosRequest;
import com.ensambla.backend.domain.useCase.inventario.registrarMovimiento.RegistrarMovimientoRequest;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventarioUseCaseConfigTest {

    private static final UUID USUARIO_ID = UUID.fromString("b9000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID PRODUCTO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");

    private final InventarioUseCaseConfig config = new InventarioUseCaseConfig();

    private NegocioService negocioService;
    private BloqueService bloqueService;
    private CampoService campoService;
    private RegistroDatosService registroDatosService;
    private InventarioService inventarioService;
    private MovimientoStockRepository movimientoStockRepository;
    private RegistroRepository registroRepository;

    @BeforeEach
    void setUp() {
        negocioService = mock(NegocioService.class);
        bloqueService = mock(BloqueService.class);
        campoService = mock(CampoService.class);
        registroDatosService = mock(RegistroDatosService.class);
        movimientoStockRepository = mock(MovimientoStockRepository.class);
        registroRepository = mock(RegistroRepository.class);
        inventarioService = new InventarioService(movimientoStockRepository);
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Bloque.builder().id(BLOQUE_ID).build());
        when(campoService.listarDeBloque(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of());
    }

    @Test
    @DisplayName("El wiring de registrar conecta los servicios y el reloj")
    void registrarMovimientoUseCaseShouldWireCollaborators() {
        when(registroDatosService.obtener(PRODUCTO_ID, NEGOCIO_ID)).thenReturn(givenProducto());
        when(movimientoStockRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(movimientoStockRepository.stockDe(List.of(PRODUCTO_ID), NEGOCIO_ID))
                .thenReturn(Map.of(PRODUCTO_ID, BigDecimal.TEN));

        var response = config.registrarMovimiento(
                        negocioService, bloqueService, campoService, registroDatosService, inventarioService,
                        movimientoStockRepository, new TransaccionDirecta(), Clock.systemUTC())
                .execute(new RegistrarMovimientoRequest(
                        USUARIO_ID, BLOQUE_ID, PRODUCTO_ID, "ENTRADA", BigDecimal.ONE, null, null));

        assertThat(response.movimiento().tipo()).isEqualTo("ENTRADA");
        assertThat(response.stockActual()).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("El wiring de listar conecta los servicios")
    void listarMovimientosUseCaseShouldWireCollaborators() {
        when(movimientoStockRepository.findRecientes(BLOQUE_ID, NEGOCIO_ID, null, 100)).thenReturn(List.of(
                MovimientoStock.builder().registroId(PRODUCTO_ID).tipo(TipoMovimiento.SALIDA)
                        .cantidad(BigDecimal.ONE).build()));
        when(registroRepository.findAllByIdsAndBloque(List.of(PRODUCTO_ID), BLOQUE_ID, NEGOCIO_ID))
                .thenReturn(List.of(givenProducto()));

        var response = config.listarMovimientos(
                        negocioService, bloqueService, campoService, registroRepository, movimientoStockRepository)
                .execute(new ListarMovimientosRequest(USUARIO_ID, BLOQUE_ID, null));

        assertThat(response).singleElement().satisfies(item -> assertThat(item.tipo()).isEqualTo("SALIDA"));
    }

    @Test
    @DisplayName("El wiring de stock conecta los servicios")
    void consultarStockUseCaseShouldWireCollaborators() {
        when(registroDatosService.listar(any())).thenReturn(new Pagina<>(List.of(givenProducto()), 0, 20, 1));
        when(movimientoStockRepository.stockDe(List.of(PRODUCTO_ID), NEGOCIO_ID))
                .thenReturn(Map.of(PRODUCTO_ID, BigDecimal.TWO));

        var response = config.consultarStock(
                        negocioService, bloqueService, campoService, registroDatosService, inventarioService)
                .execute(new ConsultarStockRequest(USUARIO_ID, BLOQUE_ID, null, null, null));

        assertThat(response.contenido()).singleElement()
                .satisfies(item -> assertThat(item.stock()).isEqualByComparingTo("2"));
    }

    private static Registro givenProducto() {
        return Registro.builder().id(PRODUCTO_ID).bloqueId(BLOQUE_ID).valores(Map.of()).build();
    }
}
